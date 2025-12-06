package com.itcast.controller;

import com.itcast.model.dto.ProductDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;
import com.itcast.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品模块", description = "商品管理相关接口")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "获取商品列表", description = "获取所有商品列表")
    @GetMapping("/getProductList")
    public Result<List<Product>> getProductList() {
        return productService.getProductList();
    }

    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详细信息")
    @GetMapping("/getProduct/{productId}")
    public Result<ProductVo> getProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.getProduct(productId);
    }

    @Operation(summary = "按店铺获取商品", description = "获取指定店铺的所有商品")
    @GetMapping("/getProductsByShop/{productId}")
    public Result<List<Product>> getProductsByShop(
            @Parameter(description = "商品ID/店铺ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.getProductByShop(productId);
    }

    @Operation(summary = "发布商品", description = "发布一个新的商品")
    @PostMapping("/postProduct")
    public Result<Void> postProduct(
            @Parameter(description = "商品信息", required = true) @RequestBody ProductDto productDto) {
        return productService.postProduct(productDto);
    }

    @Operation(summary = "更新商品", description = "更新商品信息")
    @PutMapping("/updateProduct")
    public Result<Void> updateProduct(
            @Parameter(description = "商品信息", required = true) @RequestBody Product product) {
        return productService.updateProduct(product);
    }

    @Operation(summary = "搜索商品", description = "按名称、价格搜索商品")
    @GetMapping("/search")
    public Result<List<Product>> search(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "最低价") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价") @RequestParam(required = false) Double maxPrice) {
        return productService.searchProduct(keyword, minPrice, maxPrice);
    }
}
