package com.itcast.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuSpecVo {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private Map<String, String> specs;
}

