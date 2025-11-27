package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.service.UserService;
import com.itcast.model.pojo.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;

@Tag(name = "用户模块", description = "用户基本信息 / 个人页相关接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/getInfo")
    public Result<User> getInfo() throws ParseException {
        return userService.getInfo();
    }

    @Operation(summary = "根据ID查询用户信息", description = "传入用户ID，返回用户详情数据")
    @GetMapping("/getUserById/{userId}")
    public Result<User> getUserById(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Integer userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "更新用户头像", description = "上传用户头像图片文件")
    @PostMapping("/updateImage")
    public Result<Void> updateImage(
            @Parameter(description = "头像图片文件", required = true) @RequestParam("image") MultipartFile file) throws IOException {
        return userService.updateImage(file);
    }

    @Operation(summary = "编辑用户信息", description = "更新用户的基本信息")
    @PutMapping("/editInfo")
    public Result<Void> editInfo(
            @Parameter(description = "用户信息对象", required = true) @RequestBody User user) {
        return userService.editInfo(user);
    }
}
