package com.itcast.controller;

import com.itcast.result.Result;
import com.itcast.service.LoginService;
import com.itcast.model.dto.LoginDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录模块", description = "用户登录认证相关接口")
@RestController
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Operation(summary = "发送验证码", description = "向指定手机号码发送短信验证码")
    @GetMapping("/send/{phone}")
    public Result<String> send(
            @Parameter(description = "手机号码", required = true) @PathVariable String phone) {
        return loginService.send(phone);
    }

    @Operation(summary = "验证登录", description = "验证手机号码和验证码，登录成功后返回token和角色")
    @GetMapping("/verify")
    public Result<com.itcast.model.vo.LoginVo> verify(
            @Parameter(description = "登录信息（手机号和验证码）", required = true) LoginDto loginDto) {
        return loginService.verify(loginDto);
    }
}
