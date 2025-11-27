package com.itcast.model.vo;

import com.itcast.model.pojo.CustomAttribute;
import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.Shop;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductVo extends Product {

    /**
     * 店铺
     */
    private Shop shop;

    /**
     * 商品属性
     */
    private List<CustomAttribute> customAttributes;
}
