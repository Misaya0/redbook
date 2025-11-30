package com.itcast.controller;

import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
