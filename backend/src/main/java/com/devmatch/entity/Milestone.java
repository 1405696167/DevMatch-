package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("t_milestone")
public class Milestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDate deadline;
    private String status;
    private Integer sortOrder;
    private String rejectReason;
    private LocalDateTime acceptedAt;

    @TableField(exist = false)
    private List<Deliverable> deliverables;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
