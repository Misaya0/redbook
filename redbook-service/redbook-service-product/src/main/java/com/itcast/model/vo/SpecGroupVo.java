package com.itcast.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpecGroupVo {
    private String key;
    private String title;
    private String displayType;
    private List<SpecOptionVo> options;
}
