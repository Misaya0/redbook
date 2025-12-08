package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
