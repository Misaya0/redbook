package com.itcast.controller;

import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;
import com.itcast.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;

@Tag(name = "商品模块", description = "商品管理相关接口")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "获取商品列表", description = "获取所有商品列表")
    @GetMapping("/getProductList")
    public Result<List<Product>> getProductList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return productService.getProductList(pageNum, pageSize);
    }

    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详细信息")
    @GetMapping("/getProduct/{productId}")
    public Result<ProductVo> getProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.getProduct(productId);
    }

    @Operation(summary = "按店铺获取商品", description = "获取指定店铺的所有商品")
    @GetMapping("/getProductsByShop/{shopId}")
    public Result<List<Product>> getProductsByShop(
            @Parameter(description = "店铺ID", required = true) @PathVariable("shopId") Integer shopId) {
        return productService.getProductByShop(shopId);
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
            @Parameter(description = "商品信息", required = true) @RequestBody ProductDto productDto) {
        return productService.updateProduct(productDto);
    }

    @Operation(summary = "删除商品", description = "删除商品")
    @DeleteMapping("/delete/{productId}")
    public Result<Void> deleteProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.deleteProduct(productId);
    }

    @Operation(summary = "搜索商品", description = "按名称、价格搜索商品")
    @PostMapping("/search")
    public Result<List<ProductVo>> search(
            @Parameter(description = "搜索条件") @RequestBody ProductSearchDto searchDto) {
        return productService.searchProduct(searchDto);
    }

    @Operation(summary = "上传商品图片", description = "上传商品图片")
    @PostMapping("/uploadImage")
    public Result<String> uploadImage(
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) throws IOException {
        return productService.uploadImage(file);
    }

    @Operation(summary = "获取商品分类", description = "获取商品分类列表")
    @GetMapping("/getCategoryList")
    public Result<List<com.itcast.model.pojo.Category>> getCategoryList(
            @Parameter(description = "父分类ID") @RequestParam(required = false) Integer parentId,
            @Parameter(description = "层级") @RequestParam(required = false) Integer level) {
        return productService.getCategoryList(parentId, level);
    }
}
