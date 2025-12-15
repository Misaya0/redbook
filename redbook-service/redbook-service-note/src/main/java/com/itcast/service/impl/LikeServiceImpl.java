package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.annotation.SendMessage;
import com.itcast.constant.ExceptionConstant;
import com.itcast.constant.MqConstant;
import com.itcast.constant.RedisConstant;
import com.itcast.enums.MessageTypeEnum;
import com.itcast.exception.NoteNoExistException;
import com.itcast.enums.LogType;
import com.itcast.mapper.LikeMapper;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Message;
import com.itcast.model.pojo.Like;
import com.itcast.model.pojo.Note;
import com.itcast.result.Result;
import com.itcast.service.LikeService;
import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.Cache;

@Service
@Slf4j
public class LikeServiceImpl implements LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private CacheManager cacheManager;

    @Override
    @SendMessage(type = LogType.LIKE)
    public Result<Void> like(Long noteId) {
        Integer userId = UserContext.getUserId();

        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
        }

        // 1.判断redis中的本用户是否存在这个id
        Boolean isLike = redisTemplate.opsForValue().getBit(RedisConstant.LIKE_SET_CACHE + noteId, userId);

        if (Boolean.TRUE.equals(isLike)) {
            // 删除点赞
            LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Like::getNoteId, noteId).eq(Like::getUserId, userId);
            likeMapper.delete(queryWrapper);

            // 更新redis
            redisTemplate.opsForValue().setBit(RedisConstant.LIKE_SET_CACHE + noteId, userId, false);

            // 更新点赞数
            note.setLike(note.getLike() - 1);
        } else {
            // 添加点赞
            Like like = new Like();
            like.setNoteId(noteId);
            like.setUserId(userId);
            likeMapper.insert(like);

            // 更新redis
            redisTemplate.opsForValue().setBit(RedisConstant.LIKE_SET_CACHE + noteId, userId, true);

            // 更新点赞数
            note.setLike(note.getLike() + 1);

            // 用户点赞，消息发送
            com.itcast.model.event.MessageEvent event = new com.itcast.model.event.MessageEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setActorId(userId.longValue());
            event.setRecipientId(note.getUserId().longValue());
            event.setType("LIKE");
            event.setTargetType("NOTE");
            event.setTargetId(noteId);
            event.setTimestamp(System.currentTimeMillis());
            event.setContentBrief("赞了你的笔记");
            
            rabbitTemplate.convertAndSend(MqConstant.MESSAGE_NOTICE_EXCHANGE, "note.liked", event);
        }
        // 2.更新点赞数
        noteMapper.updateById(note);
        // 3.删除笔记缓存
        redisTemplate.delete(RedisConstant.NOTE_DETAIL_CACHE + noteId);

        Cache noteCache = cacheManager.getCache("noteCache");
        if (noteCache != null) {
            noteCache.evict(noteId);  // 关键：本地缓存也要失效
        }
        return Result.success(null);
    }

    @Override
    public Result<Boolean> isLike(Long noteId) {
        Integer userId = UserContext.getUserId();
        Boolean isLike = redisTemplate.opsForValue().getBit(
                RedisConstant.LIKE_SET_CACHE + noteId, userId);
        return Result.success(isLike);
    }
}
