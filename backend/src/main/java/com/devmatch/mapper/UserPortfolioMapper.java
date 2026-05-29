package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.UserPortfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPortfolioMapper extends BaseMapper<UserPortfolio> {
    @Select("SELECT * FROM t_user_portfolio WHERE user_id = #{userId} ORDER BY created_at")
    List<UserPortfolio> findByUserId(@Param("userId") Long userId);
}
