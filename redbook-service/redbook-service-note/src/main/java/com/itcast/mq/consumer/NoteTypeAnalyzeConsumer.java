package com.itcast.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.config.RabbitConfig;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Note;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NoteTypeAnalyzeConsumer {

    private static final List<String> TYPE_LIST = Arrays.asList("情感", "体育", "穿搭", "美食", "旅行", "搞笑", "音乐", "职场");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${glm.api_url}")
    private String apiUrl;

    @Value("${glm.api_key}")
    private String apiKey;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.NOTE_TYPE_QUEUE)
    public void onMessage(Map<String, Object> message) throws Exception {
        if (message == null || message.isEmpty()) {
            return;
        }

        Long noteId = toLong(message.get("noteId"));
        String title = toString(message.get("title"));
        String content = toString(message.get("content"));

        if (noteId == null || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            log.warn("笔记类型分析消息字段缺失，message={}", message);
            return;
        }

        String analyzedType = analyzeType(title, content);
        analyzedType = cleanTypeResult(analyzedType);
        if (!TYPE_LIST.contains(analyzedType)) {
            analyzedType = "其他";
        }

        Note update = new Note();
        update.setType(analyzedType);

        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .and(w -> w.isNull(Note::getType).or().eq(Note::getType, "其他"));

        int updated = noteMapper.update(update, wrapper);
        if (updated <= 0) {
            Note db = noteMapper.selectById(noteId);
            if (db == null) {
                throw new IllegalStateException("note not found, noteId=" + noteId);
            }
            return;
        }

        log.info("笔记类型分析完成 noteId={}, type={}", noteId, analyzedType);
    }

    private String analyzeType(String title, String content) throws Exception {
        String typeListString = String.join("、", TYPE_LIST);
        String prompt = String.format(
                "你是一个专业的文本分类助手。请根据以下标题和内容，在 %s 这些类别中选择最合适的一个，并且只返回该类别的名称，不要提供任何额外的解释或回答。" +
                        "标题：%s" +
                        "内容：%s" +
                        "请只返回类别名称。如果没有合适的类别，请返回“其他”",
                typeListString, title, content);

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", "glm-4.5v");
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", "You are a helpful assistant.");
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", prompt);
        messages.add(system);
        messages.add(user);
        requestPayload.put("messages", messages);
        requestPayload.put("temperature", 0.7);

        String requestBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("glm api error, status=" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "其他";
        }
        return contentNode.asText();
    }

    private String cleanTypeResult(String type) {
        if (!StringUtils.hasText(type)) {
            return "其他";
        }

        String result = type.trim();
        result = result.replaceAll("<\\|begin_of_box\\|>", "");
        result = result.replaceAll("<\\|end_of_box\\|>", "");
        result = result.replaceAll("<[^>]+>", "");
        result = result.replaceAll("\\|[^|]+\\|", "");
        result = result.replaceAll("[\"'`]", "");
        result = result.replaceAll("\\s+", "");
        return result;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            String s = String.valueOf(v).trim();
            if (!StringUtils.hasText(s)) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String toString(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return StringUtils.hasText(s) ? s : null;
    }
}

