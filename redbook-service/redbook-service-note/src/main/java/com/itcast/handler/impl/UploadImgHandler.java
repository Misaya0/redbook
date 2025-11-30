package com.itcast.handler.impl;

import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import com.itcast.util.LocalFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@Order(2)
@Slf4j
public class UploadImgHandler extends NoteHandler {

    @Autowired
    private LocalFileUtil localFileUtil;

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        MultipartFile file = noteDto.getFile();
        String path = localFileUtil.uploadImg(file.getBytes());
        noteDto.setImage(path);
        log.info("图片上传成功，路径: {}", path);
    }

    @Override
    public void compensate(NoteDto noteDto) {
        // 补偿：删除已上传的图片
        if (noteDto.getImage() != null) {
            try {
                localFileUtil.deleteImg(noteDto.getImage());
                log.info("补偿操作：已删除上传的图片 {}", noteDto.getImage());
            } catch (Exception e) {
                log.error("补偿操作失败：删除图片 {} 时出错", noteDto.getImage(), e);
            }
        }
    }
}
