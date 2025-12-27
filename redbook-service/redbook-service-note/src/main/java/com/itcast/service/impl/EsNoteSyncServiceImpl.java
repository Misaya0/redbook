package com.itcast.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.constant.MqConstant;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.dto.NoteDto;
import com.itcast.model.dto.NoteEsSyncMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import com.itcast.mapper.NoteSyncMapper;
import com.itcast.model.pojo.Note;
import com.itcast.service.EsNoteSyncService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class EsNoteSyncServiceImpl implements EsNoteSyncService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private NoteSyncMapper noteSyncMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void syncAllNotesToEs() throws IOException {
        // 1. 查询三张表的所有笔记
        List<Note> notes = noteSyncMapper.selectAllNotesFromAllTables();
        if (notes == null || notes.isEmpty()) {
            log.info("syncAllNotesToEs: 数据库中无笔记数据");
            return;
        }
        log.info("syncAllNotesToEs: 准备同步笔记数量={}", notes.size());

        // 2. 确保索引存在
        ensureIndexExists();

        // 3. 写入 ES
        syncNotesToEs(notes);
    }

    @Override
    public void syncNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note != null) {
            NoteEsSyncMessage message = new NoteEsSyncMessage("UPDATE", note.getId(), note);
            rabbitTemplate.convertAndSend(MqConstant.NOTE_ES_EXCHANGE, MqConstant.NOTE_ES_SYNC_KEY, message);
            log.info("Manual sync triggered for note: {}", noteId);
        } else {
            log.warn("Manual sync failed: Note not found for id {}", noteId);
        }
    }

    @Override
    public void syncNotesToEs(List<Note> notes) throws IOException {
        if (notes == null || notes.isEmpty()) {
            log.info("syncNotesToEs: 传入笔记为空");
            return;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (Note note : notes) {
            IndexRequest indexRequest = new IndexRequest("rb_note")
                    .id(String.valueOf(note.getId()))
                    .source(objectMapper.writeValueAsString(note), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("syncNotesToEs: 同步完成，总数={}, hasFailures={}", notes.size(), bulkResponse.hasFailures());
    }

    private void ensureIndexExists() throws IOException {
        IndicesClient indices = client.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest("rb_note");
        boolean exists = indices.exists(getIndexRequest, RequestOptions.DEFAULT);
        if (!exists) {
            CreateIndexRequest request = new CreateIndexRequest("rb_note");

            String mapping = "{\n" +
                    "  \"properties\": {\n" +
                    "    \"id\":        { \"type\": \"long\" },\n" +
                    "    \"title\":     { \"type\": \"text\",  \"analyzer\": \"ik_max_word\", \"search_analyzer\": \"ik_smart\", \"fields\": {\"keyword\": {\"type\": \"keyword\", \"ignore_above\": 256}} },\n" +
                    "    \"content\":   { \"type\": \"text\",  \"analyzer\": \"ik_max_word\", \"search_analyzer\": \"ik_smart\" },\n" +
                    "    \"image\":     { \"type\": \"keyword\" },\n" +
                    "    \"time\":      { \"type\": \"date\", \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\" },\n" +
                    "    \"type\":      { \"type\": \"keyword\" },\n" +
                    "    \"address\":   { \"type\": \"keyword\" },\n" +
                    "    \"longitude\": { \"type\": \"double\" },\n" +
                    "    \"latitude\":  { \"type\": \"double\" },\n" +
                    "    \"like\":      { \"type\": \"integer\" },\n" +
                    "    \"collection\":{ \"type\": \"integer\" },\n" +
                    "    \"userId\":    { \"type\": \"long\" }\n" +
                    "  }\n" +
                    "}";

            request.mapping(mapping, XContentType.JSON);
            indices.create(request, RequestOptions.DEFAULT);

            log.info("索引 rb_note 不存在，已创建 mapping");
        }
    }
}
