package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.itcast.util.MinioUtil;
import com.itcast.constant.MqConstant;
import com.itcast.model.dto.NoteEsSyncMessage;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    private MinioUtil minioUtil;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheManager cacheManager;

    @Override
    @SendMessage(type = LogType.SCAN)
    public Result<NoteVo> getNote(Long noteId) {
        Note note = null;
        int retryCount = 0;
        int maxRetries = 3; // 最大自旋重试次数

        // 0.查询本地缓存 (Caffeine)
        Cache noteCache = cacheManager.getCache("noteCache");
        if (noteCache == null) throw new RuntimeException("本地缓存组件异常");
        
        Note localCacheNote = noteCache.get(noteId, Note.class);
        if (localCacheNote != null) {
            log.info("本地缓存命中: {}", noteId);
            note = localCacheNote;
        } else {
            // 1. 本地缓存未命中，进入 Redis + 分布式锁流程
            // 引入 while 循环实现自旋重试
            while (retryCount < maxRetries) {
                // 1.1 查询 Redis 中是否存在缓存
                Note cacheNote = (Note) redisTemplate.opsForHash().get(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note");

                if (cacheNote != null) {
                    log.info("Redis缓存命中: {}", noteId);
                    note = cacheNote;
                    // 回填本地缓存
                    noteCache.put(noteId, note);
                    break; 
                }

                // 2. 如果 Redis 也不存在，则尝试加锁回源
                String lockKey = "lock:note:" + noteId;
                RLock rLock = redissonClient.getLock(lockKey);
                try {
                    // 尝试获取锁，单次等待 1s，自动释放 10s
                    if (rLock.tryLock(1000, 10000, TimeUnit.MILLISECONDS)) {
                        try {
                            log.info("获取分布式锁成功: {}", lockKey);
                            // 2.2 双重检查：获取锁后再次查询 Redis
                            cacheNote = (Note) redisTemplate.opsForHash().get(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note");
                            if (cacheNote != null) {
                                note = cacheNote;
                                noteCache.put(noteId, note);
                            } else {
                                // 2.3 查询数据库
                                note = noteMapper.selectById(noteId);
                                if (note == null) {
                                    // 缓存穿透处理：数据库查不到则缓存空对象
                                    Note emptyNote = new Note();
                                    redisTemplate.opsForHash().put(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note", emptyNote);
                                    redisTemplate.expire(RedisConstant.NOTE_DETAIL_CACHE + noteId, 5, TimeUnit.MINUTES);
                                    // 同时同步到本地缓存
                                    noteCache.put(noteId, emptyNote);
                                    throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
                                } else {
                                    // 2.4 缓存到 Redis
                                    redisTemplate.opsForHash().put(RedisConstant.NOTE_DETAIL_CACHE + noteId, "note", note);
                                    redisTemplate.expire(RedisConstant.NOTE_DETAIL_CACHE + noteId, 5, TimeUnit.MINUTES);
                                    // 同时写入本地缓存
                                    noteCache.put(noteId, note);
                                }
                            }
                            break; 
                        } finally {
                            if (rLock.isHeldByCurrentThread()) {
                                rLock.unlock();
                            }
                        }
                    } else {
                        // 获取锁失败，增加重试计数
                        retryCount++;
                        log.warn("获取锁超时，正在进行第 {} 次自旋重试: noteId={}", retryCount, noteId);
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    log.error("获取分布式锁被中断: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    return Result.failure("服务被中断");
                } catch (NoteNoExistException e) {
                    throw e; 
                } catch (Exception e) {
                    log.error("获取笔记详情异常: ", e);
                    return Result.success(null);
                }
            }
        }

        // 如果达到最大重试次数仍未获取到数据
        if (note == null) {
            return Result.failure("系统繁忙，请稍后再试");
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
        String dealTime = "";
        if (note.getTime() != null) {
            long days = ChronoUnit.DAYS.between(note.getTime().toLocalDate(), LocalDateTime.now().toLocalDate());
            dealTime = DealTimeUtil.dealTime((int) days);
            if (StringUtils.isBlank(dealTime)) {
                dealTime = note.getTime().toString();
            }
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
        Long loginUserId = UserContext.getUserId();
        bloomFilterUtil.add(RedisConstant.USER_BLOOM_FILTER + loginUserId, noteId.toString());

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
        Long userId = UserContext.getUserId();
        if (!note.getUserId().equals(userId)) {
            return Result.failure("无权修改此笔记");
        }

        // 3. 如果有新图片，上传并更新
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            // 先上传新图片，成功后再替换旧图片链接
            String newUrl = minioUtil.upload(dto.getFile());
            String oldUrl = note.getImage();
            note.setImage(newUrl);

            // 旧图片做一次尽力删除，避免对象存储残留（不影响主流程）
            if (StringUtils.isNotBlank(oldUrl)) {
                try {
                    minioUtil.delete(oldUrl);
                } catch (Exception e) {
                    log.warn("删除旧图片失败（忽略）：{}", oldUrl, e);
                }
            }
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
    public Result<List<NoteVo>> getRelatedNotes(Long productId) {
        // 1. 查询关联该商品的笔记 (按点赞数降序取前2条)
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getProductId, productId)
                    .orderByDesc(Note::getLike)
                    .last("limit 2");
        
        List<Note> notes = noteMapper.selectList(queryWrapper);
        
        // 2. 转换为 VO
        List<NoteVo> noteVos = notes.stream().map(note -> {
            NoteVo vo = new NoteVo();
            BeanUtils.copyProperties(note, vo);
            // 简单填充用户信息，不做复杂查询
            try {
                User user = userClient.getUserById(note.getUserId()).getData();
                vo.setUser(user);
            } catch (Exception e) {
                log.error("获取笔记作者失败", e);
            }
            return vo;
        }).collect(java.util.stream.Collectors.toList());
        
        return Result.success(noteVos);
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
        Long userId = UserContext.getUserId();
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

        Long userId = UserContext.getUserId();

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
