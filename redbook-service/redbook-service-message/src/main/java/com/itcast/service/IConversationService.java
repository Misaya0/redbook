package com.itcast.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcast.entity.Conversation;
import com.itcast.model.vo.ConversationVo;

import java.util.List;

public interface IConversationService extends IService<Conversation> {
    List<ConversationVo> getConversationList(Long userId);

    void updateLastMessage(Long userId, Long talkerId, String content);

    void clearUnreadCount(Long userId, Long talkerId);
}
