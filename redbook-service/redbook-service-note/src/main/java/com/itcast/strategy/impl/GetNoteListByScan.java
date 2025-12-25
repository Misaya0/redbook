package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.client.UserClient;
import com.itcast.constant.ExceptionConstant;
import com.itcast.exception.NoteNoExistException;
import com.itcast.mapper.NoteMapper;
import com.itcast.mapper.NoteScanMapper;
import com.itcast.model.pojo.Note;
import com.itcast.model.pojo.NoteBrowse;
import com.itcast.model.vo.NoteVo;
import com.itcast.result.Result;
import com.itcast.strategy.GetNotesStrategy;
import com.itcast.strategy.NoteStrategyContext;
import com.itcast.strategy.NoteStrategyType;
import com.itcast.context.UserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetNoteListByScan implements GetNotesStrategy {

    @Autowired
    private NoteScanMapper noteScanMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserClient userClient;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.BY_SCAN;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        // 1.获取登录用户
        Long userId = UserContext.getUserId();
        // 2.获取数据库记录
        LambdaQueryWrapper<NoteBrowse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteBrowse::getUserId, userId);
        List<NoteBrowse> noteBrowseList = noteScanMapper.selectList(queryWrapper);
        if (noteBrowseList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 5.设置vo列表
        List<NoteVo> noteVoList = noteBrowseList.stream().map(noteBrowse -> {
            Note note = noteMapper.selectById(noteBrowse.getNoteId());
            if (note == null) {
                throw new NoteNoExistException(ExceptionConstant.NOTE_NO_EXIST);
            }
            NoteVo noteVo = new NoteVo();
            BeanUtils.copyProperties(note, noteVo);
            noteVo.setUser(userClient.getUserById(note.getUserId()).getData());
            return noteVo;
        }).collect(Collectors.toList());
        return Result.success(noteVoList);
    }
}
