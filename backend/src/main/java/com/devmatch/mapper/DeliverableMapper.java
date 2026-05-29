package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Deliverable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeliverableMapper extends BaseMapper<Deliverable> {
    @Select("SELECT * FROM t_deliverable WHERE milestone_id = #{milestoneId} ORDER BY created_at")
    List<Deliverable> findByMilestoneId(@Param("milestoneId") Long milestoneId);
}
