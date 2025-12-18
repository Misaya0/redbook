package com.itcast.client;

import com.itcast.model.pojo.Product;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("redbook-service-product")
public interface ProductClient {

    @GetMapping("/product/search")
    Result<List<Product>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice
    );

    @GetMapping("/product/getProductList")
    Result<List<Product>> getProductList(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize
    );

    @GetMapping("/product/getProductEsList")
    Result<List<ProductEsDTO>> getProductEsList(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize
    );
}
