package com.itcast.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rb_message")
public class ChatMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer msgType;
    private LocalDateTime createTime;
    private Boolean isDeleted;
}
