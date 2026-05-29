package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.UserSkill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {
    @Select("SELECT * FROM t_user_skill WHERE user_id = #{userId}")
    List<UserSkill> findByUserId(@Param("userId") Long userId);
}
