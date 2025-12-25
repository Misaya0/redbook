package com.itcast.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class AttentionVo implements Serializable {
    private Long userId;
    private String nickname;
    private String image;
    private String time; // 关注时间
}
