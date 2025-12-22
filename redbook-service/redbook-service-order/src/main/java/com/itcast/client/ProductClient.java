package com.itcast.client;

import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.Sku;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Component
@FeignClient("redbook-service-product")
public interface ProductClient {

    @GetMapping("/product/getProduct/{productId}")
    Result<ProductDetail> getProductById(@PathVariable Integer productId);

    @GetMapping("/product/getSku/{skuId}")
    Result<Sku> getSku(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/getProductSpecs/{productId}")
    Result<ProductSpecs> getProductSpecs(@PathVariable("productId") Integer productId);

    class DecreaseSkuStockRequest {
        public Long skuId;
        public Integer quantity;
    }

    @PostMapping("/product/decreaseSkuStock")
    Result<Void> decreaseSkuStock(@RequestBody DecreaseSkuStockRequest request);

    class IncreaseSkuStockRequest {
        public Long skuId;
        public Integer quantity;
    }

    @PostMapping("/product/increaseSkuStock")
    Result<Void> increaseSkuStock(@RequestBody IncreaseSkuStockRequest request);

    @GetMapping("/shop/myShop")
    Result<Shop> getMyShop();

    @GetMapping("/shop/getShopByUserId/{userId}")
    Result<Shop> getShopByUserId(@PathVariable("userId") Integer userId);

    @GetMapping("/shop/getShopById/{shopId}")
    Result<Shop> getShopById(@PathVariable("shopId") Integer shopId);

    class ProductDetail extends Product {
        public Shop shop;
        public List<Sku> skus;
    }

    class ProductSpecs {
        public Integer productId;
        public List<SpecGroup> specGroups;
        public List<SkuSpec> skus;
    }

    class SpecGroup {
        public String key;
        public String title;
        public String displayType;
        public List<SpecOption> options;
    }

    class SpecOption {
        public String value;
        public String label;
        public String image;
        public String badgeText;
        public String badgeType;
    }

    class SkuSpec {
        public Long id;
        public String name;
        public java.math.BigDecimal price;
        public Integer stock;
        public String image;
        public java.util.Map<String, String> specs;
    }

    class Shop {
        public Integer id;
        public String name;
        public String image;
        public String time;
        public Integer fans;
        public Integer sales;
        public Integer userId;
        public Integer linkAuthMode;
    }
}
