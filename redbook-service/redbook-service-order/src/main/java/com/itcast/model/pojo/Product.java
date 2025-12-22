package com.itcast.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 分类id（商品服务返回的字段）
     */
    private Integer categoryId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品价格
     */
    private Double price;

    /**
     * 商品图片
     */
    private String image;

    private String mainImage;

    /**
     * 发布时间
     */
    private String time;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 店铺id
     */
    private Integer shopId;

    /**
     * 库存
     */
    private Integer stock;
}
