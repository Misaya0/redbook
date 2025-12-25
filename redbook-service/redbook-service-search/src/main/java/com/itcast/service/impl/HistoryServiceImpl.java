package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itcast.mapper.HistoryMapper;
import com.itcast.result.Result;
import com.itcast.model.pojo.History;
import com.itcast.service.HistoryService;
import com.itcast.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryMapper historyMapper;

    @Override
    public Result<List<History>> getHistoryList() {
        Long userId = UserContext.getUserId();
        // 仅查询最近 10 条搜索历史：按 id 倒序，保证返回最新记录
        QueryWrapper<History> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("id")
                .last("limit 10");
        List<History> historyList = historyMapper.selectList(queryWrapper);
        return Result.success(historyList);
    }

    @Override
    public Result<Void> deleteHistory() {
        Long userId = UserContext.getUserId();
        QueryWrapper<History> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        historyMapper.delete(queryWrapper);
        return Result.success(null);
    }
}
