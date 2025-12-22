package com.itcast.service;

import com.itcast.model.dto.OrderDto;
import com.itcast.model.dto.OrderSearchDto;
import com.itcast.model.pojo.Order;
import com.itcast.model.vo.OrderStatisticsVo;
import com.itcast.model.vo.OrderVo;
import com.itcast.result.Result;

import java.util.List;

public interface OrderService {
    Result<Void> saveOrder(OrderDto orderDto);

    Result<Void> buyOrder(Long orderId);

    Result<List<OrderVo>> searchOrders(OrderSearchDto searchDto);

    Result<OrderVo> getOrderDetail(Long orderId);

    Result<Void> updateOrderStatus(Long orderId, Integer status);
    
    Result<OrderStatisticsVo> getStatistics();

    Result<Long> getServerTime();

    Result<Integer> getPaymentTimeoutSeconds();

    Result<Void> timeoutCancelOrder(Long orderId);

    /**
     * 处理消息队列中的订单保存（带事务）
     * @param orderDto 订单DTO
     */
    void processOrderMessage(OrderDto orderDto);
}
