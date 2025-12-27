package com.itcast.mq;

import com.itcast.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 销量更新消费者：支付成功后接收销量增量消息，更新 DB + ES。
 */
@Component
@Slf4j
public class SalesConsumer {

    private static final String SALES_EXCHANGE = "sales.exchange";
    private static final String SALES_ROUTING_KEY = "sales.update";
    private static final String SALES_QUEUE = "sales.update.queue";
    private static final String ES_INDEX_PRODUCT = "rb_product";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RestHighLevelClient client;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = SALES_QUEUE),
            exchange = @Exchange(name = SALES_EXCHANGE, type = "topic"),
            key = SALES_ROUTING_KEY
    ))
    @Transactional(rollbackFor = Exception.class)
    public void handleSalesUpdate(Map<String, Object> message) {
        if (message == null || message.isEmpty()) {
            log.warn("Sales update message is empty");
            return;
        }

        Long productId = getLong(message, "productId");
        Integer quantity = getInt(message, "quantity");
        Long skuId = getLong(message, "skuId");

        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("Invalid sales update message: {}", message);
            return;
        }

        int updatedProduct = productMapper.increaseSales(productId, quantity);

        if (updatedProduct <= 0) {
            log.error("Failed to increase sales in DB, productUpdated={}, message={}", updatedProduct, message);
            throw new IllegalStateException("DB sales update failed");
        }

        try {
            UpdateRequest request = new UpdateRequest(ES_INDEX_PRODUCT, String.valueOf(productId));
            request.retryOnConflict(3);

            Map<String, Object> params = new HashMap<>();
            params.put("num", quantity);

            String painless = "ctx._source.sales = (ctx._source.sales == null ? 0 : ctx._source.sales) + params.num";
            request.script(new Script(ScriptType.INLINE, "painless", painless, params));

            client.update(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.warn("ES doc not found when increasing sales, index={}, productId={}, quantity={}",
                        ES_INDEX_PRODUCT, productId, quantity);
                return;
            }
            log.error("Failed to update ES sales, productId={}, quantity={}", productId, quantity, e);
        } catch (Exception e) {
            log.error("Failed to update ES sales, productId={}, quantity={}", productId, quantity, e);
        }
    }

    private Long getLong(Map<String, Object> message, String key) {
        Object value = message.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInt(Map<String, Object> message, String key) {
        Object value = message.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
