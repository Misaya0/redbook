package com.itcast.service;

import com.itcast.model.dto.OrderDto;
import com.itcast.result.Result;

public interface OrderService {
    Result<Void> saveOrder(OrderDto orderDto);

    Result<Void> buyOrder(Long orderId);

    /**
     * 处理消息队列中的订单保存（带事务）
     * @param orderDto 订单DTO
     */
    void processOrderMessage(OrderDto orderDto);
}
