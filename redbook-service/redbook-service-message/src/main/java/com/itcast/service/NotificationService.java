package com.itcast.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.entity.Notification;
import com.itcast.model.event.MessageEvent;
import com.itcast.model.vo.NotificationVo;
import java.util.Map;

public interface NotificationService {
    void processEvent(MessageEvent event);
    Map<String, Integer> getUnreadSummary(Long userId);

    Page<NotificationVo> getNotifications(Long userId, String group, int page, int size);

    void markGroupRead(Long userId, String group);
    void markOneRead(Long userId, Long notificationId);
}
