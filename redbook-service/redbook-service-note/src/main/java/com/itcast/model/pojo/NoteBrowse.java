package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.itcast.config.ShardingSphereLocalDateTimeTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "rb_note_browse", autoResultMap = true)
public class NoteBrowse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 笔记id
     */
    @TableField("note_id")
    private Long noteId;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 浏览时间
     */
    @TableField(value = "time", typeHandler = ShardingSphereLocalDateTimeTypeHandler.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
}
