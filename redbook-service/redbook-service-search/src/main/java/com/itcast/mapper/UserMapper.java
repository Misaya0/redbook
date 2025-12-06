package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.model.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM rb_user.rb_user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') OR number LIKE CONCAT('%', #{keyword}, '%')")
    List<User> searchUsers(String keyword);
}
