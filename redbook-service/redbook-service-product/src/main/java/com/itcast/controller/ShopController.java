package com.itcast.controller;

import com.itcast.model.pojo.Shop;
import com.itcast.result.Result;
import com.itcast.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "店铺模块", description = "店铺管理相关接口")
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Operation(summary = "创建店铺", description = "商家创建店铺，一个商家只能创建一个店铺")
    @PostMapping("/create")
    public Result<Void> createShop(
            @Parameter(description = "店铺信息", required = true) @RequestBody Shop shop) {
        return shopService.createShop(shop);
    }

    @Operation(summary = "获取我的店铺", description = "获取当前登录商家的店铺信息")
    @GetMapping("/myShop")
    public Result<Shop> getMyShop() {
        return shopService.getMyShop();
    }

    @Operation(summary = "根据用户ID获取店铺", description = "获取指定用户的店铺信息")
    @GetMapping("/getShopByUserId/{userId}")
    public Result<Shop> getShopByUserId(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Integer userId) {
        return shopService.getShopByUserId(userId);
    }

    @Operation(summary = "根据店铺ID获取店铺", description = "根据店铺ID获取店铺信息")
    @GetMapping("/getShopById/{shopId}")
    public Result<Shop> getShopById(
            @Parameter(description = "店铺ID", required = true) @PathVariable("shopId") Integer shopId) {
        return shopService.getShopById(shopId);
    }
}
