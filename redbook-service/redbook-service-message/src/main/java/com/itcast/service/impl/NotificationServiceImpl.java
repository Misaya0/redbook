package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.itcast.entity.Notification;
import com.itcast.entity.UnreadCounter;
import com.itcast.mapper.NotificationMapper;
import com.itcast.mapper.UnreadCounterMapper;
import com.itcast.model.event.MessageEvent;
import com.itcast.service.NotificationService;
import com.itcast.session.Session;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import com.itcast.client.NoteClient;
import com.itcast.client.UserClient;
import com.google.gson.reflect.TypeToken;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.AttentionVo;
import com.itcast.model.vo.NotificationVo;
import com.itcast.result.Result;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UnreadCounterMapper unreadCounterMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserClient userClient;
    @Autowired
    private NoteClient noteClient;

    private static final String UNREAD_PREFIX = "unread:count:"; // unread:count:{userId}:{group}
    private final Gson gson = new Gson();

    @Override
    public Page<NotificationVo> getNotifications(Long userId, String group, int page, int size) {
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
            .eq(Notification::getRecipientId, userId)
            .orderByDesc(Notification::getCreatedAt);

        if ("likeCollect".equals(group)) {
            query.in(Notification::getType, Arrays.asList("LIKE", "COLLECT"));
        } else if ("follow".equals(group)) {
            query.eq(Notification::getType, "FOLLOW");
        } else if ("comment".equals(group)) {
            query.in(Notification::getType, Arrays.asList("COMMENT", "REPLY"));
        }

        Page<Notification> result = notificationMapper.selectPage(p, query);
        
        Page<NotificationVo> voPage = new Page<>(page, size);
        BeanUtils.copyProperties(result, voPage, "records");
        
        // Fetch follow list for batch check
        Set<Integer> followingIds = new HashSet<>();
        try {
            Result<List<AttentionVo>> attentionRes = userClient.getAttention(userId.intValue());
            if (attentionRes != null && attentionRes.getData() != null) {
                // Use Gson to convert potentially LinkedHashMap to AttentionVo
                String json = gson.toJson(attentionRes.getData());
                List<AttentionVo> list = gson.fromJson(json, new TypeToken<List<AttentionVo>>(){}.getType());
                
                followingIds = list.stream()
                    .map(AttentionVo::getUserId)
                    .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.error("Failed to fetch attention list", e);
        }
        
        final Set<Integer> finalFollowingIds = followingIds;

        List<NotificationVo> vos = result.getRecords().stream().map(n -> {
            NotificationVo vo = new NotificationVo();
            BeanUtils.copyProperties(n, vo);
            
            // Fill Follow Status
            if (n.getActorId() != null) {
                vo.setIsFollowed(finalFollowingIds.contains(n.getActorId().intValue()));
            } else {
                vo.setIsFollowed(false);
            }
            
            // Fill Actor Info
            try {
                if (n.getActorId() != null) {
                    Result<User> userRes = userClient.getUserById(n.getActorId().intValue());
                    if (userRes != null && userRes.getData() != null) {
                        vo.setActor(userRes.getData());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to get user info for actorId: {}", n.getActorId(), e);
            }

            // Fill Note Cover
            if (n.getTargetId() != null) {
                boolean shouldFetchNote = false;
                Long noteId = null;

                if ("LIKE".equals(n.getType()) || "COLLECT".equals(n.getType())) {
                    if (n.getTargetType() != null && n.getTargetType().toUpperCase().contains("NOTE")) {
                        shouldFetchNote = true;
                        noteId = n.getTargetId();
                    }
                } else if ("COMMENT".equals(n.getType())) {
                    // COMMENT target is NOTE
                    shouldFetchNote = true;
                    noteId = n.getTargetId();
                }

                if (shouldFetchNote && noteId != null) {
                    try {
                        Result<Map<String, Object>> noteRes = noteClient.getNote(noteId);
                        if (noteRes != null && noteRes.getData() != null) {
                            Object img = noteRes.getData().get("image"); 
                            if (img != null) {
                                vo.setNoteCover(img.toString());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get note info for noteId: {}", noteId, e);
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(vos);
        return voPage;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processEvent(MessageEvent event) {
        // 1. Deduplication for LIKE/COLLECT/FOLLOW
        if ("LIKE".equals(event.getType()) || "COLLECT".equals(event.getType()) || "FOLLOW".equals(event.getType())) {
            // Check if exists
            Notification exist = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, event.getRecipientId())
                .eq(Notification::getActorId, event.getActorId())
                .eq(Notification::getType, event.getType())
                .eq(Notification::getTargetId, event.getTargetId())
            );
            
            if (exist != null) {
                // Idempotency check: if eventId is same, ignore
                if (event.getEventId().equals(exist.getEventId())) {
                    return;
                }
                // Update time and mark unread
                exist.setCreatedAt(LocalDateTime.now());
                exist.setIsRead(0);
                exist.setEventId(event.getEventId()); // Update event ID to new one
                notificationMapper.updateById(exist);
                // Increment counter and push
                updateCounterAndPush(event, exist);
                return;
            }
        }

        // 2. Save new notification
        Notification notification = new Notification();
        notification.setRecipientId(event.getRecipientId());
        notification.setActorId(event.getActorId());
        notification.setType(event.getType());
        notification.setTargetType(event.getTargetType());
        notification.setTargetId(event.getTargetId());
        notification.setContentSnapshot(event.getContentBrief());
        notification.setIsRead(0);
        notification.setEventId(event.getEventId());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException e) {
            log.info("Duplicate event processed: {}", event.getEventId());
            return;
        }

        updateCounterAndPush(event, notification);
    }

    private void updateCounterAndPush(MessageEvent event, Notification notification) {
        String group = getGroup(event.getType());
        Long userId = event.getRecipientId();

        // 1. Redis Increment
        String key = UNREAD_PREFIX + userId + ":" + group;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            UnreadCounter c = unreadCounterMapper.selectOne(new LambdaQueryWrapper<UnreadCounter>()
                .eq(UnreadCounter::getRecipientId, userId)
                .eq(UnreadCounter::getType, group));
            int dbCount = c != null ? c.getUnreadCount() : 0;
            redisTemplate.opsForValue().set(key, String.valueOf(dbCount));
        }
        redisTemplate.opsForValue().increment(key);

        // 2. DB Update
        UnreadCounter counter = unreadCounterMapper.selectOne(new LambdaQueryWrapper<UnreadCounter>()
            .eq(UnreadCounter::getRecipientId, userId)
            .eq(UnreadCounter::getType, group));
        
        if (counter == null) {
            counter = new UnreadCounter();
            counter.setRecipientId(userId);
            counter.setType(group);
            counter.setUnreadCount(1);
            unreadCounterMapper.insert(counter);
        } else {
            counter.setUnreadCount(counter.getUnreadCount() + 1);
            unreadCounterMapper.updateById(counter);
        }

        // 3. WS Push
        // Note: Session uses Integer userId, so we cast.
        Channel channel = Session.getChannel(userId.intValue());
        if (channel != null && channel.isActive()) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("group", group);
            msg.put("delta", 1);
            msg.put("latestNotification", notification);
            channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(msg)));
        }
    }

    private String getGroup(String type) {
        if ("LIKE".equals(type) || "COLLECT".equals(type)) return "likeCollect";
        if ("FOLLOW".equals(type)) return "follow";
        if ("COMMENT".equals(type) || "REPLY".equals(type)) return "comment";
        return "other";
    }

    @Override
    public Map<String, Integer> getUnreadSummary(Long userId) {
        Map<String, Integer> summary = new HashMap<>();
        String[] groups = {"likeCollect", "follow", "comment"};
        for (String group : groups) {
            String key = UNREAD_PREFIX + userId + ":" + group;
            String val = redisTemplate.opsForValue().get(key);
            if (val == null) {
                UnreadCounter c = unreadCounterMapper.selectOne(new LambdaQueryWrapper<UnreadCounter>()
                    .eq(UnreadCounter::getRecipientId, userId)
                    .eq(UnreadCounter::getType, group));
                int dbCount = c != null ? c.getUnreadCount() : 0;
                redisTemplate.opsForValue().set(key, String.valueOf(dbCount));
                summary.put(group, dbCount);
            } else {
                summary.put(group, Integer.parseInt(val));
            }
        }
        return summary;
    }


    private List<String> getTypesByGroup(String group) {
        if ("likeCollect".equals(group)) return Arrays.asList("LIKE", "COLLECT");
        if ("follow".equals(group)) return Arrays.asList("FOLLOW");
        if ("comment".equals(group)) return Arrays.asList("COMMENT", "REPLY");
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void markGroupRead(Long userId, String group) {
        List<String> types = getTypesByGroup(group);
        if (types.isEmpty()) return;

        // Update DB
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
            .eq(Notification::getRecipientId, userId)
            .in(Notification::getType, types)
            .eq(Notification::getIsRead, 0));

        // Update Redis
        String key = UNREAD_PREFIX + userId + ":" + group;
        redisTemplate.delete(key);

        // Update UnreadCounter DB
        UnreadCounter counter = new UnreadCounter();
        counter.setUnreadCount(0);
        unreadCounterMapper.update(counter, new LambdaQueryWrapper<UnreadCounter>()
            .eq(UnreadCounter::getRecipientId, userId)
            .eq(UnreadCounter::getType, group));
    }

    @Override
    public void markOneRead(Long userId, Long notificationId) {
        Notification n = new Notification();
        n.setId(notificationId);
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }
}
