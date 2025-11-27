package com.itcast.handler.impl;

import com.itcast.handler.NoteHandler;
import com.itcast.model.dto.NoteDto;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

//@Component
@Order(4)
public class FilterTitleHandler extends NoteHandler {

    /**
     * 敏感词
     */
    @Data
    public static class SensitiveWord {
        private String sensitiveWord;
    }

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final String SENSITIVE_WORD_INDEX = "rb_sensitive_word";

    @Override
    public void handle(NoteDto noteDto) throws IOException, InterruptedException {
        String newTitle = sensitiveWordFilter(noteDto.getTitle());
        noteDto.setTitle(newTitle);
    }

    /**
     * 敏感词过滤
     * @param text
     * @return
     */
    public String sensitiveWordFilter(String text) {
        try {
            // 1.构建搜索查询
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(matchQuery("sensitiveWord", text))
                    .build();

            // 2.执行搜索请求
            List<SearchHit<SensitiveWord>> searchHits = elasticsearchOperations.search(
                    searchQuery, 
                    SensitiveWord.class, 
                    IndexCoordinates.of(SENSITIVE_WORD_INDEX)
            ).getSearchHits();

            // 3.提取敏感词列表
            List<String> sensitiveWords = searchHits.stream()
                    .map(hit -> hit.getContent().getSensitiveWord())
                    .collect(Collectors.toList());

            // 4.过滤敏感词
            for (String sensitiveWord : sensitiveWords) {
                text = text.replaceAll(sensitiveWord, "**");
            }
            return text;
        } catch (Exception e) {
            return text; // 如果搜索失败，返回原文
        }
    }
}
