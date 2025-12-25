package com.itcast.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteSimpleVo {
    private Long id;
    private String title;
    private String image;
    private Integer like;
    private Long userId;
}
