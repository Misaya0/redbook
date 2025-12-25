package com.itcast.model.dto;

import com.itcast.model.pojo.CustomAttribute;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDto implements Serializable {

    /**
     * 产品id
     */
    private Integer productId;

    /**
     * 店铺id
     */
    private Integer shopId;

    /**
     * skuId
     */
    private Long skuId;

    /**
     * 产品价格
     */
    private BigDecimal price;

    /**
     * sku单价快照
     */
    private BigDecimal skuPrice;

    /**
     * 选择的属性
     */
    private List<CustomAttribute> selectAttributes;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 优惠券id
     */
    private Integer couponId;

    /**
     * 优惠券价格
     */
    private BigDecimal couponPrice;

    /**
     * 最终价格
     */
    private BigDecimal finalPrice;

    /**
     * 消息唯一标识（用于幂等性控制）
     */
    private String messageId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品名称（冗余）
     */
    private String productName;
}
