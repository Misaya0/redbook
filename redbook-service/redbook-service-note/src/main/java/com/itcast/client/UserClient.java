package com.itcast.client;

import com.itcast.model.vo.AttentionVo;
import com.itcast.model.pojo.User;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("redbook-service-user")
public interface UserClient {

    /**
     * 单个查询用户信息
     */
    @GetMapping("/user/getUserById/{userId}")
    Result<User> getUserById(@PathVariable("userId") Integer userId);

    /**
     * 批量查询用户信息：用于避免 N+1 远程调用
     */
    @PostMapping("/user/getUsersByIds")
    Result<List<User>> getUsersByIds(@RequestBody List<Integer> userIds);

    /**
     * 获取关注列表
     */
    @GetMapping("/user/getAttention/{userId}")
    Result<List<AttentionVo>> getAttention(@PathVariable("userId") Integer userId);
}
