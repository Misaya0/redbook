package com.itcast.config;

import com.itcast.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MqConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private static final String SKU_STOCK_KEY_PREFIX = "product:stock:";
    private static final String MQ_STOCK_ROLLBACK_KEY_PREFIX = "order:mq:stockRollback:";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 声明交换机
     */
    @Bean
    public FanoutExchange saveOrderExchange() {
        return new FanoutExchange(MqConstant.SAVE_ORDER_EXCHANGE, true, false);
    }

    /**
     * 声明队列
     */
    @Bean
    public Queue saveOrderQueue() {
        return new Queue(MqConstant.SAVE_ORDER_QUEUE, true);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding saveOrderBinding(Queue saveOrderQueue, FanoutExchange saveOrderExchange) {
        return BindingBuilder.bind(saveOrderQueue).to(saveOrderExchange);
    }


    /**
     * 构造方法执行结束后立刻执行此方法。即初始化逻辑。
     */
    @PostConstruct
    public void init(){
        // 设置RabbitTemplate中的回调逻辑
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setConfirmCallback(this);
        this.rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * 消息路由失败回调逻辑
     * @param returned 路由失败的消息
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        String correlationId = null;
        if (returned != null && returned.getMessage() != null && returned.getMessage().getMessageProperties() != null) {
            Object header = returned.getMessage().getMessageProperties().getHeaders().get("spring_returned_message_correlation");
            if (header != null) {
                correlationId = String.valueOf(header);
            }
        }

        log.error("MQ路由失败 exchange={}, routingKey={}, replyCode={}, replyText={}, correlationId={}",
                returned == null ? null : returned.getExchange(),
                returned == null ? null : returned.getRoutingKey(),
                returned == null ? null : returned.getReplyCode(),
                returned == null ? null : returned.getReplyText(),
                correlationId
        );

        if (StringUtils.hasText(correlationId)) {
            rollbackRedisStockByCorrelationId(correlationId, "路由失败");
        }
    }

    /**
     * 交换器到达队列失败回调逻辑
     * @param correlationData 消息唯一标记
     * @param ack 是否确认
     * @param cause 不能到达交换器（即ack为false）的具体原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String correlationId = correlationData == null ? null : correlationData.getId();
        if (!StringUtils.hasText(correlationId)) {
            return;
        }

        if (ack) {
            try {
                stringRedisTemplate.delete(MQ_STOCK_ROLLBACK_KEY_PREFIX + correlationId);
            } catch (Exception e) {
                log.warn("删除MQ库存回补标记失败 correlationId={}", correlationId, e);
            }
            return;
        }

        log.error("MQ发送确认失败 correlationId={}, cause={}", correlationId, cause);
        rollbackRedisStockByCorrelationId(correlationId, cause);
    }

    private void rollbackRedisStockByCorrelationId(String correlationId, String reason) {
        String key = MQ_STOCK_ROLLBACK_KEY_PREFIX + correlationId;
        String payload = null;
        try {
            payload = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("读取MQ库存回补标记失败 correlationId={}, reason={}", correlationId, reason, e);
        }
        if (!StringUtils.hasText(payload)) {
            return;
        }

        Long skuId = null;
        Integer quantity = null;
        try {
            String[] parts = payload.split("\\|");
            if (parts.length >= 2) {
                skuId = Long.valueOf(parts[0]);
                quantity = Integer.valueOf(parts[1]);
            }
        } catch (Exception e) {
            log.error("解析MQ库存回补标记失败 correlationId={}, payload={}, reason={}", correlationId, payload, reason, e);
        }
        if (skuId == null || quantity == null || quantity <= 0) {
            return;
        }

        try {
            String stockKey = SKU_STOCK_KEY_PREFIX + skuId;
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
            stringRedisTemplate.opsForValue().set(key, "rolledback", 10, TimeUnit.MINUTES);
            log.info("MQ发送失败库存已回补 correlationId={}, skuId={}, quantity={}, reason={}", correlationId, skuId, quantity, reason);
        } catch (Exception e) {
            log.error("MQ发送失败库存回补失败 correlationId={}, skuId={}, quantity={}, reason={}", correlationId, skuId, quantity, reason, e);
        }
    }
}
