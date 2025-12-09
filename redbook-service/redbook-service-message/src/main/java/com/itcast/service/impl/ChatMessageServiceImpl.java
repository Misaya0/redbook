package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.entity.ChatMessage;
import com.itcast.entity.Conversation;
import com.itcast.mapper.ChatMessageMapper;
import com.itcast.service.IChatMessageService;
import com.itcast.service.IConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

    @Autowired
    private IConversationService conversationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ChatMessage> getHistory(Long userId, Long talkerId) {
        return list(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, talkerId)
                        .or().eq(ChatMessage::getSenderId, talkerId).eq(ChatMessage::getReceiverId, userId))
                .orderByAsc(ChatMessage::getCreateTime));
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, Integer type) {
        // 1. 保存消息
        ChatMessage message = new ChatMessage();
//        senderId = 1L;
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMsgType(type);
        message.setCreateTime(LocalDateTime.now());
        message.setIsDeleted(false);
        save(message);

        // 2. 更新发送方的会话信息
        conversationService.updateLastMessage(senderId, receiverId, content);

        // 3. 更新接收方的会话信息
        updateReceiverConversation(senderId, receiverId, content);

        // 4. 如果接收方在线，则实时推送消息
        io.netty.channel.Channel channel = com.itcast.session.Session.getChannel(receiverId.intValue());
        if (channel != null && channel.isActive()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(json));
            } catch (Exception e) {
                log.error("序列化消息失败", e);
            }
        }

        return message;
    }

    private void updateReceiverConversation(Long senderId, Long receiverId, String content) {
        Conversation conversation = conversationService.getOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, receiverId)
                .eq(Conversation::getTalkerId, senderId));

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(receiverId);
            conversation.setTalkerId(senderId);
            conversation.setUnreadCount(0);
            conversation.setIsTop(false);
            conversation.setIsMuted(false);
            conversation.setCreateTime(LocalDateTime.now());
        }

        conversation.setLastMessageContent(content);
        conversation.setLastMessageTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        conversationService.saveOrUpdate(conversation);
    }
}
