package com.devmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;
    private String category;
    private List<String> skills;
    private String experience;
    private Boolean requireKyc;
    private String contractType;
    private String paymentType;
    /** DEVELOPER | ENTERPRISE，一次性付款时忽略 */
    private String milestonePlanBy;

    @NotNull(message = "预算下限不能为空")
    private BigDecimal budgetMin;

    @NotNull(message = "预算上限不能为空")
    private BigDecimal budgetMax;

    private LocalDate deadline;
    private Integer durationDays;
    private String action; // DRAFT / PUBLISH
    private List<Map<String, String>> attachments;
}
