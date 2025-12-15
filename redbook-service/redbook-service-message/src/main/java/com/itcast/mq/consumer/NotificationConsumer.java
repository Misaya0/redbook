package com.itcast.mq.consumer;

import com.itcast.model.event.MessageEvent;
import com.itcast.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = "message.notification.queue")
    public void onMessage(MessageEvent event) {
        log.info("Received notification event: {}", event);
        try {
            notificationService.processEvent(event);
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
        }
    }
}
