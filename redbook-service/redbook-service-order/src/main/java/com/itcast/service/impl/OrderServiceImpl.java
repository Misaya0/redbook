package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.ProductClient;
import com.itcast.constant.MqConstant;
import com.itcast.context.UserContext;
import com.itcast.enums.OrderStatusEnum;
import com.itcast.mapper.OrderAttributeMapper;
import com.itcast.mapper.OrderMapper;
import com.itcast.model.dto.OrderDto;
import com.itcast.model.dto.OrderSearchDto;
import com.itcast.model.pojo.Order;
import com.itcast.model.pojo.Product;
import com.itcast.model.vo.OrderStatisticsVo;
import com.itcast.model.vo.OrderVo;
import com.itcast.result.Result;
import com.itcast.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private OrderAttributeMapper orderAttributeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<Void> saveOrder(OrderDto orderDto) {
        // 扣减库存--防止超卖
        RLock lock = redissonClient.getLock("saveOrder：" + orderDto.getProductId());
        try {
            boolean res = lock.tryLock(100, TimeUnit.SECONDS);
            if (res) {
                // 获取商品库存
                Product product = productClient.getProductById(orderDto.getProductId()).getData();
                if (product == null) {
                    return Result.failure("商品不存在");
                }
                Integer stock = product.getStock();
                Integer sales = product.getSales();
                if (stock > 0) {
                    log.info("扣减库存,增加销量");
                    product.setStock(stock - 1);
                    product.setSales(sales + 1);
                    productClient.updateProduct(product);

                    // 设置当前用户ID到DTO
                    orderDto.setUserId(UserContext.getUserId());
                    
                    // 异步保存订单
                    rabbitTemplate.convertAndSend(MqConstant.SAVE_ORDER_EXCHANGE, "", orderDto);
                } else {
                    log.info("商品已经售空");
                    return Result.failure("商品已经售空");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }

        return Result.success(null);
    }

    @Override
    public Result<Void> buyOrder(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatusEnum.PAID.getCode());
        orderMapper.updateById(order);
        return Result.success(null);
    }

    @Override
    public Result<List<OrderVo>> searchOrders(OrderSearchDto searchDto) {
        Page<Order> page = new Page<>(searchDto.getPageNum(), searchDto.getPageSize());
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();

        if (searchDto.getStatus() != null) {
            queryWrapper.eq("status", searchDto.getStatus());
        }
        if (searchDto.getOrderId() != null) {
            queryWrapper.eq("id", searchDto.getOrderId());
        }
        if (searchDto.getUserId() != null) {
            queryWrapper.eq("user_id", searchDto.getUserId());
        }
        if (StringUtils.hasText(searchDto.getStartTime())) {
            queryWrapper.ge("create_time", searchDto.getStartTime());
        }
        if (StringUtils.hasText(searchDto.getEndTime())) {
            queryWrapper.le("create_time", searchDto.getEndTime());
        }

        queryWrapper.orderByDesc("id"); // 假设ID是自增或雪花，近似时间排序

        Page<Order> orderPage = orderMapper.selectPage(page, queryWrapper);
        List<Order> records = orderPage.getRecords();

        List<OrderVo> orderVos = records.stream().map(order -> {
            OrderVo vo = new OrderVo();
            BeanUtils.copyProperties(order, vo);
            
            // 填充商品信息
            try {
                Result<Product> productResult = productClient.getProductById(order.getProductId());
                if (productResult != null && productResult.getData() != null) {
                    vo.setProductName(productResult.getData().getName());
                    vo.setProductImage(productResult.getData().getImage());
                }
            } catch (Exception e) {
                log.error("获取商品信息失败: " + e.getMessage());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(orderVos, orderPage.getTotal());
    }

    @Override
    public Result<OrderVo> getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.failure("订单不存在");
        }
        OrderVo vo = new OrderVo();
        BeanUtils.copyProperties(order, vo);
        try {
            Result<Product> productResult = productClient.getProductById(order.getProductId());
            if (productResult != null && productResult.getData() != null) {
                vo.setProductName(productResult.getData().getName());
                vo.setProductImage(productResult.getData().getImage());
            }
        } catch (Exception e) {
            log.error("获取商品信息失败: " + e.getMessage());
        }
        return Result.success(vo);
    }

    @Override
    public Result<OrderStatisticsVo> getStatistics() {
        OrderStatisticsVo vo = new OrderStatisticsVo();
        
        // Count total
        vo.setTotalOrders(orderMapper.selectCount(null));
        
        // Count by status
        vo.setPendingPayment(countByStatus(OrderStatusEnum.DUE.getCode()));
        vo.setPaid(countByStatus(OrderStatusEnum.PAID.getCode()));
        vo.setShipped(countByStatus(OrderStatusEnum.SHIPPED.getCode()));
        vo.setCompleted(countByStatus(OrderStatusEnum.COMPLETED.getCode()));
        vo.setCancelled(countByStatus(OrderStatusEnum.CANCEL.getCode()));
        
        vo.setTotalAmount(BigDecimal.ZERO); // 暂不统计金额
        
        return Result.success(vo);
    }
    
    private Long countByStatus(Integer status) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return orderMapper.selectCount(queryWrapper);
    }

    @Override
    public Result<Void> updateOrderStatus(Long orderId, Integer status) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(status);
        orderMapper.updateById(order);
        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrderMessage(OrderDto orderDto) {
        // Integer userId = UserContext.getUserId(); // 消费者线程中没有 UserContext
        
        Order order = new Order();
        BeanUtils.copyProperties(orderDto, order);
        
        // userId 应该从 orderDto 中获取
        if (order.getUserId() == null) {
             // 如果 DTO 里没有，说明上游没传，这里可能是个问题，但在消息队列中我们无法获取 Context
             log.warn("OrderDto missing userId");
        }
        
        order.setStatus(OrderStatusEnum.DUE.getCode());
        order.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        orderMapper.insert(order);
    }
}
