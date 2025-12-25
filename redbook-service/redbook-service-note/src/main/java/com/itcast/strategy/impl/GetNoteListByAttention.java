package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcast.client.UserClient;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.vo.AttentionVo;
import com.itcast.model.pojo.Note;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetNoteListByAttention implements GetNotesStrategy {

    @Autowired
    private UserClient userClient;

    @Autowired
    private NoteMapper noteMapper;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.BY_ATTENTION;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        // 1.获取登录用户id
        Long userId = UserContext.getUserId();
        // 2.获取该用户关注的人
        List<AttentionVo> attentions = userClient.getAttention(userId).getData();
        // 3.获取该用户关注的人的笔记
        List<NoteVo> resultList = new ArrayList<>();
        for (AttentionVo attention : attentions) {
            LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Note::getUserId, attention.getUserId());
            List<Note> noteList = noteMapper.selectList(queryWrapper);
            if (noteList.isEmpty()) {
                continue;
            }
            for (Note note : noteList) {
                NoteVo noteVo = new NoteVo();
                BeanUtils.copyProperties(note, noteVo);
                noteVo.setUser(userClient.getUserById(note.getUserId()).getData());
                resultList.add(noteVo);
            }
        }
        // 4.按照事件排序
        List<NoteVo> sortList
                = resultList.stream().sorted(Comparator.comparing(NoteVo::getTime)).collect(Collectors.toList());
        return Result.success(sortList);
    }
}
