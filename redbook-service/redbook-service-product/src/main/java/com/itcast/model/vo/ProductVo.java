package com.itcast.model.vo;

import com.itcast.model.pojo.CustomAttribute;
import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.Shop;
import com.itcast.model.pojo.Sku;
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
     * 商品属性 (兼容旧版)
     */
    private List<CustomAttribute> customAttributes;

    /**
     * SKU列表
     */
    private List<Sku> skus;

    /**
     * 关联种草笔记 (MVP新增)
     */
    private List<NoteSimpleVo> relatedNotes;

    /**
     * 总库存 (聚合SKU)
     */
    private Integer totalStock;
}
