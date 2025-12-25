package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("rb_comment")
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 内容
     */
    private String content;

    /**
     * 时间
     */
    private String time;

    /**
     * 父id--0代表是评论，非0代表回复，parentId代表回复哪个评论或回复
     */
    @TableField("parent_id")
    private Long parentId;

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
     * 回复目标用户id
     */
    @TableField("target_user_id")
    private Long targetUserId;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;
}
