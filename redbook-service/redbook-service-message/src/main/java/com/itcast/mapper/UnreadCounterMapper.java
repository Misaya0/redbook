package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.entity.UnreadCounter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UnreadCounterMapper extends BaseMapper<UnreadCounter> {
}
