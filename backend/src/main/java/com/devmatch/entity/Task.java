package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "t_task", autoResultMap = true)
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private Long companyId;
    private String category;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> skills;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<Map<String, String>> attachments;

    private String experience;
    private Boolean requireKyc;
    private String contractType;
    private String paymentType;
    /** DEVELOPER=开发者拆分里程碑；ENTERPRISE=企业统一规划（仅分阶段付款有效） */
    private String milestonePlanBy;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate deadline;
    private Integer durationDays;
    private String status;
    private Integer bidCount;
    private Integer viewCount;
    private String rejectReason;

    /** 当前需求已扣除的发布押金（元），选标/关闭招募/审核驳回后清零并退款 */
    private BigDecimal publishDepositAmount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 发布方摘要（列表/详情展示，非表字段） */
    @TableField(exist = false)
    private Map<String, Object> company;

    /** 是否已不可投标（详情页用，非表字段） */
    @TableField(exist = false)
    private Boolean biddingClosed;
}
