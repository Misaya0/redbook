package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("rb_note")
public class Note implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 内容
     */
    @TableField("content")
    private String content;

    /**
     * 图片
     */
    @TableField("image")
    private String image;

    /**
     * 发布时间
     */
    @TableField("time")
    private String time;

    /**
     * 类型
     */
    @TableField("type")
    private String type;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    /**
     * 经度
     */
    @TableField("longitude")
    private Double longitude;

    /**
     * 纬度
     */
    @TableField("latitude")
    private Double latitude;

    /**
     * 点赞
     */
    @TableField("`like`")
    private Integer like;

    /**
     * 收藏
     */
    @TableField("collection")
    private Integer collection;

    /**
     * 发布人id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 关联店铺ID
     */
    @TableField("shop_id")
    private Long shopId;

    /**
     * 挂链状态: 0-待审, 1-正常, 2-拒绝
     */
    @TableField("product_link_status")
    private Integer productLinkStatus;
}
