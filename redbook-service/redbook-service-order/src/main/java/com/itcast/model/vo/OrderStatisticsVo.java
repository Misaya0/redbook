package com.itcast.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderStatisticsVo {
    private Long totalOrders;
    private BigDecimal totalAmount;
    private Long pendingPayment;
    private Long paid;
    private Long shipped;
    private Long completed;
    private Long cancelled;
}
