package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.UserClient;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Note;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.strategy.GetNotesStrategy;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetNoteListByUserId implements GetNotesStrategy {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserClient userClient;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.BY_USER_ID;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        Long userId = context.getUserId();
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }
        
        // 分页
        Integer page = context.getPage();
        Integer pageSize = context.getPageSize();
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 10;

        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getUserId, userId);
        // 按时间倒序，假设 Note 有 time 字段且是 String 格式，可能需要特殊处理，但这里先尝试直接排序
        // Note.java 中 time 可能是 String，如果格式是 yyyy-MM-dd HH:mm:ss 也是可以排序的
        queryWrapper.orderByDesc(Note::getTime);
        
        Page<Note> pageParam = new Page<>(page, pageSize);
        Page<Note> notePage = noteMapper.selectPage(pageParam, queryWrapper);
        
        List<Note> noteList = notePage.getRecords();
        if (noteList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<NoteVo> noteVoList = noteList.stream().map(note -> {
            NoteVo noteVo = new NoteVo();
            BeanUtils.copyProperties(note, noteVo);
            noteVo.setUser(userClient.getUserById(note.getUserId()).getData());
            return noteVo;
        }).collect(Collectors.toList());
        return Result.success(noteVoList);
    }
}
