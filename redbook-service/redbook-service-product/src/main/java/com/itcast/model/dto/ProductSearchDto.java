package com.itcast.model.dto;

import lombok.Data;

@Data
public class ProductSearchDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Double minPrice;
    private Double maxPrice;
    private Integer shopId;
    private Long categoryId;
}
