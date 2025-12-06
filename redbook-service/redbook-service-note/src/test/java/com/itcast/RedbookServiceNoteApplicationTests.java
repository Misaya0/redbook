package com.itcast;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itcast.constant.RedisConstant;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Note;
import com.itcast.util.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@SpringBootTest
@Slf4j
class RedbookServiceNoteApplicationTests {

    @Autowired
    private NoteMapper noteMapper;

    @Value("${map.key}")
    private String mapKey;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

//    @Test
//    void testMybatisPlus() {
//        Page<Note> page = new Page<>(1, 1);
//        page = noteMapper.selectPage(page, null);
//        System.out.println(page.getRecords());
//    }

    @Test
    void testRedisConnection() {
        // 1. 设置一个 Key
        String key = "test:connection";
        String value = "Hello Redis " + System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, value);
        log.info("Set Redis key: {} = {}", key, value);

        // 2. 获取这个 Key
        Object retrievedValue = redisTemplate.opsForValue().get(key);
        log.info("Get Redis key: {} = {}", key, retrievedValue);

        // 3. 验证值是否相等
        if (value.equals(retrievedValue)) {
            log.info("Redis connection test PASSED!");
        } else {
            log.error("Redis connection test FAILED! Expected: {}, but got: {}", value, retrievedValue);
        }

