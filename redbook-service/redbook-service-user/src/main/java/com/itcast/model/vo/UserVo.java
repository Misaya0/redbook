package com.itcast.model.vo;

import com.itcast.model.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserVo extends User {
    /**
     * 年龄
     */
    private Integer age;
}
