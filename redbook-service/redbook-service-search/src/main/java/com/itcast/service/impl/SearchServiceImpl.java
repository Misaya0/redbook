package com.itcast.service.impl;

import com.google.gson.Gson;
import com.itcast.client.ProductClient;
import com.itcast.client.UserClient;
import com.itcast.constant.RedisConstant;
import com.itcast.mapper.HistoryMapper;
import com.itcast.mapper.UserMapper;
import com.itcast.model.pojo.History;
import com.itcast.model.pojo.Note;

import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.SearchService;
import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private HistoryMapper historyMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<List<NoteVo>> search(String key) throws IOException {
        // 1.构造request
        SearchRequest request = new SearchRequest("rb_note");
        // 2.多字段查询
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery(key, "title", "content"));
        request.source(sourceBuilder);
        // 3.获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        // 4.设置vo
        List<NoteVo> noteVos = new ArrayList<>();
        for (SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            // 4.1 解析JSON
            Note note = new Gson().fromJson(source, Note.class);
            // 4.2 设置vo
            NoteVo noteVo = new NoteVo();
            BeanUtils.copyProperties(note, noteVo);
            noteVo.setUser(userClient.getUserById(note.getUserId()).getData());
            noteVos.add(noteVo);
        }



        // 5.保存搜索记录
        try {
            History history = new History();
            history.setHistory(key);
            history.setUserId(UserContext.getUserId());
            historyMapper.insert(history);
        } catch (Exception e) {
            log.info("用户搜索重复");
        }
        // 6.保存热度
        redisTemplate.opsForZSet().incrementScore(RedisConstant.NOTE_SCORE, key, 1);
        return Result.success(noteVos);
    }

    @Override
    public Result<Object> searchAll(String keyword, Integer type, Integer page, Integer size) throws IOException {
        if (type == null) type = 0;
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        if (type == 1) { // User
            List<User> users = userMapper.searchUsers(keyword);
            if (users == null) users = new ArrayList<>();
            int fromIndex = (page - 1) * size;
            if (fromIndex >= users.size()) {
                return Result.success(new ArrayList<>());
            }
            int toIndex = Math.min(fromIndex + size, users.size());
            return Result.success(users.subList(fromIndex, toIndex));

        } else if (type == 2) { // Product
            Result<List<Product>> result = productClient.search(keyword, null, null);
            List<Product> products = result.getData();
            if (products == null) products = new ArrayList<>();

            int fromIndex = (page - 1) * size;
            if (fromIndex >= products.size()) {
                return Result.success(new ArrayList<>());
            }
            int toIndex = Math.min(fromIndex + size, products.size());
            return Result.success(products.subList(fromIndex, toIndex));

        } else { // Note (ES)
            SearchRequest request = new SearchRequest("rb_note");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "title", "content"));

            sourceBuilder.from((page - 1) * size);
            sourceBuilder.size(size);

            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title").field("content");
            highlightBuilder.preTags("<span style='color:red'>").postTags("</span>");
            sourceBuilder.highlighter(highlightBuilder);

            request.source(sourceBuilder);

            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();

            List<NoteVo> noteVos = new ArrayList<>();
            for (SearchHit hit : hits) {
                String source = hit.getSourceAsString();
                Note note = new Gson().fromJson(source, Note.class);

                // Highlight handling
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (highlightFields.containsKey("title")) {
                    Text[] fragments = highlightFields.get("title").fragments();
                    StringBuilder title = new StringBuilder();
                    for (Text text : fragments) title.append(text);
                    note.setTitle(title.toString());
                }
                if (highlightFields.containsKey("content")) {
                    Text[] fragments = highlightFields.get("content").fragments();
                    StringBuilder content = new StringBuilder();
                    for (Text text : fragments) content.append(text);
                    note.setContent(content.toString());
                }

                NoteVo noteVo = new NoteVo();
                BeanUtils.copyProperties(note, noteVo);
                try {
                    Result<User> userResult = userClient.getUserById(note.getUserId());
                    if (userResult != null && userResult.getData() != null) {
                        noteVo.setUser(userResult.getData());
                    }
                } catch (Exception e) {
                    log.error("Failed to get user info for note", e);
                }
                noteVos.add(noteVo);
            }
            
            try {
                History history = new History();
                history.setHistory(keyword);
                history.setUserId(UserContext.getUserId());
                historyMapper.insert(history);
                redisTemplate.opsForZSet().incrementScore(RedisConstant.NOTE_SCORE, keyword, 1);
            } catch (Exception e) {
                // ignore
            }

            return Result.success(noteVos);
        }
    }

    @Override
    public Result<List<String>> suggest(String keyword) throws IOException {
        SearchRequest request = new SearchRequest("rb_note");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        // Use match phrase prefix for suggestion
        sourceBuilder.query(QueryBuilders.matchPhrasePrefixQuery("title", keyword));
        sourceBuilder.fetchSource("title", null);
        sourceBuilder.size(10);
        
        request.source(sourceBuilder);
        
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        
        List<String> suggestions = new ArrayList<>();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            if (sourceAsMap != null && sourceAsMap.containsKey("title")) {
                suggestions.add((String) sourceAsMap.get("title"));
            }
        }
        return Result.success(suggestions);
    }
}
