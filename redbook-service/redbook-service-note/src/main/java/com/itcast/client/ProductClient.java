package com.itcast.client;

import com.itcast.result.Result;
import com.itcast.model.vo.ProductSimpleVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 商品服务 Feign 客户端（笔记服务侧调用）
 */
@FeignClient("redbook-service-product")
public interface ProductClient {

    /**
     * 获取商品详情
     * 说明：为兼容不同的返回结构，这里用 Map 接收 data，再在 Service 层解析。
     */
    @GetMapping("/product/getProduct/{productId}")
    Result<Map<String, Object>> getProduct(@PathVariable("productId") Long productId);

    /**
     * 批量获取商品简要信息（用于笔记列表回填，避免 N+1）
     */
    @PostMapping("/product/getProductsByIds")
    Result<List<ProductSimpleVo>> getProductsByIds(@RequestBody List<Long> ids);
}
