package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_wallet")
public class Wallet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozen;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 本月收入流水合计（开发者，非表字段） */
    @TableField(exist = false)
    private BigDecimal monthlyIncome;

    /** 本月支出流水合计（企业，非表字段） */
    @TableField(exist = false)
    private BigDecimal monthlyExpense;
}
