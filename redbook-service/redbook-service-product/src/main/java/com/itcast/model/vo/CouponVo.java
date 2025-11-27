package com.itcast.model.vo;

import com.itcast.model.pojo.Coupon;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CouponVo extends Coupon {
    /**
     * 是否可用
     */
    private Boolean isUsable;
}
