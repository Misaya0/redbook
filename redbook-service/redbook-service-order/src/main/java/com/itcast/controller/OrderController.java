package com.itcast.controller;

import com.itcast.model.dto.OrderDto;
import com.itcast.model.dto.OrderSearchDto;
import com.itcast.model.vo.OrderStatisticsVo;
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
        // 这里可以调用 searchOrders，传入当前 userId
        // 但目前先留空，或后续实现
        return null;
    }

    @Operation(summary = "搜索订单", description = "管理后台搜索订单")
    @PostMapping("/search")
    public Result<List<OrderVo>> searchOrders(
            @Parameter(description = "搜索条件") @RequestBody OrderSearchDto searchDto) {
        return orderService.searchOrders(searchDto);
    }

    @Operation(summary = "获取订单详情", description = "获取订单详细信息")
    @GetMapping("/detail/{orderId}")
    public Result<OrderVo> getOrderDetail(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

    @Operation(summary = "更新订单状态", description = "更新订单状态")
    @PutMapping("/status/{orderId}")
    public Result<Void> updateOrderStatus(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "状态码", required = true) @RequestParam Integer status) {
        return orderService.updateOrderStatus(orderId, status);
    }

    @Operation(summary = "获取订单统计", description = "获取订单统计数据")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVo> getStatistics() {
        return orderService.getStatistics();
    }
}
