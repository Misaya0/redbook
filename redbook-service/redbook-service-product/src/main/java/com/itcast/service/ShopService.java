package com.itcast.service;

import com.itcast.model.pojo.Shop;
import com.itcast.result.Result;

public interface ShopService {
    /**
     * 创建店铺
     * @param shop 店铺信息
     * @return 结果
     */
    Result<Void> createShop(Shop shop);

    /**
     * 获取我的店铺
     * @return 店铺信息
     */
    Result<Shop> getMyShop();

    /**
     * 根据用户ID获取店铺
     * @param userId 用户ID
     * @return 店铺信息
     */
    Result<Shop> getShopByUserId(Integer userId);
}
