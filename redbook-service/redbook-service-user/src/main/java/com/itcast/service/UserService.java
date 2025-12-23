package com.itcast.service;

import com.itcast.result.Result;
import com.itcast.model.pojo.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public interface UserService {
    Result<User> getInfo() throws ParseException;

    Result<User> getUserById(Integer userId);

    /**
     * 批量查询用户信息：用于下游服务一次性回填用户信息，避免 N+1 远程调用
     */
    Result<List<User>> getUsersByIds(List<Integer> userIds);

    Result<Void> updateImage(MultipartFile file) throws IOException;

    Result<Void> editInfo(User user);
}
