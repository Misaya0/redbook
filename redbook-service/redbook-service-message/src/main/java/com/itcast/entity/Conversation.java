package com.itcast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rb_conversation")
public class Conversation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long talkerId;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Boolean isTop;
    private Boolean isMuted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isDeleted;
}
