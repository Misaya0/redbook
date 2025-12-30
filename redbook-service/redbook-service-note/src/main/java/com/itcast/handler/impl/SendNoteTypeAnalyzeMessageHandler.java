package com.itcast.handler.impl;

import com.itcast.config.RabbitConfig;
import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(11)
@Slf4j
public class SendNoteTypeAnalyzeMessageHandler extends NoteHandler {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        if (noteDto == null || noteDto.getId() == null) {
            return;
        }
        if (!StringUtils.hasText(noteDto.getTitle()) || !StringUtils.hasText(noteDto.getContent())) {
            return;
        }
        if (StringUtils.hasText(noteDto.getType()) && !"其他".equals(noteDto.getType())) {
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("noteId", noteDto.getId());
        message.put("title", noteDto.getTitle());
        message.put("content", noteDto.getContent());

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.NOTE_TYPE_EXCHANGE, RabbitConfig.NOTE_TYPE_ROUTING_KEY, message);
        } catch (Exception e) {
            log.error("发送笔记类型分析消息失败 noteId={}", noteDto.getId(), e);
        }
    }
}

