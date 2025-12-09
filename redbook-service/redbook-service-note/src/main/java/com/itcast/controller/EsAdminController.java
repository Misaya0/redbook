package com.itcast.controller;

import com.itcast.service.EsNoteSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/es")
public class EsAdminController {

    @Autowired
    private EsNoteSyncService esNoteSyncService;

    /**
     * 手动触发：全量同步 rb_note_0/1/2 三张表的数据到 ES
     */
    @GetMapping("/rebuildNotes")
    public String rebuildNotes() throws IOException {
        log.info("接收到重建笔记 ES 索引请求");
        esNoteSyncService.syncAllNotesToEs();
        return "ok";
    }

    @GetMapping("/syncNote")
    public String syncNote(Long noteId) {
        log.info("接收到同步单个笔记请求: {}", noteId);
        esNoteSyncService.syncNote(noteId);
        return "ok";
    }
}
