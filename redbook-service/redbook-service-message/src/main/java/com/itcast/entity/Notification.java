package com.itcast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String type; // FOLLOW, LIKE, COLLECT, COMMENT, REPLY
    private String targetType; // USER, NOTE, COMMENT
    private Long targetId;
    private String contentSnapshot;
    private Integer isRead; // 0: unread, 1: read
    private String eventId;
    private LocalDateTime createdAt;
}
