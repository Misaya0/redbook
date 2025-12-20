package com.itcast.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductSpecsVo {
    private Long productId;
    private List<SpecGroupVo> specGroups;
    private List<SkuSpecVo> skus;
}

