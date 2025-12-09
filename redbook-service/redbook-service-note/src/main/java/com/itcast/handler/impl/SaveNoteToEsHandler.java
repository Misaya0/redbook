package com.itcast.handler.impl;

import com.itcast.constant.MqConstant;
import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import com.itcast.model.dto.NoteEsSyncMessage;
import lombok.extern.slf4j.Slf4j;
import com.itcast.model.pojo.Note;
import org.springframework.beans.BeanUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(10)
@Slf4j
public class SaveNoteToEsHandler extends NoteHandler {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        // Convert NoteDto to Note (or just use Note properties) to avoid serialization issues with MultipartFile
        Note note = new Note();
        BeanUtils.copyProperties(noteDto, note);
        
        NoteEsSyncMessage message = new NoteEsSyncMessage("INSERT", note.getId(), note);
        rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
        log.info("Sent ES sync message (INSERT) for note: {}", note.getId());
    }

    @Override
    public void compensate(NoteDto noteDto) {
        // 补偿：发送删除消息
        if (noteDto.getId() != null) {
            try {
                NoteEsSyncMessage message = new NoteEsSyncMessage("DELETE", noteDto.getId(), null);
                rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
                log.info("补偿操作：Sent ES sync message (DELETE) for note: {}", noteDto.getId());
            } catch (Exception e) {
                log.error("补偿操作失败：发送 ES 删除消息失败 noteId: {} ", noteDto.getId(), e);
            }
        }
    }
}
