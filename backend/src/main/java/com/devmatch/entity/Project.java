package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long bidId;
    private String name;
    private Long developerId;
    private Long enterpriseId;
    private BigDecimal amount;
    /** MILESTONE 分阶段验收；ONCE 一次性整单验收 */
    private String paymentType;
    /** DEVELOPER / ENTERPRISE，与立项时任务一致 */
    private String milestonePlanBy;
    private String status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate endDate;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
