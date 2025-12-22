package com.itcast.client;

import com.itcast.model.pojo.User;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务远程调用客户端
 */
@Component
@FeignClient("redbook-service-user")
public interface UserClient {

    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/getUserById/{userId}")
    Result<User> getUserById(@PathVariable("userId") Integer userId);
}

