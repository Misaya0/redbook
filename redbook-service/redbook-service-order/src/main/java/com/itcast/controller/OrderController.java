package com.itcast.controller;

import com.itcast.model.dto.OrderDto;
import com.itcast.model.vo.OrderVo;
import com.itcast.result.Result;
import com.itcast.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单模块", description = "订单管理相关接口")
@RestController
@RequestMapping("/order")
public class OrderController {


    @Autowired
    private OrderService orderService;

    @Operation(summary = "保存订单", description = "创建并保存一个新订单")
    @PostMapping("/saveOrder")
    public Result<Void> saveOrder(
            @Parameter(description = "订单信息", required = true) @RequestBody OrderDto orderDto) {
        return orderService.saveOrder(orderDto);
    }

    @Operation(summary = "购买订单", description = "提交购买指定订单，完成支付")
    @PutMapping("/buyOrder/{orderId}")
    public Result<Void> buyOrder(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        return orderService.buyOrder(orderId);
    }

    @Operation(summary = "获取订单列表", description = "获取当前用户的所有订单")
    @GetMapping("/getOrderList")
    public Result<List<OrderVo>> getOrderList() {
        return null;
    }
}
