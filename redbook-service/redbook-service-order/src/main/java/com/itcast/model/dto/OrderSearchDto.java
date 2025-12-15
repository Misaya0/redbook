package com.itcast.model.dto;

import lombok.Data;

@Data
public class OrderSearchDto {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer status;
    private String startTime;
    private String endTime;
    private Long orderId;
    private Integer userId;
}
