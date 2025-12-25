package com.itcast.controller;

import com.itcast.model.vo.AttentionVo;
import com.itcast.result.Result;
import com.itcast.service.AttentionService;
import com.itcast.model.pojo.Attention;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "关注模块", description = "用户关注/粉丝相关接口")
@RestController
@RequestMapping("/user")
public class AttentionController {

    @Autowired
    private AttentionService attentionService;

    @Operation(summary = "查询是否关注", description = "查询当前用户是否关注了指定用户")
    @GetMapping("/isAttention/{otherId}")
    public Result<Integer> isAttention(
            @Parameter(description = "目标用户ID", required = true) @PathVariable("otherId") Long otherId){
        return attentionService.isAttention(otherId);
    }

    @Operation(summary = "关注/取消关注", description = "关注或取消关注指定用户")
    @GetMapping("/attention/{otherId}")
    public Result<Void> attention(
            @Parameter(description = "目标用户ID", required = true) @PathVariable("otherId") Long otherId) {
        return attentionService.attention(otherId);
    }

    @Operation(summary = "获取关注列表", description = "获取指定用户的关注列表")
    @GetMapping("/getAttention/{userId}")
    public Result<List<AttentionVo>> getAttention(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
        return attentionService.getAttention(userId);
    }

    @Operation(summary = "获取粉丝列表", description = "获取指定用户的粉丝列表")
    @GetMapping("/getFans/{userId}")
    public Result<List<AttentionVo>> getFans(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
        return attentionService.getFans(userId);
    }
}
