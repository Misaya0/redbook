package com.itcast.controller;

import com.itcast.model.dto.CommentDto;
import com.itcast.model.vo.CommentVo;
import com.itcast.result.Result;
import com.itcast.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Tag(name = "评论模块", description = "笔记评论相关接口")
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Operation(summary = "发布评论", description = "对指定笔记发布评论")
    @PostMapping("/postComment")
    public Result<Void> postComment(
            @Parameter(description = "评论信息", required = true) @RequestBody CommentDto commentDto) {
        return commentService.postComment(commentDto);
    }

    @Operation(summary = "获取评论列表", description = "获取指定笔记的所有评论")
    @GetMapping("/getCommentList/{noteId}")
    public Result<List<CommentVo>> getCommentList(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) throws ParseException {
        return commentService.getCommentList(noteId);
    }

    @Operation(summary = "获取评论数量", description = "获取指定笔记的评论总数")
    @GetMapping("/getCommentCount/{noteId}")
    public Result<Integer> getCommentCount(
            @Parameter(description = "笔记ID", required = true) @PathVariable("noteId") Long noteId) {
        return commentService.getCommentCount(noteId);
    }

    @Operation(summary = "点赞评论", description = "对指定评论进行点赞")
    @PostMapping("/like/{commentId}")
    public Result<Void> likeComment(
            @Parameter(description = "评论ID", required = true) @PathVariable("commentId") Long commentId) {
        return commentService.likeComment(commentId);
    }

    @Operation(summary = "取消点赞评论", description = "取消对指定评论的点赞")
    @PostMapping("/unlike/{commentId}")
    public Result<Void> unlikeComment(
            @Parameter(description = "评论ID", required = true) @PathVariable("commentId") Long commentId) {
        return commentService.unlikeComment(commentId);
    }

}
