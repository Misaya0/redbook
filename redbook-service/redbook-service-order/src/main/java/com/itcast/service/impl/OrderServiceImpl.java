package com.itcast.service.impl;

import com.itcast.client.ProductClient;
import com.itcast.constant.MqConstant;
import com.itcast.enums.OrderStatusEnum;
import com.itcast.mapper.OrderAttributeMapper;
import com.itcast.mapper.OrderMapper;
import com.itcast.model.dto.OrderDto;
import com.itcast.model.pojo.CustomAttribute;
import com.itcast.model.pojo.Order;
import com.itcast.model.pojo.OrderAttribute;
import com.itcast.model.pojo.Product;
import com.itcast.result.Result;
import com.itcast.service.OrderService;
import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                Integer stock = product.getStock();
                Integer sales = product.getSales();
                if (stock > 0) {
                    log.info("扣减库存,增加销量");
                    product.setStock(stock - 1);
                    product.setSales(sales + 1);
                    productClient.updateProduct(product);

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
    @Transactional(rollbackFor = Exception.class)
    public void processOrderMessage(OrderDto orderDto) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户ID不能为空，请检查UserThreadLocal是否已正确设置");
        }

        // 幂等性检查：使用消息ID（消息ID在Consumer中已保证一定存在）
        String messageId = orderDto.getMessageId();
        
        // 使用Redis原子操作setIfAbsent防止并发处理（Redis单线程保证原子性）
        String processedKey = "order:message:processed:" + messageId;
        // setIfAbsent是原子操作：如果key不存在则设置并返回true，如果已存在则返回false
        // 设置临时值"1"并设置过期时间30秒（防止处理失败时留下永久标记）
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", 30, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isNew)) {
            // key已存在，说明消息已处理过或正在处理中
            log.warn("消息已处理过或正在处理中，跳过处理，messageId: {}", messageId);
            throw new IllegalStateException("消息已处理过，messageId: " + messageId);
        }

        try {
            // 执行业务逻辑（订单保存）
            saveOrderInternal(orderDto, userId);

            // 订单保存成功，标记消息为已处理（在事务提交前标记，确保数据一致性）
            // 将临时值"1"更新为"0"，并设置过期时间7天
            redisTemplate.opsForValue().set(processedKey, "0", 7, TimeUnit.DAYS);
            log.info("消息已标记为已处理，messageId: {}", messageId);
        } catch (Exception e) {
            // 处理失败，删除临时标记，允许重试
            redisTemplate.delete(processedKey);
            throw e;
        }
    }

    /**
     * 保存订单的内部方法（提取公共逻辑）
     */
    private void saveOrderInternal(OrderDto orderDto, Integer userId) {
        // 保存订单
        Order order = new Order();
        order.setProductId(orderDto.getProductId());
        order.setQuantity(orderDto.getQuantity());
        order.setCouponId(orderDto.getCouponId());
        order.setUserId(userId);
        order.setStatus(OrderStatusEnum.DUE.getCode());

        // 计算最终价格：(单价 * 数量) - 优惠券价格
        BigDecimal finalPrice = orderDto.getPrice()
                .multiply(BigDecimal.valueOf(orderDto.getQuantity()));
        if (orderDto.getCouponPrice() != null) {
            finalPrice = finalPrice.subtract(orderDto.getCouponPrice());
        }
        // 确保价格不为负数
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }
        order.setFinalPrice(finalPrice);

        orderMapper.insert(order);

        // 保存订单属性
        Long orderId = order.getId();
        if (orderId == null) {
            throw new IllegalStateException("订单ID生成失败");
        }

        List<CustomAttribute> selectAttributes = orderDto.getSelectAttributes();
        if (selectAttributes != null && !selectAttributes.isEmpty()) {
            for (CustomAttribute selectAttribute : selectAttributes) {
                if (selectAttribute == null) {
                    log.warn("订单属性为空，跳过");
                    continue;
                }

                List<String> values = selectAttribute.getValue();
                if (values == null || values.isEmpty()) {
                    log.warn("订单属性值为空，跳过属性：{}", selectAttribute.getLabel());
                    continue;
                }

                OrderAttribute orderAttribute = new OrderAttribute();
                orderAttribute.setOrderId(orderId);
                orderAttribute.setLabel(selectAttribute.getLabel());
                orderAttribute.setValue(values.get(0));
                orderAttributeMapper.insert(orderAttribute);
            }
        }
    }
}