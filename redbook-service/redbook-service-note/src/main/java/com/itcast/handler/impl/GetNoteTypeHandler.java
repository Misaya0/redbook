package com.itcast.handler.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Order(5)
@Slf4j
public class GetNoteTypeHandler extends NoteHandler {

    private final List<String> typeList = Arrays.asList("情感", "体育", "穿搭", "美食", "旅行", "搞笑", "音乐", "职场");

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        // 使用 String.join() 将列表转换为字符串
        String typeListString = String.join("、", typeList);

        // 生成最终的提示词
        String prompt = String.format(
                "你是一个专业的文本分类助手。请根据以下标题和内容，在 %s 这些类别中选择最合适的一个，并且只返回该类别的名称，不要提供任何额外的解释或回答。" +
                        "标题：%s" +
                        "内容：%s" +
                        "请只返回类别名称。如果没有合适的类别，请返回“其他”",
                typeListString, noteDto.getTitle(), noteDto.getContent());

        // 发送请求并获取响应
        String type = sendRequest(prompt);

        // 清理大模型返回的结果，去除特殊标记
        if (type != null) {
            type = cleanTypeResult(type);
        }

        noteDto.setType(type);
    }

    /**
     * 清理大模型返回的类型结果
     * 去除 <|begin_of_box|> 和 <|end_of_box|> 等特殊标记
     */
    private String cleanTypeResult(String type) {
        if (type == null || type.isEmpty()) {
            return type;
        }

        log.info("清理前的类型: {}", type);

        // 去除前后空格
        type = type.trim();

        // 去除 <|begin_of_box|> 和 <|end_of_box|> 标记
        type = type.replaceAll("<\\|begin_of_box\\|>", "");
        type = type.replaceAll("<\\|end_of_box\\|>", "");

        // 去除其他可能的特殊标记
        type = type.replaceAll("<[^>]+>", "");
        type = type.replaceAll("\\|[^|]+\\|", "");

        // 去除引号
        type = type.replaceAll("[\"'`]", "");

        // 去除换行符和多余空格
        type = type.replaceAll("\\s+", "");

        log.info("清理后的类型: {}", type);

        return type;
    }

    @Value("${glm.api_url}")
    private String apiUrl;

    @Value("${glm.api_key}")
    private String apiKey;

    @SuppressWarnings("unchecked")
    protected String sendRequest(String userMessage) {
        try {
            // 1. 创建 HttpClient
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // 2. 构造 JSON 请求体（GLM4 格式）
            String requestBody = String.format(
                    "{ \"model\": \"glm-4.5v\", \"messages\": [ " +
                            "{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, " +
                            "{\"role\": \"user\", \"content\": \"%s\"} " +
                            "], \"temperature\": 0.7 }",
                    userMessage
            );

            // 3. 创建 HTTP 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15)) // 请求超时时间
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 4. 发送请求并获取响应
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. 处理响应
            if (response.statusCode() == 200) {
                Type typeOfHashMap = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> responseMap = new Gson().fromJson(response.body(), typeOfHashMap);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                Map<String, Object> message = choices.get(0);
                Map<String, Object> messageObj = (Map<String, Object>) message.get("message");
                return (String) messageObj.get("content");
            } else {
                log.error("Request failed: {} - {}",response.statusCode(),response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error occurred: {}",e.getMessage());
            return null;
        }
    }
}
