package com.itcast.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.client.UserClient;
import com.itcast.mapper.CollectionMapper;
import com.itcast.mapper.NoteMapper;
import com.itcast.model.pojo.Collection;
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
public class GetNoteListByCollectionUserId implements GetNotesStrategy {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CollectionMapper collectionMapper;

    @Override
    public NoteStrategyType getStrategyType() {
        return NoteStrategyType.BY_COLLECTION_USER_ID;
    }

    @Override
    public Result<List<NoteVo>> getNotes(NoteStrategyContext context) {
        Long userId = context.getUserId();
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }

        Integer page = context.getPage();
        Integer pageSize = context.getPageSize();
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 10;

        // 1.获取用户收藏
        LambdaQueryWrapper<Collection> queryWrapper
                = new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId);
        
        Page<Collection> pageParam = new Page<>(page, pageSize);
        Page<Collection> collectionPage = collectionMapper.selectPage(pageParam, queryWrapper);
        
        List<Collection> collectionList = collectionPage.getRecords();
        
        if (collectionList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 2.设置vo
        List<NoteVo> noteVoList = collectionList.stream().map(collection -> {
            Note note = noteMapper.selectById(collection.getNoteId());
            if (note == null) {
                return null;
            }
            NoteVo noteVo = new NoteVo();
            BeanUtils.copyProperties(note, noteVo);
            noteVo.setUser(userClient.getUserById(note.getUserId()).getData());
            return noteVo;
        }).filter(noteVo -> noteVo != null).collect(Collectors.toList());
        
        return Result.success(noteVoList);
    }
}
