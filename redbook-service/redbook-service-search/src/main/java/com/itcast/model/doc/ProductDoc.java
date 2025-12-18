package com.itcast.model.doc;

import lombok.Data;

@Data
public class ProductDoc {
    private Long id;
    private String name;
    private String title;
    private Double price;
    private Integer stock;
    private Integer sales;
    private Long shopId;
    private Long categoryId;
    private String image;
    private String createTime;
    
    // Suggestion or other fields if needed
    private String suggestion;
}
