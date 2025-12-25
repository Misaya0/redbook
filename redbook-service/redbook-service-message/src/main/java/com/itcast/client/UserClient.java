package com.itcast.client;

import com.itcast.model.pojo.User;
import com.itcast.model.vo.AttentionVo;
import com.itcast.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "redbook-service-user")
public interface UserClient {

    @GetMapping("/user/getUserById/{userId}")
    Result<User> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/user/getAttention/{userId}")
    Result<List<AttentionVo>> getAttention(@PathVariable("userId") Long userId);

    @GetMapping("/user/isAttention/{otherId}")
    Result<Integer> isAttention(@PathVariable("otherId") Long otherId);
}
