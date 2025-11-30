package com.itcast.controller;

import com.itcast.model.vo.CouponVo;
import com.itcast.result.Result;
import com.itcast.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "优惠券模块", description = "优惠券管理相关接口")
@RestController
@RequestMapping("/product")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Operation(summary = "获取用户优惠券", description = "获取当前用户的所有优惠券")
    @GetMapping("/getCouponsByUserId")
    public Result<List<CouponVo>> getCouponsByUserId() {
        return couponService.getCouponsByUserId();
    }

    @Operation(summary = "使用优惠券", description = "使用指定的优惠券")
    @GetMapping("/useCoupon/{couponId}")
    public Result<Void> useCoupon(
            @Parameter(description = "优惠券ID", required = true) @PathVariable Integer couponId) {
        return couponService.useCoupon(couponId);
    }
}
