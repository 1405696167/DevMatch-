package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE id = #{id} AND deleted = 0")
    User selectByIdNotDeleted(@Param("id") Long id);

    @Select("""
        SELECT u.* FROM t_user u
        WHERE u.deleted = 0 AND u.role = 'DEVELOPER' AND u.status = 'ACTIVE'
        AND (#{keyword} IS NULL OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.bio LIKE CONCAT('%', #{keyword}, '%'))
        AND (#{skill} IS NULL OR JSON_CONTAINS(
            (SELECT JSON_ARRAYAGG(s.name) FROM t_user_skill s WHERE s.user_id = u.id),
            JSON_QUOTE(#{skill})))
        ORDER BY u.credit_score DESC
        """)
    IPage<User> searchDevelopers(Page<User> page, @Param("keyword") String keyword, @Param("skill") String skill);

    @Select("""
        SELECT u.* FROM t_user u
        WHERE u.deleted = 0
        AND (#{keyword} IS NULL OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
        AND (#{role} IS NULL OR #{role} = '' OR u.role = #{role})
        AND (#{status} IS NULL OR #{status} = '' OR u.status = #{status})
        """)
    IPage<User> searchAdmin(Page<User> page, @Param("keyword") String keyword,
                            @Param("role") String role, @Param("status") String status);
}
