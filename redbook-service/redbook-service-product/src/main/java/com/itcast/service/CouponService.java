package com.itcast.service;

import com.itcast.model.vo.CouponVo;
import com.itcast.result.Result;

import java.util.List;

public interface CouponService {
    Result<List<CouponVo>> getCouponsByUserId();

    Result<Void> useCoupon(Integer couponId);
}
