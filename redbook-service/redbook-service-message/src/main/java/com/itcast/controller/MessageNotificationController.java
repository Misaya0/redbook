package com.itcast.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.context.UserContext;
import com.itcast.entity.Notification;
import com.itcast.model.vo.NotificationVo;
import com.itcast.result.Result;
import com.itcast.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@SecurityRequirement(name = "bearerAuth")
@Tag(name = "消息通知模块", description = "用户消息通知相关接口")
@RestController
@RequestMapping("/message")
public class MessageNotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "获取未读消息汇总", description = "获取当前用户各类未读消息数量汇总")
    @GetMapping("/unread/summary")
    public Result<Map<String, Integer>> getUnreadSummary() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getUnreadSummary(userId));
    }

    @Operation(summary = "获取消息通知列表", description = "分页获取指定类型的消息通知列表")
    @GetMapping("/notifications")
    public Result<Page<NotificationVo>> getNotifications(
            @Parameter(description = "消息分组", required = true) @RequestParam String group,
            @Parameter(description = "页码", required = false) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", required = false) @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getNotifications(userId, group, page, size));
    }

    @Operation(summary = "标记消息组已读", description = "将指定类型的消息全部标记为已读")
    @PostMapping("/unread/markRead")
    public Result<String> markGroupRead(
            @Parameter(description = "消息分组", required = true) @RequestParam String group) {
        Long userId = getCurrentUserId();
        notificationService.markGroupRead(userId, group);
        return Result.success("标记消息组已读成功");
    }

    @Operation(summary = "标记单条消息已读", description = "将指定ID的消息标记为已读")
    @PostMapping("/notifications/{id}/read")
    public Result<String> markOneRead(
            @Parameter(description = "消息ID", required = true) @PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.markOneRead(userId, id);
        return Result.success("标记单条消息已读成功");
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // For robust handling, but existing interceptors should handle this.
            // Returning null or throwing might depend on global handler.
            throw new RuntimeException("User not logged in");
        }
        return userId;
    }
}
