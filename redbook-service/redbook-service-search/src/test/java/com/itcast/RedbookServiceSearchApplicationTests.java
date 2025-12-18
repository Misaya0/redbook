package com.itcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.client.ProductClient;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.result.Result;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(classes = RedbookServiceSearchApplication.class)
class RedbookServiceSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private ObjectMapper objectMapper;

    }
}
