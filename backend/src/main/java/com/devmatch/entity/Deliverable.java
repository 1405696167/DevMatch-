package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_deliverable")
public class Deliverable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long milestoneId;
    private String name;
    private String path;
    private Long size;
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
