package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.context.UserContext;
import com.itcast.mapper.ShopMapper;
import com.itcast.model.pojo.Shop;
import com.itcast.result.Result;
import com.itcast.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public Result<Void> createShop(Shop shop) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }

        // 1. 检查是否已经开店
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getUserId, userId);
        Long count = shopMapper.selectCount(queryWrapper);
        if (count > 0) {
            return Result.failure("您已经拥有一家店铺，无法重复创建");
        }

        // 2. 创建店铺
        shop.setUserId(userId);
        shop.setTime(LocalDateTime.now());
        shop.setFans(0);
        shop.setSales(0);
        
        // 默认自动授权 (可根据需求调整)
        shop.setLinkAuthMode(0);

        shopMapper.insert(shop);
        return Result.success(null);
    }

    @Override
    public Result<Shop> getMyShop() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        return getShopByUserId(userId);
    }

    @Override
    public Result<Shop> getShopByUserId(Long userId) {
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shop::getUserId, userId);
        Shop shop = shopMapper.selectOne(queryWrapper);
        
        if (shop == null) {
            return Result.failure("该用户没有店铺");
        }
        
        return Result.success(shop);
    }

    @Override
    public Result<Shop> getShopById(Integer shopId) {
        if (shopId == null) {
            return Result.failure("店铺ID不能为空");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return Result.failure("店铺不存在");
        }
        return Result.success(shop);
    }
}
