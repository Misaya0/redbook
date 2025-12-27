package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rb_shop")
public class Shop implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 店铺名称
     */
    @TableField("name")
    private String name;

    /**
     * 头像
     */
    @TableField("image")
    private String image;

    /**
     * 成立时间
     */
    @TableField("time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;

    /**
     * 粉丝
     */
    @TableField("fans")
    private Integer fans;

    /**
     * 销量
     */
    @TableField("sales")
    private Integer sales;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 挂链授权模式: 0-自动, 1-手动
     */
    @TableField("link_auth_mode")
    private Integer linkAuthMode;
}
