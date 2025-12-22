package com.itcast.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息(订单服务内的简化模型，用于跨服务数据组装)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String image;

    /**
     * 角色: 0-普通, 1-商家
     */
    private Integer role;
}

