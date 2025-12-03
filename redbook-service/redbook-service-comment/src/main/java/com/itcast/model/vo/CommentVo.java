package com.itcast.model.vo;

import com.itcast.model.pojo.Comment;
import com.itcast.model.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentVo extends Comment {
    /**
     * 评论void
     */
    private String commentVoId;

    /**
     * 处理时间
     */
    private String dealTime;

    /**
     * 用户
     */
    private User user;

    /**
     * 子评论列表
     */
    private List<CommentVo> childrenList;

    /**
     * 是否点赞
     */
    private Boolean isLiked;

    /**
     * 回复目标用户昵称
     */
    private String replyToUser;
}
