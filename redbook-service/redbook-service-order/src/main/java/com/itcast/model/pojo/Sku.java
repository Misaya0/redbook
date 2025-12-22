package com.itcast.model.pojo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class Sku implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private String specs;
}

