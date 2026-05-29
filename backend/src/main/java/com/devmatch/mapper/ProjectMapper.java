package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("SELECT * FROM t_project WHERE developer_id = #{userId} AND deleted = 0 AND (#{status} IS NULL OR #{status} = '' OR status = #{status}) ORDER BY created_at DESC")
    List<Project> findByDeveloperId(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT * FROM t_project WHERE enterprise_id = #{userId} AND deleted = 0 AND (#{status} IS NULL OR #{status} = '' OR status = #{status}) ORDER BY created_at DESC")
    List<Project> findByEnterpriseId(@Param("userId") Long userId, @Param("status") String status);

    @Update("UPDATE t_project SET progress = #{progress} WHERE id = #{id}")
    void updateProgress(@Param("id") Long id, @Param("progress") int progress);
}
