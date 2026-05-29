package com.devmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MilestoneRequest {
    @NotBlank(message = "里程碑名称不能为空")
    private String name;
    private String description;
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
    private LocalDate deadline;
    private Integer sortOrder;
}
