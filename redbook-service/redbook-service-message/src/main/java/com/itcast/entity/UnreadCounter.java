package com.itcast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("unread_counter")
public class UnreadCounter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientId;
    private String type; // LIKE_COLLECT, FOLLOW, COMMENT
    private Integer unreadCount;
    private LocalDateTime updatedAt;
}
