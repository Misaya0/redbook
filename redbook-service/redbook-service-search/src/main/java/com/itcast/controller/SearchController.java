package com.itcast.controller;

import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "搜索模块", description = "笔记搜索相关接口")
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Operation(summary = "搜索笔记", description = "根据关键词搜索笔记")
    @GetMapping("/search/{key}")
    public Result<List<NoteVo>> search(
            @Parameter(description = "搜索关键词", required = true) @PathVariable String key) throws IOException {
        return searchService.search(key);
    }

    @Operation(summary = "综合搜索", description = "根据关键词、类型、分页参数搜索")
    @GetMapping("/all")
    public Result<Object> searchAll(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "类型(0-笔记, 1-用户, 2-商品)") @RequestParam(required = false, defaultValue = "0") Integer type,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "页大小") @RequestParam(required = false, defaultValue = "10") Integer size) throws IOException {
        return searchService.searchAll(keyword, type, page, size);
    }

    @Operation(summary = "搜索建议", description = "根据关键词获取搜索建议")
    @GetMapping("/suggest")
    public Result<List<String>> suggest(
            @Parameter(description = "关键词") @RequestParam String keyword) throws IOException {
        return searchService.suggest(keyword);
    }

    @Operation(summary = "搜索商品 (ES)", description = "商品专用高级搜索")
    @PostMapping("/product")
    public Result<Object> searchProduct(@RequestBody ProductSearchDto searchDto) throws IOException {
        return searchService.searchProduct(searchDto);
    }

    @Operation(summary = "全量同步商品到ES", description = "将商品服务的MySQL商品全量写入 rb_product")
    @PostMapping("/product/syncAll")
    public Result<Void> syncAllProductsToEs() throws IOException {
        return searchService.syncAllProductsToEs();
    }
}
