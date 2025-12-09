package com.itcast.service.impl;

import com.itcast.annotation.SendMessage;
import com.itcast.client.CommentClient;
import com.itcast.client.UserClient;
import com.itcast.constant.ExceptionConstant;
import com.itcast.constant.RedisConstant;
import com.itcast.exception.NoteNoExistException;
import com.itcast.exception.UserNoExistException;
import com.itcast.handler.NoteHandler;
import com.itcast.mapper.NoteMapper;
import com.itcast.mapper.NoteScanMapper;
import com.itcast.model.dto.NoteDto;
import com.itcast.enums.LogType;
import com.itcast.model.pojo.Note;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.NoteService;
import com.itcast.strategy.GetNotesStrategy;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyFactory;
import com.itcast.strategy.NoteStrategyType;
import com.itcast.context.UserContext;
import com.itcast.util.BloomFilterUtil;
import com.itcast.util.DealTimeUtil;
import com.itcast.util.DiffDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.itcast.util.LocalFileUtil;
import com.itcast.constant.MqConstant;
import com.itcast.model.dto.NoteEsSyncMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.itcast.model.pojo.NoteEs;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteScanMapper noteScanMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CommentClient commentClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;
    
    @Autowired
    private LocalFileUtil localFileUtil;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Lock lock = new ReentrantLock();

    @Autowired
    private CacheManager cacheManager;

    @Override
    @SendMessage(type = LogType.SCAN)
    public Result<NoteVo> getNote(Long noteId) throws ParseException {
        Note note;
        // 0.查询本地缓存中是否存在缓存
        Cache noteCache = cacheManager.getCache("noteCache");
        if (noteCache == null) throw new RuntimeException("缓存不存在");
        Note localCacheNote = noteCache.get(noteId, Note.class);
        if (localCacheNote != null) {
            note = localCacheNote;
        } else {
            // 1.查询redis中是否存在缓存
            Note cacheNote = (Note) redisTemplate.opsForHash().get(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note");

            // 2.如果缓存不存在，则
            if (cacheNote == null) {
                boolean locked = false;
                try {
                    // 2.1 加锁防止缓存击穿
                    if (lock.tryLock()) {
                        log.info("加锁成功");
                        locked = true;
                        // 2.2 查询数据库
                        note = noteMapper.selectById(noteId);
                        if (note == null) {
                            redisTemplate.opsForHash().put(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note", new Note());
                            redisTemplate.expire(RedisConstant.NOTE_DETAIL_CACHE + noteId, 5, TimeUnit.MINUTES);
                            throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
                        } else {
                            // 2.3 缓存到redis并设置有效时间
                            redisTemplate.opsForHash().put(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note", note);
                            redisTemplate.expire(RedisConstant.NOTE_DETAIL_CACHE + noteId, 5, TimeUnit.MINUTES);
                            noteCache.put(noteId, note);
                        }
                    } else {
                        Thread.sleep(50);
                        return getNote(noteId);
                    }
                } catch (Exception e) {
                    log.error(e.toString());
                    return Result.success(null);
                } finally {
                    if (locked) {
                        log.info("释放锁");
                        lock.unlock();
                    }
                }
            } else {
                note = cacheNote;
                noteCache.put(noteId, note);
            }
        }

        // 3.获取发布笔记用户信息
        if (note.getUserId() == null) {
            throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
        }
        User user = userClient.getUserById(note.getUserId()).getData();
        if (user == null) {
            throw new UserNoExistException(ExceptionConstant.USER_NO_EXIST);
        }

        // 4.处理时间字符串
        int days = DiffDayUtil.diffDays(
                new SimpleDateFormat("yyyy-MM-dd").parse(note.getTime()), new Date());
        String dealTime = DealTimeUtil.dealTime(days);
        if (StringUtils.isBlank(dealTime)) {
            dealTime = note.getTime();
        }

        // 5.设置vo
        NoteVo noteVo = new NoteVo();
        BeanUtils.copyProperties(note, noteVo);
        noteVo.setDealTime(dealTime);
        noteVo.setUser(user);
        
        // 填充评论数 (远程调用)
        Result<Integer> commentCountResult = commentClient.getCommentCount(noteId);
        if (commentCountResult != null && commentCountResult.getData() != null) {
            noteVo.setComment(commentCountResult.getData());
        } else {
            noteVo.setComment(0);
        }
        
        // 填充点赞数和收藏数 (从Redis实时获取更准确，或者直接使用数据库中的值)
        // 注意：这里直接使用note中的值，如果note来自缓存，可能不是最新的。
        // 如果需要实时性，应该从Redis中获取最新的点赞/收藏数覆盖
        // 这里假设数据库/缓存中的值是准实时的


        // 6.加入布隆过滤器
        Integer loginUserId = UserContext.getUserId();
        bloomFilterUtil.add(RedisConstant.USER_BLOOM_FILTER + loginUserId, noteId.toString());

//        // 7.记录用户访问笔记
//        try {
//            NoteBrowse noteBrowse = new NoteBrowse();
//            noteBrowse.setNoteId(noteId);
//            noteBrowse.setUserId(loginUserId);
//            noteScanMapper.insert(noteBrowse);
//        } catch (Exception e) {
//            log.error("用户已经访问过，不需要再次插入数据库");
//        }
        return Result.success(noteVo);
    }

    @Autowired
    private NoteStrategyFactory strategyFactory;

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyType strategyType, NoteStrategyContext context) {
        GetNotesStrategy strategy = strategyFactory.getStrategy(strategyType);
        return strategy.getNotes(context);
    }

    @Autowired
    private List<NoteHandler> noteHandlers;

    /**
     * 发布笔记（使用 Saga 补偿型事务）
     * 将执行成功的步骤保存在ThreadLocal中用于后续补偿操作
     * 当某个步骤失败时，会自动回滚已执行的所有步骤
     * 流程：设置用户ID -> 上传图片 -> 获取位置 -> 过滤标题 -> 获取笔记类型 -> 提取话题 -> 保存笔记 -> 保存笔记话题 -> 保存位置到Redis -> 保存到ES
     */
    @Override
    public Result<Void> postNote(NoteDto dto) {
        log.info("发布笔记开始（Saga 补偿型事务）...");
        
        try {
            // 初始化补偿上下文
            NoteHandler.initCompensationContext();
            
            // 责任链执行
            Iterator<NoteHandler> iterator = noteHandlers.iterator();
            NoteHandler firstHandler = iterator.next();
            firstHandler.handleWithCompensation(dto, iterator);
            
            log.info("笔记发布成功，noteId: {}", dto.getId());
            return Result.success(null);
        } catch (Exception e) {
            log.error("笔记发布失败，已执行补偿操作，noteId: {}", dto.getId(), e);
        } finally {
            // 确保清理 ThreadLocal
            NoteHandler.clearExecutedHandlers();
        }

        return Result.failure("发布笔记失败");
}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateNote(NoteDto dto) throws java.io.IOException {
        // 1. 检查笔记是否存在
        Note note = noteMapper.selectById(dto.getId());
        if (note == null) {
            throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
        }

        // 2. 权限校验
        Integer userId = UserContext.getUserId();
        if (!note.getUserId().equals(userId)) {
            return Result.failure("无权修改此笔记");
        }

        // 3. 如果有新图片，上传并更新
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            String path = localFileUtil.uploadImg(dto.getFile().getBytes());
            note.setImage(path);
        }

        // 4. 更新字段
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        if (StringUtils.isNotBlank(dto.getType())) {
            note.setType(dto.getType());
        }
        if (dto.getLongitude() != null) note.setLongitude(dto.getLongitude());
        if (dto.getLatitude() != null) note.setLatitude(dto.getLatitude());

        // 5. 更新数据库
        noteMapper.updateById(note);

        // 6. 更新缓存
        redisTemplate.delete(RedisConstant.NOTE_DETAIL_CACHE + note.getId());
        Cache noteCache = cacheManager.getCache("noteCache");
        if (noteCache != null) {
            noteCache.evict(note.getId());
        }

        // 7. 更新 ES (通过MQ异步同步)
        try {
            NoteEsSyncMessage message = new NoteEsSyncMessage("UPDATE", note.getId(), note);
            rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
            log.info("Sent ES sync message (UPDATE) for note: {}", note.getId());
        } catch (Exception e) {
            log.error("ES同步消息发送失败", e);
        }

        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteNote(Long noteId) {
        // 1. 检查笔记是否存在
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
        }

        // 2. 权限校验
        Integer userId = UserContext.getUserId();
        if (!note.getUserId().equals(userId)) {
            return Result.failure("无权删除此笔记");
        }

        // 3. 删除数据库
        noteMapper.deleteById(noteId);

        // 4. 删除缓存
        redisTemplate.delete(RedisConstant.NOTE_DETAIL_CACHE + noteId);
        Cache noteCache = cacheManager.getCache("noteCache");
        if (noteCache != null) {
            noteCache.evict(noteId);
        }

        // 5. 删除 ES (通过MQ异步同步)
        try {
            NoteEsSyncMessage message = new NoteEsSyncMessage("DELETE", noteId, null);
            rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
            log.info("Sent ES sync message (DELETE) for note: {}", noteId);
        } catch (Exception e) {
            log.error("ES删除消息发送失败", e);
        }

        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteNotes(List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Result.success(null);
        }

        Integer userId = UserContext.getUserId();

        // 1. 校验所有笔记权限
        List<Note> notes = noteMapper.selectBatchIds(noteIds);

        for (Note note : notes) {
            if (!note.getUserId().equals(userId)) {
                return Result.failure("包含无权删除的笔记: " + note.getTitle());
            }
        }

        // 2. 批量删除数据库
        noteMapper.deleteBatchIds(noteIds);

        // 3. 删除缓存和ES
        Cache noteCache = cacheManager.getCache("noteCache");
        for (Long id : noteIds) {
            redisTemplate.delete(RedisConstant.NOTE_DETAIL_CACHE + id);
            if (noteCache != null) {
                noteCache.evict(id);
            }
            try {
                NoteEsSyncMessage message = new NoteEsSyncMessage("DELETE", id, null);
                rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
            } catch (Exception e) {
                log.error("ES删除消息发送失败 for id: " + id, e);
            }
        }

        return Result.success(null);
    }
}

