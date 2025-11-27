package com.itcast.service;

import com.itcast.result.Result;

public interface LikeService {
    Result<Void> like(Long noteId);

    Result<Boolean> isLike(Long noteId);
}
