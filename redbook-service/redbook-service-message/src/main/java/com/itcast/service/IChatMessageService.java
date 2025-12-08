package com.itcast.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcast.entity.ChatMessage;
import java.util.List;

public interface IChatMessageService extends IService<ChatMessage> {
    List<ChatMessage> getHistory(Long userId, Long talkerId);
    ChatMessage sendMessage(Long senderId, Long receiverId, String content, Integer type);
}
