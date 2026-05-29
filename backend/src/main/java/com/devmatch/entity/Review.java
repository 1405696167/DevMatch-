package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "t_review", autoResultMap = true)
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer rating;
    private String content;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
