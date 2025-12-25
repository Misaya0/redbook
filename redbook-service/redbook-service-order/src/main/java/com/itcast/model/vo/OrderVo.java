package com.itcast.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderVo {
    private Long id;
    private Integer productId;
    private Long skuId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal skuPrice;
    private Integer couponId;
    private BigDecimal finalPrice;
    private Long userId;
    private Integer status;
    private String createTime;

    /**
     * SKU规格描述 (如: 颜色:红 / 尺码:L)
     */
    private String skuSpec;

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 店铺头像
     */
    private String storeAvatar;

    /**
     * 商家备注
     */
    private String merchantMemo;

    /**
     * 买家名称
     */
    private String buyerName;

    /**
     * 买家头像
     */
    private String buyerAvatar;

    /**
     * 买家电话
     */
    private String buyerPhone;
}
