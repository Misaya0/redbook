package com.itcast.model.dto;

import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.ProductAttribute;
import com.itcast.model.pojo.Sku;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductDto extends Product {

    /**
     * 商品属性 (兼容旧版)
     */
    private ProductAttribute productAttribute;

    /**
     * SKU列表 (新版必填)
     */
    private List<Sku> skus;
}
