package com.itcast.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AttentionVo implements Serializable {
    private Long userId;
    private String nickname;
    private String image;
    private LocalDateTime time; // 关注时间
}
