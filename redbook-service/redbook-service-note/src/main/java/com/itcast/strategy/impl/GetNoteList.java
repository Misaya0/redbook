package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.UserClient;
import com.itcast.constant.RedisConstant;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Note;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.strategy.GetNotesStrategy;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import com.itcast.context.UserContext;
import com.itcast.util.BloomFilterUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetNoteList implements GetNotesStrategy {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.DEFAULT;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        // 1.构造分页
        Page<Note> ipage = new Page<>(context.getPage(), context.getPageSize());
        
        // 2.构造查询条件
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        if (context.getType() != null && !context.getType().isEmpty()) {
            queryWrapper.eq(Note::getType, context.getType());
        }
        
        // 3.根据分页查询笔记
        Page<Note> notePage = noteMapper.selectPage(ipage, queryWrapper);
        if (notePage == null) {
            return Result.success(new ArrayList<>());
        }
        // 4.获取记录
        List<Note> noteList = notePage.getRecords();
        if (noteList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 4.布隆过滤器过滤(过滤掉用户已经看过的笔记)
        Integer userId = UserContext.getUserId();
        List<NoteVo> noteVoList = noteList.stream()
                .filter(note -> {
                    // 如果用户未登录，不过滤
                    if (userId == null) {
                        return true;
                    }
                    return !bloomFilterUtil.mightContain(RedisConstant.USER_BLOOM_FILTER + userId, note.getId().toString());
                })
                .map(note -> {
                    NoteVo noteVo = new NoteVo();
                    BeanUtils.copyProperties(note, noteVo);
                    
                    // 安全地获取用户信息
                    if (note.getUserId() != null) {
                        try {
                            Result<com.itcast.model.pojo.User> userResult = userClient.getUserById(note.getUserId());
                            if (userResult != null && userResult.getData() != null) {
                                noteVo.setUser(userResult.getData());
                            }
                        } catch (Exception e) {
                            // 记录日志但不中断流程
                            System.err.println("获取用户信息失败: " + e.getMessage());
                        }
                    }
                    
                    return noteVo;
                })
                .collect(Collectors.toList());
        return Result.success(noteVoList);
    }
}
