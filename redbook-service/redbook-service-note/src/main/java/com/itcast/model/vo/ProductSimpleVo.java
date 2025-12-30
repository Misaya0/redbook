package com.itcast.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品简要信息（笔记服务用于展示商品卡片）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSimpleVo {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品价格（展示价格/起售价）
     */
    private BigDecimal price;
}
