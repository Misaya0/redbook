package com.itcast.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AttentionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String image;

    /**
     * 关注时间
     */
    private String time;
}
