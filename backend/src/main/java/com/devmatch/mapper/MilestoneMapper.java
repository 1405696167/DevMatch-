package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Milestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MilestoneMapper extends BaseMapper<Milestone> {
    @Select("SELECT * FROM t_milestone WHERE project_id = #{projectId} ORDER BY sort_order, created_at")
    List<Milestone> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM t_milestone WHERE project_id = #{projectId} AND status = 'ACCEPTED'")
    int countAccepted(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM t_milestone WHERE project_id = #{projectId}")
    int countTotal(@Param("projectId") Long projectId);
}
