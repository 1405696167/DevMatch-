package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_bid")
public class Bid {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long developerId;
    private BigDecimal amount;
    private Integer days;
    private String proposal;
    private String status;
    private Long projectId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
