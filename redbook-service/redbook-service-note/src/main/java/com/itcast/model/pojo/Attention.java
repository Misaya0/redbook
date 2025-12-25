package com.itcast.model.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Attention implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 被关注者id
     */
    private Long otherId;

    /**
     * 关注者id
     */
    private Long ownId;

    /**
     * 关注时间
     */
    private String time;
}
