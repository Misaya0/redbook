package com.itcast.mq.consumer;

import com.itcast.constant.MqConstant;
import com.itcast.model.dto.OrderDto;
import com.itcast.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = MqConstant.SAVE_ORDER_QUEUE)
    public void onMessage(OrderDto orderDto) {
        String messageId = orderDto == null ? null : orderDto.getMessageId();
        try {
            if (!StringUtils.hasText(messageId)) {
                messageId = "unknown";
            }
            log.info("第一个消息消费者监听，处理消息：{}, messageId: {}", orderDto, messageId);

            // 处理订单保存（通过Service调用，事务会生效，包含幂等性检查）
            orderService.processOrderMessage(orderDto);
            log.info("消息已确认，订单处理成功，messageId: {}", messageId);
        } catch (IllegalStateException e) {
            log.warn("消息已处理过或正在处理中，跳过处理，messageId: {}, 错误: {}", messageId, e.getMessage());
        } catch (Exception e) {
            log.error("处理订单消息时发生异常，messageId: {}", messageId, e);
            throw e;
        }
    }
}
