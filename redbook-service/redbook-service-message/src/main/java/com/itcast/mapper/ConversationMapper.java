package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
