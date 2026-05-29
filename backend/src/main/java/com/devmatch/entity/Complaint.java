package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_complaint")
public class Complaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long complainantId;
    private Long respondentId;
    private String title;
    private String content;
    private String status;
    private String result;
    private String remark;
    private Long handlerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
