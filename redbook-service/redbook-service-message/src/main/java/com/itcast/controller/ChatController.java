package com.itcast.controller;

import com.itcast.context.UserContext;
import com.itcast.entity.ChatMessage;
import com.itcast.entity.Conversation;
import com.itcast.model.vo.ConversationVo;
import com.itcast.result.Result;
import com.itcast.service.IChatMessageService;
import com.itcast.service.IConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "聊天模块", description = "用户聊天相关接口")
@RestController
@RequestMapping("/message/chat")
public class ChatController {

    @Autowired
    private IConversationService conversationService;

    @Autowired
    private IChatMessageService chatMessageService;

    @Operation(summary = "获取会话列表", description = "获取当前用户的会话列表")
    @GetMapping("/conversation/list")
    public Result<List<ConversationVo>> getConversationList() {
        Long userId = UserContext.getUserId();
        return Result.success(conversationService.getConversationList(userId));
    }

    @Operation(summary = "获取聊天历史记录", description = "获取与指定用户的聊天历史记录")
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(
            @Parameter(description = "聊天对象ID", required = true) @RequestParam Long talkerId) {
        Long userId = UserContext.getUserId();
        return Result.success(chatMessageService.getHistory(userId, talkerId));
    }

    @Operation(summary = "标记会话已读", description = "将与指定用户的会话未读数清零")
    @PostMapping("/conversation/read")
    public Result<Void> markAsRead(
            @Parameter(description = "聊天对象ID", required = true) @RequestParam Long talkerId) {
        Long userId = UserContext.getUserId();
        conversationService.clearUnreadCount(userId, talkerId);
        return Result.success(null);
    }

    @Operation(summary = "发送消息", description = "向指定用户发送消息")
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(
            @Parameter(description = "接收者ID", required = true) @RequestParam Long receiverId,
            @Parameter(description = "消息内容", required = true) @RequestParam String content,
            @Parameter(description = "消息类型(0:文本,1:图片)", required = false) @RequestParam(defaultValue = "0") Integer type) {
        Long userId = UserContext.getUserId();
        return Result.success(chatMessageService.sendMessage(userId, receiverId, content, type));
    }
}
