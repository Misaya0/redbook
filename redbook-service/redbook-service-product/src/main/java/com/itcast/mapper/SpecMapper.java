package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.model.pojo.Spec;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpecMapper extends BaseMapper<Spec> {
}

