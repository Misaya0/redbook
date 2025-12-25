package com.itcast.service.impl;

import com.google.gson.Gson;
import com.itcast.client.ProductClient;
import com.itcast.client.UserClient;
import com.itcast.constant.RedisConstant;
import com.itcast.mapper.HistoryMapper;
import com.itcast.mapper.UserMapper;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.model.pojo.History;
import com.itcast.model.pojo.Note;

import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.User;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.SearchService;
import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    private static final Gson GSON = new Gson();

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
        
        // 添加高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").field("content");
        highlightBuilder.preTags("<span style='color:red'>").postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        
        request.source(sourceBuilder);
        // 3.获取结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        
        // 4.解析结果并填充用户信息
        List<NoteVo> noteVos = fillNoteVoList(hits);

        // 5.保存搜索记录
        saveSearchHistory(key);
        
        // 6.保存热度
        incrementHotScore(key);
        return Result.success(noteVos);
    }

    /**
     * 保存搜索历史记录
     */
    private void saveSearchHistory(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }
        // 注意：UserContext 基于 ThreadLocal，异步线程无法直接获取用户ID，因此这里需要提前捕获
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                History history = new History();
                history.setHistory(keyword);
                history.setUserId(userId);
                historyMapper.insert(history);
            } catch (DuplicateKeyException e) {
                // 数据库唯一索引冲突（重复搜索记录）属于正常情况，记录日志即可，不影响主流程
                log.info("用户搜索历史重复，忽略写入 userId:{} keyword:{}", userId, keyword);
            } catch (Exception e) {
                // 捕获所有异常，避免影响搜索主流程
                log.warn("用户搜索历史写入失败 userId:{} keyword:{}", userId, keyword, e);
            }
        });
    }

    /**
     * 解析 Note 搜索结果并批量填充用户信息
     */
    private List<NoteVo> fillNoteVoList(SearchHit[] hits) {
        if (hits == null || hits.length == 0) {
            return new ArrayList<>();
        }

        List<NoteVo> noteVos = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        for (SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            // 1. 解析JSON
            Note note = GSON.fromJson(source, Note.class);

            // 2. 处理高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                if (highlightFields.containsKey("title")) {
                    note.setTitle(Arrays.stream(highlightFields.get("title").fragments())
                            .map(Text::string)
                            .collect(Collectors.joining()));
                }
                if (highlightFields.containsKey("content")) {
                    note.setContent(Arrays.stream(highlightFields.get("content").fragments())
                            .map(Text::string)
                            .collect(Collectors.joining()));
                }
            }

            // 3. 转换为 VO
            NoteVo noteVo = new NoteVo();
            BeanUtils.copyProperties(note, noteVo);
            noteVos.add(noteVo);
            
            // 4. 收集用户ID用于批量查询
            if (note.getUserId() != null) {
                userIds.add(note.getUserId());
            }
        }

        // 5. 批量查询用户信息并填充
        if (!userIds.isEmpty()) {
            try {
                // 去重
                List<Long> distinctUserIds = userIds.stream().distinct().collect(Collectors.toList());
                Result<List<User>> usersResult = userClient.getUsersByIds(distinctUserIds);
                if (usersResult != null && usersResult.getData() != null) {
                    Map<Long, User> userMap = usersResult.getData().stream()
                            .collect(Collectors.toMap(User::getId, u -> u, (k1, k2) -> k1));
                    
                    for (NoteVo vo : noteVos) {
                        if (vo.getUserId() != null) {
                            vo.setUser(userMap.get(vo.getUserId()));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("批量获取用户信息失败", e);
            }
        }

        return noteVos;
    }

    @Override
    public Result<Object> searchProduct(com.itcast.model.dto.ProductSearchDto searchDto) throws IOException {
        SearchRequest request = new SearchRequest("rb_product");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // BoolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Keyword
        if (StringUtils.isNotBlank(searchDto.getKeyword())) {
            boolQuery.must(QueryBuilders.multiMatchQuery(searchDto.getKeyword(), "name", "title", "tags"));
        }

        // Category
        if (searchDto.getCategoryId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("categoryId", searchDto.getCategoryId()));
        }

        // Shop
        if (searchDto.getShopId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("shopId", searchDto.getShopId()));
        }

        // Price Range
        if (searchDto.getMinPrice() != null || searchDto.getMaxPrice() != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (searchDto.getMinPrice() != null) rangeQuery.gte(searchDto.getMinPrice());
            if (searchDto.getMaxPrice() != null) rangeQuery.lte(searchDto.getMaxPrice());
            boolQuery.filter(rangeQuery);
        }

        sourceBuilder.query(boolQuery);

        // Paging
        int page = searchDto.getPageNum() == null ? 1 : searchDto.getPageNum();
        int size = searchDto.getPageSize() == null ? 10 : searchDto.getPageSize();
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name").preTags("<span style='color:red'>").postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);

        request.source(sourceBuilder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();

        List<Product> productList = new ArrayList<>();
        for (SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            Product product = GSON.fromJson(source, Product.class);
            
            // Handle highlight
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields.containsKey("name")) {
                product.setName(Arrays.stream(highlightFields.get("name").fragments())
                        .map(Text::string).collect(Collectors.joining()));
            }
            
            productList.add(product);
        }

        return Result.success(productList, response.getHits().getTotalHits().value);
    }

    @Override
    public Result<Object> searchAll(String keyword, Integer type, Integer page, Integer size) throws IOException {
        if (type == null) type = 0;
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        if (type == 1) { // User
            int offset = (page - 1) * size;
            List<User> users = userMapper.searchUsers(keyword, offset, size);
            return Result.success(users == null ? new ArrayList<>() : users);

        } else if (type == 2) { // Product
            Result<List<Product>> result = productClient.search(keyword, null, null, page, size);
            List<Product> products = result == null ? null : result.getData();
            return Result.success(products == null ? new ArrayList<>() : products);

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

            List<NoteVo> noteVos = fillNoteVoList(hits);

            saveSearchHistory(keyword);
            incrementHotScore(keyword);

            return Result.success(noteVos);
        }
    }

    private void incrementHotScore(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }

        redisTemplate.opsForZSet().incrementScore(RedisConstant.NOTE_SCORE, keyword, 1);

        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String todayKey = RedisConstant.NOTE_SCORE_TODAY_PREFIX + dateStr;

        Boolean existed = redisTemplate.hasKey(todayKey);
        redisTemplate.opsForZSet().incrementScore(todayKey, keyword, 1);
        if (existed == null || !existed) {
            redisTemplate.expire(todayKey, 48, TimeUnit.HOURS);
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

    @Override
    public Result<Void> syncAllProductsToEs() throws IOException {
        int pageNum = 1;
        int pageSize = 200;

        while (true) {
            Result<List<ProductEsDTO>> result = productClient.getProductEsList(pageNum, pageSize);
            List<ProductEsDTO> products = result == null ? null : result.getData();
            if (products == null || products.isEmpty()) {
                break;
            }

            BulkRequest bulkRequest = new BulkRequest();
            for (ProductEsDTO dto : products) {
                if (dto == null || dto.getId() == null) {
                    continue;
                }
                IndexRequest indexRequest = new IndexRequest("rb_product").id(dto.getId().toString());
                indexRequest.source(GSON.toJson(dto), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            if (bulkRequest.numberOfActions() > 0) {
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                if (bulkResponse.hasFailures()) {
                    return Result.failure(bulkResponse.buildFailureMessage());
                }
            }

            if (products.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        return Result.success(null);
    }
}
