package com.itcast.model.vo;

import com.itcast.model.pojo.Note;
import com.itcast.model.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoteVo extends Note {

    /**
     * 发布笔记用户信息
     */
    private User user;

    /**
     * 关联商品（用于前端商品卡片展示）
     */
    private ProductSimpleVo product;

    /**
     * 处理后的时间
     */
    private String dealTime;

    /**
     * 距离
     */
    private String distance;

    /**
     * 评论数
     */
    private Integer comment;
}
