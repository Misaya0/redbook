package com.itcast.controller;

import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.Sku;
import com.itcast.model.vo.ProductSpecsVo;
import com.itcast.model.vo.ProductVo;
import com.itcast.model.vo.SpecGroupVo;
import com.itcast.result.Result;
import com.itcast.mapper.ProductMapper;
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

    @Autowired
    private ProductMapper productMapper;

    @Operation(summary = "获取商品列表", description = "获取所有商品列表")
    @GetMapping("/getProductList")
    public Result<List<Product>> getProductList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return productService.getProductList(pageNum, pageSize);
    }

    @Operation(summary = "批量获取商品", description = "根据商品ID列表批量查询商品信息")
    @PostMapping("/getProductsByIds")
    public Result<List<Product>> getProductsByIds(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(productMapper.selectBatchIds(ids));
    }

    @Operation(summary = "获取商品ES同步列表", description = "用于全量同步到ES的商品列表")
    @GetMapping("/getProductEsList")
    public Result<List<ProductEsDTO>> getProductEsList(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "100") Integer pageSize) {
        return productService.getProductEsList(pageNum, pageSize);
    }

    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详细信息")
    @GetMapping("/getProduct/{productId}")
    public Result<ProductVo> getProduct(
            @Parameter(description = "商品ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.getProduct(productId);
    }

    @Operation(summary = "获取商品规格Schema", description = "按品类返回规格组与SKU规格映射")
    @GetMapping("/getProductSpecs/{productId}")
    public Result<ProductSpecsVo> getProductSpecs(
            @Parameter(description = "商品ID", required = true) @PathVariable("productId") Integer productId) {
        return productService.getProductSpecs(productId);
    }

    @Operation(summary = "获取品类规格Schema", description = "按品类返回规格组与选项")
    @GetMapping("/getCategorySpecs/{categoryId}")
    public Result<List<SpecGroupVo>> getCategorySpecs(
            @Parameter(description = "品类ID", required = true) @PathVariable("categoryId") Integer categoryId) {
        return productService.getCategorySpecs(categoryId);
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

    @Operation(summary = "获取SKU详情", description = "根据skuId获取SKU信息")
    @GetMapping("/getSku/{skuId}")
    public Result<Sku> getSku(
            @Parameter(description = "SKU ID", required = true) @PathVariable("skuId") Long skuId) {
        return productService.getSku(skuId);
    }

    public static class DecreaseStockRequest {
        public Long skuId;
        public Integer quantity;
    }

    @Operation(summary = "扣减SKU库存", description = "按SKU维度扣减库存（原子校验 stock>=quantity）")
    @PostMapping("/decreaseSkuStock")
    public Result<Void> decreaseSkuStock(@RequestBody DecreaseStockRequest request) {
        if (request == null) {
            return Result.failure("参数不能为空");
        }
        return productService.decreaseSkuStock(request.skuId, request.quantity);
    }

    public static class IncreaseStockRequest {
        public Long skuId;
        public Integer quantity;
    }

    @Operation(summary = "回补SKU库存", description = "按SKU维度回补库存（用于下单失败补偿）")
    @PostMapping("/increaseSkuStock")
    public Result<Void> increaseSkuStock(@RequestBody IncreaseStockRequest request) {
        if (request == null) {
            return Result.failure("参数不能为空");
        }
        return productService.increaseSkuStock(request.skuId, request.quantity);
    }
}
