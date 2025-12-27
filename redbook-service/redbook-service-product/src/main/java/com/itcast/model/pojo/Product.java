package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rb_product")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 店铺ID
     */
    @TableField("shop_id")
    private Long shopId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 副标题/卖点
     */
    @TableField("title")
    private String title;

    /**
     * 展示价格 (起售价)
     */
    @TableField("price")
    private Double price;

    /**
     * 主图
     */
    @TableField("main_image")
    private String mainImage;

    /**
     * 详情图(JSON数组)
     */
    @TableField("detail_images")
    private String detailImages;

    /**
     * 销量
     */
    @TableField("sales")
    private Integer sales;

    /**
     * 状态: 0-下架, 1-上架
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
