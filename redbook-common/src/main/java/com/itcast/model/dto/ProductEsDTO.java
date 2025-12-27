package com.itcast.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductEsDTO {
    private Long id;
    private String name;
    private String title;
    private Double price;
    private Integer stock;
    private Integer sales;
    private Long shopId;
    private Long categoryId;
    private String image;
    private LocalDateTime createTime;
}
