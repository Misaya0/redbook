package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏模块", description = "笔记收藏相关接口")
@RestController
@RequestMapping("/note")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否已收藏指定笔记")
    @GetMapping("/isCollection/{noteId}")
    public Result<Boolean> isCollection(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return collectionService.isCollection(noteId);
    }

    @Operation(summary = "收藏/取消收藏笔记", description = "切换笔记的收藏状态")
    @PutMapping("/collection/{noteId}")
    public Result<Void> collection(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return collectionService.collection(noteId);
    }
}
