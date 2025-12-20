package com.itcast.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("rb_spec_option")
public class SpecOption implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("spec_id")
    private Long specId;

    @TableField("option_value")
    private String optionValue;

    @TableField("option_label")
    private String optionLabel;

    @TableField("image")
    private String image;

    @TableField("sort")
    private Integer sort;
}

