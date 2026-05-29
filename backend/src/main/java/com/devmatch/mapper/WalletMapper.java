package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {
    @Select("SELECT * FROM t_wallet WHERE user_id = #{userId}")
    Wallet findByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_wallet SET balance = balance + #{amount}, total_income = total_income + #{amount} WHERE user_id = #{userId} AND balance + #{amount} >= 0")
    int addBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE t_wallet SET balance = balance - #{amount}, frozen = frozen + #{amount} WHERE user_id = #{userId} AND balance >= #{amount}")
    int freezeAmount(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE t_wallet SET frozen = frozen - #{amount}, total_expense = total_expense + #{amount} WHERE user_id = #{userId} AND frozen >= #{amount}")
    int deductFrozen(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE t_wallet SET frozen = frozen - #{amount}, balance = balance + #{amount} WHERE user_id = #{userId} AND frozen >= #{amount}")
    int unfreeze(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
}
