package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "点赞模块", description = "笔记点赞相关接口")
@RestController
@RequestMapping("/note")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Operation(summary = "点赞/取消点赞笔记", description = "切换笔记的点赞状态")
    @PutMapping("/like/{noteId}")
    public Result<Void> like(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return likeService.like(noteId);
    }

    @Operation(summary = "检查是否已点赞", description = "检查当前用户是否已点赞指定笔记")
    @GetMapping("/isLike/{noteId}")
    public Result<Boolean> isLike(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return likeService.isLike(noteId);
    }
}
