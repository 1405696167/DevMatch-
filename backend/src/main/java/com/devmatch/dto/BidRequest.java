package com.devmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BidRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "报价不能为空")
    private BigDecimal amount;

    @NotNull(message = "工期不能为空")
    private Integer days;

    @NotBlank(message = "投标说明不能为空")
    private String proposal;
}
