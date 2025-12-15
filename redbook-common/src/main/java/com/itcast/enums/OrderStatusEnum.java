package com.itcast.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    DUE(0, "待付款"),
    PAID(1, "已付款"),
    CANCEL(2, "已取消"),
    SHIPPED(3, "已发货"),
    COMPLETED(4, "已完成");

    private final int code;
    private final String status;

    OrderStatusEnum(int code, String status) {
        this.code = code;
        this.status = status;
    }
}
