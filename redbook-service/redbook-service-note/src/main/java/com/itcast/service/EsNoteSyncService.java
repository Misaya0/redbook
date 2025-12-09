package com.itcast.service;


import com.itcast.model.pojo.Note;

import java.io.IOException;
import java.util.List;

public interface EsNoteSyncService {

    /**
     * 从数据库三张表中加载所有笔记并写入 ES 索引 rb_note
     */
    void syncAllNotesToEs() throws IOException;

    /**
     * 可选：只同步指定的若干笔记
     */
    void syncNotesToEs(List<Note> notes) throws IOException;

    /**
     * 手动同步单个笔记
     */
    void syncNote(Long noteId);
}
