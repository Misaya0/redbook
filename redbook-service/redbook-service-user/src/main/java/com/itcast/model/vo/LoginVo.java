package com.itcast.model.vo;

import lombok.Data;

@Data
public class LoginVo {
    private String token;
    private Integer role;
}
