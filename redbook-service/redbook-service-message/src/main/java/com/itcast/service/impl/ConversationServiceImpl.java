package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcast.client.UserClient;
import com.itcast.entity.Conversation;
import com.itcast.mapper.ConversationMapper;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.ConversationVo;
import com.itcast.result.Result;
import com.itcast.service.IConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements IConversationService {

    @Autowired
    private UserClient userClient;

    @Override
    public List<ConversationVo> getConversationList(Long userId) {
        // 1. 获取基础会话列表
        List<Conversation> conversationList = list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdateTime));

        List<ConversationVo> conversationVos = new ArrayList<>();

        // 2. 遍历并填充用户信息
        // 注意：循环调用远程接口性能较差，生产环境建议优化为批量查询或使用缓存
        for (Conversation conversation : conversationList) {
            ConversationVo vo = new ConversationVo();
            BeanUtils.copyProperties(conversation, vo);

            try {
                // 调用用户服务获取目标用户信息
                Long talkerId = conversation.getTalkerId();
                if (talkerId != null) {
                    Result<User> userResult = userClient.getUserById(talkerId.intValue());
                    if (userResult != null && userResult.getData() != null) {
                        User user = userResult.getData();
                        vo.setTargetName(user.getNickname()); // 使用 nickname 作为显示名称
                        vo.setTargetAvatar(user.getImage());
                    }
                }
            } catch (Exception e) {
                log.error("获取用户信息失败，userId: {}", conversation.getTalkerId(), e);
                // 失败时可以设置默认值或保留为空
                vo.setTargetName("未知用户");
            }
            conversationVos.add(vo);
        }

        return conversationVos;
    }

    @Override
    public void updateLastMessage(Long userId, Long talkerId, String content) {
        Conversation conversation = getOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getTalkerId, talkerId));

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTalkerId(talkerId);
            conversation.setUnreadCount(0);
            conversation.setIsTop(false);
            conversation.setIsMuted(false);
            conversation.setCreateTime(LocalDateTime.now());
        }

        conversation.setLastMessageContent(content);
        conversation.setLastMessageTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(conversation);
    }

    @Override
    public void clearUnreadCount(Long userId, Long talkerId) {
        Conversation conversation = getOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getTalkerId, talkerId));

        if (conversation != null && conversation.getUnreadCount() > 0) {
            conversation.setUnreadCount(0);
            updateById(conversation);
        }
    }
}
