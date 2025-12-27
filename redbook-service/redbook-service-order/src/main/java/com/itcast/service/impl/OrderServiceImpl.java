package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.ProductClient;
import com.itcast.client.UserClient;
import com.itcast.constant.MqConstant;
import com.itcast.context.UserContext;
import com.itcast.enums.OrderStatusEnum;
import com.itcast.mapper.OrderAttributeMapper;
import com.itcast.mapper.OrderMapper;
import com.itcast.model.dto.OrderDto;
import com.itcast.model.dto.OrderSearchDto;
import com.itcast.model.pojo.*;
import com.itcast.model.vo.OrderStatisticsVo;
import com.itcast.model.vo.OrderVo;
import com.itcast.result.Result;
import com.itcast.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int DEFAULT_PAYMENT_TIMEOUT_SECONDS = 120;
    private static final String SKU_STOCK_KEY_PREFIX = "product:stock:";
    private static final String MQ_STOCK_ROLLBACK_KEY_PREFIX = "order:mq:stockRollback:";

    /**
     * Lua 脚本：校验库存是否充足，充足则扣减库存并返回 1，否则返回 -1
     */
    private static final DefaultRedisScript<Long> DEDUCT_STOCK_SCRIPT;

    static {
        DEDUCT_STOCK_SCRIPT = new DefaultRedisScript<>();
        DEDUCT_STOCK_SCRIPT.setResultType(Long.class);
        DEDUCT_STOCK_SCRIPT.setScriptText(
                "local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')\n" +
                "local qty = tonumber(ARGV[1])\n" +
                "if (not qty) or qty <= 0 then\n" +
                "  return -1\n" +
                "end\n" +
                "if stock >= qty then\n" +
                "  redis.call('DECRBY', KEYS[1], qty)\n" +
                "  return 1\n" +
                "end\n" +
                "return -1"
        );
    }

    @Value("${order.payment-timeout-seconds:120}")
    private int paymentTimeoutSeconds;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private OrderAttributeMapper orderAttributeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private int resolvedPaymentTimeoutSeconds() {
        return paymentTimeoutSeconds > 0 ? paymentTimeoutSeconds : DEFAULT_PAYMENT_TIMEOUT_SECONDS;
    }

    @Override
    public Result<Void> saveOrder(OrderDto orderDto) {
        // 1) 参数校验：下单请求的基本合法性检查
        if (orderDto == null) {
            return Result.failure("参数不能为空");
        }
        if (orderDto.getSkuId() == null) {
            return Result.failure("skuId不能为空");
        }
        if (!StringUtils.hasText(orderDto.getMessageId())) {
            orderDto.setMessageId(UUID.randomUUID().toString());
        }
        if (orderDto.getQuantity() == null || orderDto.getQuantity() <= 0) {
            orderDto.setQuantity(1);
        }

        // 2) 读操作前置：先查 SKU / Product 并做后端价格重算，严禁信任前端价格
        boolean stockDeducted = false;
        String stockKey = SKU_STOCK_KEY_PREFIX + orderDto.getSkuId();
        try {
            Result<Sku> skuResult = productClient.getSku(orderDto.getSkuId());
            if (skuResult == null || skuResult.getCode() != 200 || skuResult.getData() == null) {
                return Result.failure(skuResult == null ? "SKU不存在" : skuResult.getMessage());
            }
            Sku sku = skuResult.getData();

            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Result.failure("请先登录");
            }
            orderDto.setUserId(userId);
            orderDto.setSkuPrice(sku.getPrice());
            if (sku.getProductId() != null) {
                orderDto.setProductId(sku.getProductId().intValue());
            }
            if (orderDto.getProductId() == null) {
                throw new IllegalStateException("productId不能为空");
            }
            // 获取商品信息（包含店铺ID、商品名称等冗余字段）
            Result<ProductClient.ProductDetail> productResult = productClient.getProductById(orderDto.getProductId());
            if (productResult == null || productResult.getCode() != 200 || productResult.getData() == null || productResult.getData().getShopId() == null) {
                throw new IllegalStateException(productResult == null ? "获取店铺信息失败" : productResult.getMessage());
            }
            orderDto.setShopId(productResult.getData().getShopId());
            orderDto.setProductName(productResult.getData().getName());

            // 后端重算最终价格：单价 * 数量 - 优惠，防止前端篡改
            BigDecimal couponPrice = orderDto.getCouponPrice() == null ? BigDecimal.ZERO : orderDto.getCouponPrice();
            if (couponPrice.compareTo(BigDecimal.ZERO) < 0) {
                couponPrice = BigDecimal.ZERO;
            }
            BigDecimal finalPrice = sku.getPrice()
                    .multiply(BigDecimal.valueOf(orderDto.getQuantity()))
                    .subtract(couponPrice);
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }
            orderDto.setFinalPrice(finalPrice);

            // 3) 使用 Redis Lua 脚本原子预扣库存：避免 Redisson 锁导致的串行瓶颈
            Long deductResult = stringRedisTemplate.execute(
                    DEDUCT_STOCK_SCRIPT,
                    Collections.singletonList(stockKey),
                    String.valueOf(orderDto.getQuantity())
            );
            if (deductResult == null) {
                return Result.failure("扣减库存失败");
            }
            if (deductResult.longValue() != 1L) {
                return Result.failure("库存不足");
            }
            stockDeducted = true;

            // 4) 库存预扣成功后立即发 MQ 异步落库；发送异常时必须回滚 Redis 库存，防止库存泄漏
            // 用于 MQ 确认失败/路由失败的库存回补（ConfirmCallback/ReturnsCallback 触发）
            stringRedisTemplate.opsForValue().set(
                    MQ_STOCK_ROLLBACK_KEY_PREFIX + orderDto.getMessageId(),
                    orderDto.getSkuId() + "|" + orderDto.getQuantity(),
                    5,
                    TimeUnit.MINUTES
            );

            CorrelationData correlationData = new CorrelationData(orderDto.getMessageId());
            rabbitTemplate.convertAndSend(MqConstant.SAVE_ORDER_EXCHANGE, "", orderDto, correlationData);
        } catch (Exception e) {
            log.error("saveOrder failed", e);
            if (stockDeducted) {
                try {
                    // MQ 发送失败或后续异常时，回补 Redis 预扣库存
                    stringRedisTemplate.opsForValue().increment(stockKey, orderDto.getQuantity());
                } catch (Exception ex) {
                    log.error("Redis库存回补失败, skuId={}, quantity={}", orderDto.getSkuId(), orderDto.getQuantity(), ex);
                }
                try {
                    if (StringUtils.hasText(orderDto.getMessageId())) {
                        stringRedisTemplate.delete(MQ_STOCK_ROLLBACK_KEY_PREFIX + orderDto.getMessageId());
                    }
                } catch (Exception ignored) {
                }
            }
            return Result.failure("下单失败");
        }

        return Result.success(null);
    }

    @Override
    public Result<Void> buyOrder(Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        if (orderId == null) {
            return Result.failure("订单ID不能为空");
        }

        Order dbOrder = orderMapper.selectById(orderId);
        if (dbOrder == null) {
            return Result.failure("订单不存在");
        }
        if (dbOrder.getUserId() == null || !dbOrder.getUserId().equals(userId)) {
            return Result.failure("无权限操作该订单");
        }
        if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.DUE.getCode()) {
            return Result.failure("订单状态不可支付");
        }

        if (dbOrder.getCreateTime() == null) {
            return Result.failure("订单创建时间缺失");
        }
        try {
            LocalDateTime createTime = dbOrder.getCreateTime();
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(resolvedPaymentTimeoutSeconds());
            if (!createTime.isAfter(cutoff)) {
                UpdateWrapper<Order> cancelWrapper = new UpdateWrapper<>();
                cancelWrapper.eq("id", orderId)
                        .eq("status", OrderStatusEnum.DUE.getCode())
                        .set("status", OrderStatusEnum.CANCEL.getCode());
                int cancelled = orderMapper.update(null, cancelWrapper);
                if (cancelled > 0) {
                    restoreSkuStock(dbOrder);
                }
                log.info("buyOrder timeout cancel orderId={}, userId={}, skuId={}, quantity={}, createTime={}, cutoff={}", orderId, userId, dbOrder.getSkuId(), dbOrder.getQuantity(), dbOrder.getCreateTime(), cutoff);
                return Result.failure("订单已超时自动取消");
            }
        } catch (Exception e) {
            log.error("buyOrder process failed, orderId={}, createTime={}", orderId, dbOrder.getCreateTime(), e);
            return Result.failure("订单处理失败");
        }

        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .eq("status", OrderStatusEnum.DUE.getCode())
                .set("status", OrderStatusEnum.PAID.getCode());
        int updated = orderMapper.update(null, updateWrapper);
        if (updated <= 0) {
            return Result.failure("支付失败，请刷新后重试");
        }
        try {
            Integer quantity = dbOrder.getQuantity();
            if (quantity == null || quantity <= 0) {
                quantity = 1;
            }
            Map<String, Object> salesMessage = new HashMap<>();
            salesMessage.put("productId", dbOrder.getProductId());
            salesMessage.put("skuId", dbOrder.getSkuId());
            salesMessage.put("quantity", quantity);
            rabbitTemplate.convertAndSend("sales.exchange", "sales.update", salesMessage);
            log.info("Sent sales update message, orderId={}, productId={}, skuId={}, quantity={}",
                    orderId, dbOrder.getProductId(), dbOrder.getSkuId(), quantity);
        } catch (Exception e) {
            log.error("Failed to send sales update message, orderId={}, productId={}, skuId={}, quantity={}",
                    orderId, dbOrder.getProductId(), dbOrder.getSkuId(), dbOrder.getQuantity(), e);
        }

        return Result.success(null);
    }

    @Override
    public Result<List<OrderVo>> searchOrders(OrderSearchDto searchDto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        if (searchDto == null) {
            searchDto = new OrderSearchDto();
        }

        Integer merchantShopId = resolveMerchantShopId();
        if (merchantShopId != null) {
            return searchOrdersAsMerchant(searchDto, merchantShopId);
        }

        searchDto.setUserId(userId);
        Page<Order> page = new Page<>(searchDto.getPageNum(), searchDto.getPageSize());
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();

        if (searchDto.getStatus() != null) {
            queryWrapper.eq("status", searchDto.getStatus());
        }
        if (searchDto.getOrderId() != null) {
            queryWrapper.eq("id", searchDto.getOrderId());
        }
        if (StringUtils.hasText(searchDto.getKeyword())) {
            String keyword = searchDto.getKeyword();
            queryWrapper.and(wrapper -> {
                wrapper.like("product_name", keyword);
                if (keyword.matches("^\\d+$")) {
                     wrapper.or().eq("id", Long.valueOf(keyword));
                }
            });
        }
        queryWrapper.eq("user_id", searchDto.getUserId());
        if (searchDto.getStartTime() != null) {
            queryWrapper.ge("create_time", searchDto.getStartTime());
        }
        if (searchDto.getEndTime() != null) {
            queryWrapper.le("create_time", searchDto.getEndTime());
        }

        queryWrapper.orderByDesc("id");

        Page<Order> orderPage = orderMapper.selectPage(page, queryWrapper);
        List<Order> records = orderPage.getRecords();

        Map<Integer, com.itcast.model.pojo.Product> productCache = new HashMap<>();
        Map<Integer, ProductClient.Shop> shopCache = new HashMap<>();
        Map<Long, User> userCache = new HashMap<>();
        List<OrderVo> orderVos = records.stream()
                .map(order -> buildOrderVo(order, productCache, shopCache, userCache))
                .collect(Collectors.toList());

        return Result.success(orderVos, orderPage.getTotal());
    }

    @Override
    public Result<OrderVo> getOrderDetail(Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        if (orderId == null) {
            return Result.failure("订单ID不能为空");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.failure("订单不存在");
        }

        Integer merchantShopId = resolveMerchantShopId();
        if (merchantShopId != null) {
            if (!isOrderInMerchantShop(order, merchantShopId, new HashMap<>())) {
                return Result.failure("无权限查看该订单");
            }
            if (order.getStatus() != null
                    && order.getStatus() != OrderStatusEnum.PAID.getCode()
                    && order.getStatus() != OrderStatusEnum.SHIPPED.getCode()
                    && order.getStatus() != OrderStatusEnum.COMPLETED.getCode()) {
                return Result.failure("无权限查看该订单");
            }
        } else {
            if (order.getUserId() == null || !order.getUserId().equals(userId)) {
                return Result.failure("无权限查看该订单");
            }
        }

        OrderVo vo = buildOrderVo(order, new HashMap<>(), new HashMap<>(), new HashMap<>());
        return Result.success(vo);
    }

    @Override
    public Result<OrderStatisticsVo> getStatistics() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }

        OrderStatisticsVo vo = new OrderStatisticsVo();

        Integer merchantShopId = resolveMerchantShopId();
        if (merchantShopId != null) {
            List<Integer> statuses = Arrays.asList(
                    OrderStatusEnum.PAID.getCode(),
                    OrderStatusEnum.SHIPPED.getCode(),
                    OrderStatusEnum.COMPLETED.getCode(),
                    OrderStatusEnum.CANCEL.getCode()
            );
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("shop_id", merchantShopId);
            queryWrapper.in("status", statuses);
            List<Order> candidates = orderMapper.selectList(queryWrapper);

            long paid = 0;
            long shipped = 0;
            long completed = 0;
            long cancelled = 0;

            for (Order order : candidates) {
                if (order.getStatus() == null) {
                    continue;
                }
                if (order.getStatus() == OrderStatusEnum.PAID.getCode()) {
                    paid++;
                } else if (order.getStatus() == OrderStatusEnum.SHIPPED.getCode()) {
                    shipped++;
                } else if (order.getStatus() == OrderStatusEnum.COMPLETED.getCode()) {
                    completed++;
                } else if (order.getStatus() == OrderStatusEnum.CANCEL.getCode()) {
                    cancelled++;
                }
            }

            vo.setPendingPayment(0L);
            vo.setPaid(paid);
            vo.setShipped(shipped);
            vo.setCompleted(completed);
            vo.setCancelled(cancelled);
            vo.setTotalOrders(paid + shipped + completed + cancelled);
            vo.setTotalAmount(BigDecimal.ZERO);
            return Result.success(vo);
        }

        vo.setTotalOrders(orderMapper.selectCount(new QueryWrapper<Order>().eq("user_id", userId)));
        vo.setPendingPayment(countByStatusForUser(userId, OrderStatusEnum.DUE.getCode()));
        vo.setPaid(countByStatusForUser(userId, OrderStatusEnum.PAID.getCode()));
        vo.setShipped(countByStatusForUser(userId, OrderStatusEnum.SHIPPED.getCode()));
        vo.setCompleted(countByStatusForUser(userId, OrderStatusEnum.COMPLETED.getCode()));
        vo.setCancelled(countByStatusForUser(userId, OrderStatusEnum.CANCEL.getCode()));
        vo.setTotalAmount(BigDecimal.ZERO);
        return Result.success(vo);
    }
    
    private Long countByStatusForUser(Long userId, Integer status) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("status", status);
        return orderMapper.selectCount(queryWrapper);
    }

    @Override
    public Result<Void> updateOrderStatus(Long orderId, Integer status) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        if (orderId == null) {
            return Result.failure("订单ID不能为空");
        }
        if (status == null) {
            return Result.failure("状态不能为空");
        }

        Order dbOrder = orderMapper.selectById(orderId);
        if (dbOrder == null) {
            return Result.failure("订单不存在");
        }

        if (status == OrderStatusEnum.CANCEL.getCode()) {
            if (dbOrder.getUserId() == null || !dbOrder.getUserId().equals(userId)) {
                return Result.failure("无权限操作该订单");
            }
            if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.DUE.getCode()) {
                return Result.failure("订单状态不可取消");
            }
            UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderId)
                    .eq("status", OrderStatusEnum.DUE.getCode())
                    .set("status", OrderStatusEnum.CANCEL.getCode());
            int updated = orderMapper.update(null, updateWrapper);
            if (updated <= 0) {
                return Result.failure("取消失败，请刷新后重试");
            }
            restoreSkuStock(dbOrder);
            return Result.success(null);
        }

        if (status == OrderStatusEnum.SHIPPED.getCode()) {
            Integer merchantShopId = resolveMerchantShopId();
            if (merchantShopId == null) {
                return Result.failure("无权限操作该订单");
            }
            if (!isOrderInMerchantShop(dbOrder, merchantShopId, new HashMap<>())) {
                return Result.failure("无权限操作该订单");
            }

            if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.PAID.getCode()) {
                return Result.failure("订单状态不可发货");
            }

            UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderId)
                    .eq("status", OrderStatusEnum.PAID.getCode())
                    .set("status", status);
            int updated = orderMapper.update(null, updateWrapper);
            if (updated <= 0) {
                return Result.failure("发货失败，请刷新后重试");
            }
            return Result.success(null);
        }

        if (status == OrderStatusEnum.COMPLETED.getCode()) {
            // 用户确认收货
            if (dbOrder.getUserId() != null && dbOrder.getUserId().equals(userId)) {
                if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.SHIPPED.getCode()) {
                    return Result.failure("订单状态不可确认收货");
                }
                UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", orderId)
                        .eq("status", OrderStatusEnum.SHIPPED.getCode())
                        .set("status", status);
                int updated = orderMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    return Result.failure("确认收货失败，请刷新后重试");
                }
                return Result.success(null);
            }
            
            // 商家也可以强制完成？暂不考虑，或者保持原有逻辑
            Integer merchantShopId = resolveMerchantShopId();
            if (merchantShopId != null && isOrderInMerchantShop(dbOrder, merchantShopId, new HashMap<>())) {
                 if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.SHIPPED.getCode()) {
                    return Result.failure("订单状态不可完成");
                }
                UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", orderId)
                        .eq("status", OrderStatusEnum.SHIPPED.getCode())
                        .set("status", status);
                int updated = orderMapper.update(null, updateWrapper);
                if (updated <= 0) {
                    return Result.failure("操作失败，请刷新后重试");
                }
                return Result.success(null);
            }
             
            return Result.failure("无权限操作该订单");
        }

        return Result.failure("不支持的状态变更");
    }

    @Override
    public Result<Long> getServerTime() {
        return Result.success(System.currentTimeMillis());
    }

    @Override
    public Result<Integer> getPaymentTimeoutSeconds() {
        return Result.success(resolvedPaymentTimeoutSeconds());
    }

    @Override
    public Result<Void> timeoutCancelOrder(Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.failure("请先登录");
        }
        if (orderId == null) {
            return Result.failure("订单ID不能为空");
        }

        Order dbOrder = orderMapper.selectById(orderId);
        if (dbOrder == null) {
            return Result.failure("订单不存在");
        }
        if (dbOrder.getUserId() == null || !dbOrder.getUserId().equals(userId)) {
            return Result.failure("无权限操作该订单");
        }
        if (dbOrder.getStatus() == null || dbOrder.getStatus() != OrderStatusEnum.DUE.getCode()) {
            return Result.failure("订单状态不可取消");
        }
        if (dbOrder.getCreateTime() == null) {
            return Result.failure("订单创建时间缺失");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(resolvedPaymentTimeoutSeconds());
        if (dbOrder.getCreateTime().isAfter(cutoff)) {
            return Result.failure("订单未超时");
        }

        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .eq("status", OrderStatusEnum.DUE.getCode())
                .set("status", OrderStatusEnum.CANCEL.getCode());
        int updated = orderMapper.update(null, updateWrapper);
        if (updated <= 0) {
            return Result.failure("取消失败，请刷新后重试");
        }
        restoreSkuStock(dbOrder);
        log.info("timeoutCancelOrder orderId={}, userId={}, skuId={}, quantity={}, createTime={}", orderId, userId, dbOrder.getSkuId(), dbOrder.getQuantity(), dbOrder.getCreateTime());
        return Result.success(null);
    }

    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutOrders() {
        RLock lock = redissonClient.getLock("order:job:timeoutCancel");
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 55, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(resolvedPaymentTimeoutSeconds());

            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", OrderStatusEnum.DUE.getCode())
                    .le("create_time", cutoff);
            List<Order> overdueOrders = orderMapper.selectList(queryWrapper);
            if (overdueOrders == null || overdueOrders.isEmpty()) {
                return;
            }

            for (Order overdue : overdueOrders) {
                if (overdue == null || overdue.getId() == null) {
                    continue;
                }
                UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", overdue.getId())
                        .eq("status", OrderStatusEnum.DUE.getCode())
                        .set("status", OrderStatusEnum.CANCEL.getCode());
                int updated = orderMapper.update(null, updateWrapper);
                if (updated > 0) {
                    restoreSkuStock(overdue);
                    log.info("cancelTimeoutOrders orderId={}, userId={}, skuId={}, quantity={}, createTime={}, cutoff={}", overdue.getId(), overdue.getUserId(), overdue.getSkuId(), overdue.getQuantity(), overdue.getCreateTime(), cutoff);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void restoreSkuStock(Order order) {
        if (order == null || order.getSkuId() == null) {
            return;
        }
        Integer quantity = order.getQuantity();
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }

        // 1. 订单取消/超时释放预扣库存：回补 Redis 库存，避免库存泄漏
        try {
            String stockKey = SKU_STOCK_KEY_PREFIX + order.getSkuId();
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        } catch (Exception e) {
            log.error("取消订单-Redis库存回补失败, orderId={}, skuId={}", order.getId(), order.getSkuId(), e);
        }

        // 2. 同步回补 MySQL 数据库库存
        try {
            ProductClient.IncreaseSkuStockRequest stockRequest = new ProductClient.IncreaseSkuStockRequest();
            stockRequest.skuId = order.getSkuId();
            stockRequest.quantity = quantity;
            Result<Void> stockResult = productClient.increaseSkuStock(stockRequest);
            if (stockResult == null || stockResult.getCode() != 200) {
                String errorMsg = (stockResult == null) ? "远程调用返回空" : stockResult.getMessage();
                log.error("取消订单-MySQL库存回补失败: orderId={}, skuId={}, quantity={}, error={}", 
                        order.getId(), order.getSkuId(), quantity, errorMsg);
            }
        } catch (Exception e) {
            log.error("取消订单-MySQL库存回补异常: orderId={}, skuId={}", order.getId(), order.getSkuId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrderMessage(OrderDto orderDto) {
        if (orderDto == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        String messageKey = null;
        if (StringUtils.hasText(orderDto.getMessageId())) {
            messageKey = "order:msg:" + orderDto.getMessageId();
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(messageKey, "processing", 2, TimeUnit.MINUTES);
            if (ok == null || !ok) {
                throw new IllegalStateException("消息已处理");
            }
        }

        try {
            Order order = new Order();
            BeanUtils.copyProperties(orderDto, order);
        
            if (order.getSkuId() == null) {
                throw new IllegalArgumentException("skuId不能为空");
            }
            if (order.getQuantity() == null || order.getQuantity() <= 0) {
                order.setQuantity(1);
            }
            if (order.getFinalPrice() == null) {
                // 这属于系统级 Bug（生产者没算价格），必须抛出异常人工介入
                log.error("严重错误：消费到的订单消息缺失最终价格！MessageId: {}", orderDto.getMessageId());
                throw new IllegalArgumentException("订单数据异常：缺少成交价");
            }
            if (order.getShopId() == null && order.getProductId() != null) {
                Result<ProductClient.ProductDetail> productResult = productClient.getProductById(order.getProductId());
                if (productResult != null && productResult.getCode() == 200 && productResult.getData() != null) {
                    order.setShopId(productResult.getData().getShopId());
                    order.setProductName(productResult.getData().getName());
                }
            }

            order.setStatus(OrderStatusEnum.DUE.getCode());
            order.setCreateTime(LocalDateTime.now());
        
            orderMapper.insert(order);

            // 同步扣减MySQL库存
            ProductClient.DecreaseSkuStockRequest stockRequest = new ProductClient.DecreaseSkuStockRequest();
            stockRequest.skuId = order.getSkuId();
            stockRequest.quantity = order.getQuantity();
            Result<Void> stockResult = productClient.decreaseSkuStock(stockRequest);
            if (stockResult == null || stockResult.getCode() != 200) {
                // 如果扣减失败，抛出异常以触发Spring事务回滚和RabbitMQ重试
                String errorMsg = (stockResult == null) ? "远程调用返回空" : stockResult.getMessage();
                log.error("MySQL库存扣减失败: skuId={}, quantity={}, error={}", order.getSkuId(), order.getQuantity(), errorMsg);
                throw new RuntimeException("MySQL库存扣减失败: " + errorMsg);
            }

            if (orderDto.getSelectAttributes() != null && !orderDto.getSelectAttributes().isEmpty()) {
                for (CustomAttribute attr : orderDto.getSelectAttributes()) {
                    if (attr == null || !StringUtils.hasText(attr.getLabel())) {
                        continue;
                    }
                    if (attr.getValue() == null || attr.getValue().isEmpty()) {
                        OrderAttribute oa = new OrderAttribute();
                        oa.setOrderId(order.getId());
                        oa.setLabel(attr.getLabel());
                        oa.setValue(null);
                        orderAttributeMapper.insert(oa);
                        continue;
                    }
                    for (String v : attr.getValue()) {
                        if (!StringUtils.hasText(v)) {
                            continue;
                        }
                        OrderAttribute oa = new OrderAttribute();
                        oa.setOrderId(order.getId());
                        oa.setLabel(attr.getLabel());
                        oa.setValue(v);
                        orderAttributeMapper.insert(oa);
                    }
                }
            }

            if (messageKey != null) {
                redisTemplate.opsForValue().set(messageKey, "done", 1, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            if (messageKey != null) {
                redisTemplate.delete(messageKey);
            }
            throw e;
        }
    }

    private Integer resolveMerchantShopId() {
        try {
            Result<ProductClient.Shop> shopResult = productClient.getMyShop();
            if (shopResult != null && shopResult.getCode() == 200 && shopResult.getData() != null && shopResult.getData().id != null) {
                return shopResult.getData().id;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Result<List<OrderVo>> searchOrdersAsMerchant(OrderSearchDto searchDto, Integer merchantShopId) {
        int pageNum = searchDto.getPageNum() == null || searchDto.getPageNum() <= 0 ? 1 : searchDto.getPageNum();
        int pageSize = searchDto.getPageSize() == null || searchDto.getPageSize() <= 0 ? 10 : searchDto.getPageSize();

        List<Integer> allowedStatuses = Arrays.asList(
                OrderStatusEnum.PAID.getCode(),
                OrderStatusEnum.SHIPPED.getCode(),
                OrderStatusEnum.COMPLETED.getCode()
        );

        Page<Order> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("shop_id", merchantShopId);
        if (searchDto.getStatus() != null) {
            if (!allowedStatuses.contains(searchDto.getStatus())) {
                return Result.success(new ArrayList<>(), 0L);
            }
            queryWrapper.eq("status", searchDto.getStatus());
        } else {
            queryWrapper.in("status", allowedStatuses);
        }
        if (searchDto.getOrderId() != null) {
            queryWrapper.eq("id", searchDto.getOrderId());
        }
        if (searchDto.getStartTime() != null) {
            queryWrapper.ge("create_time", searchDto.getStartTime());
        }
        if (searchDto.getEndTime() != null) {
            queryWrapper.le("create_time", searchDto.getEndTime());
        }
        queryWrapper.orderByDesc("id");

        Page<Order> orderPage = orderMapper.selectPage(page, queryWrapper);
        List<Order> records = orderPage.getRecords();
        if (records == null || records.isEmpty()) {
            return Result.success(new ArrayList<>(), 0L);
        }

        Map<Integer, Product> productCache = new HashMap<>();
        Map<Integer, ProductClient.Shop> shopCache = new HashMap<>();
        Map<Long, User> userCache = new HashMap<>();
        List<OrderVo> orderVos = records.stream()
                .map(order -> buildOrderVo(order, productCache, shopCache, userCache))
                .collect(Collectors.toList());

        return Result.success(orderVos, orderPage.getTotal());
    }

    private boolean isOrderInMerchantShop(Order order, Integer merchantShopId, Map<Integer, Integer> productShopCache) {
        if (order == null || merchantShopId == null) {
            return false;
        }
        if (order.getShopId() != null) {
            return order.getShopId().equals(merchantShopId);
        }
        if (order.getProductId() == null) {
            return false;
        }
        Integer productId = order.getProductId();
        Integer shopId = productShopCache.get(productId);
        if (shopId == null) {
            try {
                    Result<ProductClient.ProductDetail> productResult = productClient.getProductById(productId);
                    if (productResult != null && productResult.getCode() == 200 && productResult.getData() != null) {
                        shopId = productResult.getData().getShopId();
                        if (shopId != null) {
                            productShopCache.put(productId, shopId);
                        }
                    }
            } catch (Exception ignored) {
            }
        }
        return shopId != null && shopId.equals(merchantShopId);
    }

    private OrderVo buildOrderVo(Order order, Map<Integer, Product> productCache, Map<Integer, ProductClient.Shop> shopCache, Map<Long, User> userCache) {
        OrderVo vo = new OrderVo();
        if (order == null) {
            return vo;
        }
        BeanUtils.copyProperties(order, vo);

        Product product = null;
        if (order.getProductId() != null) {
            Integer productId = order.getProductId();
            product = productCache.get(productId);
            if (product == null) {
                try {
                    Result<ProductClient.ProductDetail> productResult = productClient.getProductById(productId);
                    if (productResult != null && productResult.getCode() == 200 && productResult.getData() != null) {
                        product = productResult.getData();
                        productCache.put(productId, product);
                    }
                } catch (Exception ignored) {
                }
            }
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(StringUtils.hasText(product.getMainImage()) ? product.getMainImage() : product.getImage());
            }
        }

        if (order.getSkuId() != null) {
            try {
                Result<Sku> skuResult = productClient.getSku(order.getSkuId());
                if (skuResult != null && skuResult.getCode() == 200 && skuResult.getData() != null) {
                    Sku sku = skuResult.getData();
                    if (!StringUtils.hasText(vo.getProductName())) {
                        vo.setProductName(sku.getName());
                    }
                    if (!StringUtils.hasText(vo.getProductImage())) {
                        vo.setProductImage(sku.getImage());
                    }

                    // 解析规格
                    if (StringUtils.hasText(sku.getSpecs())) {
                        try {
                            Map<String, String> specs = new com.fasterxml.jackson.databind.ObjectMapper().readValue(sku.getSpecs(), Map.class);
                            if (specs != null && !specs.isEmpty()) {
                                String specStr = specs.entrySet()
                                        .stream()
                                        .map(e -> e.getKey() + ":" + e.getValue())
                                        .collect(Collectors.joining(" / "));
                                vo.setSkuSpec(specStr);
                            }
                        } catch (Exception e) {
                            // 解析失败直接显示原始字符串
                             vo.setSkuSpec(sku.getSpecs());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        ProductClient.Shop shop = null;
        if (product instanceof ProductClient.ProductDetail) {
            ProductClient.ProductDetail detail = (ProductClient.ProductDetail) product;
            shop = detail.shop;
        }
        Integer shopId = order.getShopId();
        if (shopId == null && product != null) {
            shopId = product.getShopId();
        }
        if (shop == null && shopId != null) {
            shop = shopCache.get(shopId);
            if (shop == null) {
                try {
                    Result<ProductClient.Shop> shopResult = productClient.getShopById(shopId);
                    if (shopResult != null && shopResult.getCode() == 200 && shopResult.getData() != null) {
                        shop = shopResult.getData();
                        shopCache.put(shopId, shop);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (shop != null) {
            vo.setStoreName(shop.name);
            vo.setStoreAvatar(shop.image);
        }

        // 填充买家信息（真实数据）
        if (order.getUserId() != null) {
            Long buyerId = order.getUserId();
            User buyer = userCache.get(buyerId);
            if (buyer == null) {
                try {
                    Result<User> buyerResult = userClient.getUserById(buyerId);
                    if (buyerResult != null && buyerResult.getCode() == 200 && buyerResult.getData() != null) {
                        buyer = buyerResult.getData();
                        userCache.put(buyerId, buyer);
                    }
                } catch (Exception ignored) {
                }
            }

            if (buyer != null && StringUtils.hasText(buyer.getNickname())) {
                vo.setBuyerName(buyer.getNickname());
            } else {
                vo.setBuyerName("用户" + buyerId);
            }

            if (buyer != null && StringUtils.hasText(buyer.getImage())) {
                vo.setBuyerAvatar(buyer.getImage());
            }

            if (buyer != null && StringUtils.hasText(buyer.getPhone())) {
                vo.setBuyerPhone(buyer.getPhone());
            }
        }

        // 填充商家备注
        try {
             QueryWrapper<OrderAttribute> attrQuery = new QueryWrapper<>();
             attrQuery.eq("order_id", order.getId());
             attrQuery.eq("label", "merchant_memo");
             OrderAttribute attr = orderAttributeMapper.selectOne(attrQuery);
             if (attr != null) {
                 vo.setMerchantMemo(attr.getValue());
             }
        } catch (Exception ignored) {}

        return vo;
    }
}
