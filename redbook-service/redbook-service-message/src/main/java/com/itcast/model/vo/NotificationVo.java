package com.itcast.model.vo;

import com.itcast.entity.Notification;
import com.itcast.model.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationVo extends Notification {
    private User actor;
    private String noteCover;
    private Boolean isFollowed;
}
