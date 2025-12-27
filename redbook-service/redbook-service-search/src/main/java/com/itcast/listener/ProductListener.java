package com.itcast.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.constant.MqConstant;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.model.dto.ProductSyncMessage;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class ProductListener {

    @Autowired
    private RestHighLevelClient client;
    
    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.PRODUCT_ES_SYNC_QUEUE),
            exchange = @Exchange(name = MqConstant.PRODUCT_ES_EXCHANGE),
            key = MqConstant.PRODUCT_ES_SYNC_KEY
    ))
    public void listenProductSync(String msg) {
        log.info("Received product sync message: {}", msg);
        try {
            // Message format: {"type": "save/delete", "data": {...}}
            // For simplicity, let's assume the message is the ProductDoc itself for save/update
            // and maybe a special structure for delete.
            // Or better, define a SyncMessage class.
            // But to be quick, let's assume productDto is passed.
            // However, we need ProductDoc in ES.
            
            // Let's expect a SyncDTO wrapper or just handle simple logic.
            // Let's assume the message is a JSON string of a wrapper object.
            ProductSyncMessage syncMessage = objectMapper.readValue(msg, ProductSyncMessage.class);
            
            if ("delete".equals(syncMessage.getType())) {
                DeleteRequest request = new DeleteRequest("rb_product", syncMessage.getId().toString());
                executeWithRetry(() -> client.delete(request, RequestOptions.DEFAULT), "delete", syncMessage.getId());
                log.info("Deleted product from ES: {}", syncMessage.getId());
            } else {
                ProductEsDTO doc = syncMessage.getData();
                if (doc != null) {
                    IndexRequest request = new IndexRequest("rb_product").id(doc.getId().toString());
                    request.source(objectMapper.writeValueAsString(doc), XContentType.JSON);
                    executeWithRetry(() -> client.index(request, RequestOptions.DEFAULT), "index", doc.getId());
                    log.info("Indexed product to ES: {}", doc.getId());
                }
            }
        } catch (IOException e) {
            log.error("Failed to sync product to ES", e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private void executeWithRetry(IoAction action, String op, Long productId) throws IOException {
        int maxAttempts = 3;
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                return;
            } catch (IOException e) {
                last = e;
                log.warn("ES {} failed, attempt {}/{}, productId={}, msg={}", op, attempt, maxAttempts, productId, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(300L * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw last;
    }
}
