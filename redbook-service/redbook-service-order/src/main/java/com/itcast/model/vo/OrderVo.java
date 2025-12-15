package com.itcast.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderVo {
    private Long id;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Integer couponId;
    private BigDecimal finalPrice;
    private Integer userId;
    private Integer status;
    private String createTime;
}
