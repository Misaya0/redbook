package com.itcast.client;

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

    @GetMapping("/user/getUserById/{userId}")
    Result<User> getUserById(@PathVariable("userId") Long userId);

    /**
     * 批量根据用户ID获取用户信息
     * @param ids 用户ID列表
     * @return 用户列表
     */
    @PostMapping("/user/getUsersByIds")
    Result<List<User>> getUsersByIds(@RequestBody List<Long> ids);
}