        // 4. 清理 Key
        redisTemplate.delete(key);
    }

    @Test
    void testMap() throws IOException, InterruptedException {
        double longitude = 116.481488;
        double latitude = 39.990464;
        String location = longitude + "," + latitude;
        String url = "https://restapi.amap.com/v3/geocode/regeo"
                .concat("?location=").concat(location)
                .concat("&key=").concat(mapKey);
        String response = HttpUtil.get(url);
        Type typeOfHashMap = new TypeToken<HashMap<String, Object>>(){}.getType();
        Map<String, Object> resultMap = new Gson().fromJson(response, typeOfHashMap);
//        HashMap resultMap = new Gson().fromJson(response, HashMap.class);
//        System.out.println(resultMap);
//        System.out.println(resultMap.get("regeocode"));
        Map regeocode = (Map) resultMap.get("regeocode");
        System.out.println(regeocode);
//        System.out.println(regeocode.get("formatted_address"));
    }

    @Test
    void testLocation() {
        List<Note> notes = noteMapper.selectList(null);
        for (Note note : notes) {
            RedisGeoCommands.GeoLocation<Object> location
                    = new RedisGeoCommands.GeoLocation<>(note.getId(),
                    new Point(note.getLongitude(), note.getLatitude()));
            redisTemplate.opsForGeo().add(RedisConstant.NOTE_GEO, location);
        }
    }

    @Test
    void testLocation2() {
//        GeoResults<RedisGeoCommands.GeoLocation<Object>> location = redisTemplate.opsForGeo().search("location", new Circle(
//                new Point(125.01940325459928, 45.092957286143765), new Distance(1, RedisGeoCommands.DistanceUnit.KILOMETERS)));
////        System.out.println(location.getContent());
//        List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> content = location.getContent();
//        content.forEach(result -> {
//            System.out.println(result.getContent().getName());
//            System.out.println(result.getDistance().getValue());
//        });
//        GeoResults<RedisGeoCommands.GeoLocation<Object>> content = redisTemplate.opsForGeo().search("location",
//                GeoReference.fromCoordinate(125.01940325459928, 45.092957286143765),
//                new Distance(5000, RedisGeoCommands.DistanceUnit.KILOMETERS));
//        content.forEach(result -> {
//            System.out.println(result.getContent().getName());
//            System.out.println(result.getDistance().getValue());
//        });

        //设置当前位置
        // // Point 中 x：经度"longitude":114.56，y：纬度"latitude":38.13
        Point point = new Point(125, 45);
        //设置半径范围 (KILOMETERS 千米；METERS 米)
        Metric metric = RedisGeoCommands.DistanceUnit.KILOMETERS;
        Distance distance = new Distance(1000, metric);
        Circle circle = new Circle(point, distance);
        //设置参数 包括距离、坐标、条数
        RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()//包含距离
                .includeCoordinates();//包含经纬度
//                .sortAscending()//正序排序
//                .limit(50); //条数
        GeoResults<RedisGeoCommands.GeoLocation<Object>> radius
                = redisTemplate.opsForGeo().radius("location",circle, geoRadiusCommandArgs);

        if (radius != null) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<Object>>> iterator = radius.iterator();
            while (iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult = iterator.next();
                // 与目标点相距的距离信息
                Distance geoResultDistance = geoResult.getDistance();
                // 该点的信息
                RedisGeoCommands.GeoLocation<Object> geoResultContent = geoResult.getContent();
                System.out.println(geoResultContent.getName());
                System.out.println(geoResultDistance.getValue());
            }
        }
    }

    @Test
    public void saveSensitiveWord() throws IOException {
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.227.200", 9200, "http")));
             BufferedReader reader = new BufferedReader(
                     new FileReader(
                             "D:\\projects\\redbook\\redbook-service\\redbook-service-note" +
                                     "\\src\\main\\resources\\sensitive_words_lines.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                IndexRequest request = new IndexRequest("rb_sensitive_word");
                request.source("sensitiveWord", line);
                client.index(request, RequestOptions.DEFAULT);
                System.out.println(line);
            }
        }
    }

    @Test
    void testSensitiveWord() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.227.200", 9200, "http")));
        SearchRequest request = new SearchRequest("rb_sensitive_word");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("sensitiveWord", "甲庆林"));
        request.source(sourceBuilder);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            // 4.解析JSON
            SensitiveWord sensitiveWord = new Gson().fromJson(source, SensitiveWord.class);
            System.out.println(sensitiveWord.getSensitiveWord());
        }
    }
    @Data
    static class SensitiveWord {
        private String sensitiveWord;
    }

    @Test
    void testEsSearch() {
        SearchRequest request = new SearchRequest("rb_note");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("title", "帅哥"));
        request.source(sourceBuilder);
        try(RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.227.200", 9200, "http")))) {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();
            for (SearchHit hit : hits) {
                String source = hit.getSourceAsString();
                // 4.解析JSON
                Note note = new Gson().fromJson(source, Note.class);
                System.out.println(note);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void testPublisherConfirm() {
        // 成功消费
        // rabbitTemplate.convertAndSend("publisher_confirm_exchange", "", "hello");

        // routingKey不存在 NO_ROUTE
        // rabbitTemplate.convertAndSend("publisher_confirm_exchange", "red", "hello");

        // 消费者消费失败
        rabbitTemplate.convertAndSend("publisher_confirm_exchange", "", "hello");
    }

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = "sk-c0f852e695e846508858e7f051a7ded6";
    private final List<String> typeList = Arrays.asList("情感", "体育", "穿搭", "美食");
    @Test
    @SuppressWarnings("unchecked")
    void testDeepseek() {
        String typeListString = String.join("，", typeList);
        String prompt = String.format(
                "你是一个专业的文本分类助手。请根据以下标题和内容，在 %s 这些类别中选择最合适的一个，并且只返回该类别的名称，不要提供任何额外的解释或回答。" +
                        "标题：%s" +
                        "内容：%s" +
                        "请只返回类别名称。",
                typeListString, "185阳光男友初春日常", "条纹衬衫很适合日常穿搭");
        String response = sendRequest(prompt);
        System.out.println(response);
        Type typeOfHashMap = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> responseMap = new Gson().fromJson(response, typeOfHashMap);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> message = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) message.get("message");
        String content = (String) messageObj.get("content");
        System.out.println("Response from DeepSeek: " + content);
    }

    public static String sendRequest(String userMessage) {
        try {
            // 1. 创建 HttpClient
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)) // 设置连接超时时间
                    .build();

            // 2. 构造 JSON 请求体
            String requestBody = String.format(
                    "{ \"model\": \"deepseek-chat\", \"messages\": [ " +
                            "{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}, " +
                            "{\"role\": \"user\", \"content\": \"%s\"} " +
                            "], \"temperature\": 0.7 }",
                    userMessage
            );

            System.out.println(requestBody);

            // 3. 创建 HTTP 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY) // 认证
                    .header("Content-Type", "application/json") // 指定 JSON 格式
                    .timeout(Duration.ofSeconds(15)) // 请求超时时间
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)) // 发送 JSON 请求体
                    .build();

            // 4. 发送请求并获取响应
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. 处理响应
            if (response.statusCode() == 200) {
                return response.body(); // 返回 JSON 响应
            } else {
                System.err.println("Request failed: " + response.statusCode() + " - " + response.body());
                return null;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
            return null;
        }
    }
}