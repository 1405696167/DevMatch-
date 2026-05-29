package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {

    @Select("SELECT * FROM t_wallet_transaction WHERE user_id = #{userId} AND (#{type} IS NULL OR #{type} = '' OR type = #{type}) ORDER BY created_at DESC")
    IPage<WalletTransaction> findByUserId(Page<WalletTransaction> page, @Param("userId") Long userId, @Param("type") String type);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_wallet_transaction WHERE user_id = #{userId} AND type = 'INCOME' AND MONTH(created_at) = MONTH(NOW()) AND YEAR(created_at) = YEAR(NOW())")
    BigDecimal getMonthlyIncome(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(ABS(amount)), 0) FROM t_wallet_transaction WHERE user_id = #{userId} AND type = 'EXPENSE' AND MONTH(created_at) = MONTH(NOW()) AND YEAR(created_at) = YEAR(NOW())")
    BigDecimal getMonthlyExpense(@Param("userId") Long userId);
}
