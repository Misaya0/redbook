package com.itcast.handler.impl;

import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@Order(5)
@Slf4j
public class GetNoteTypeHandler extends NoteHandler {

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        log.info("开始执行笔记类型默认值设置...GetNoteTypeHandler");
        if (noteDto == null) {
            return;
        }
        if (!StringUtils.hasText(noteDto.getType())) {
            noteDto.setType("其他");
        }
    }
}
