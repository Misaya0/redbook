package com.itcast.service;

import com.itcast.model.vo.AttentionVo;
import com.itcast.result.Result;
import com.itcast.model.pojo.Attention;

import java.util.List;

public interface AttentionService {
    Result<Integer> isAttention(Long otherId);
    Result<Void> attention(Long otherId);
    Result<List<AttentionVo>> getAttention(Long userId);
    Result<List<AttentionVo>> getFans(Long userId);
}
