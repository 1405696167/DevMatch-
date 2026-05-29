package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Bid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BidMapper extends BaseMapper<Bid> {

    @Select("SELECT * FROM t_bid WHERE task_id = #{taskId} AND status != 'CANCELLED' ORDER BY created_at DESC")
    List<Bid> findByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM t_bid WHERE developer_id = #{developerId} AND (#{status} IS NULL OR #{status} = '' OR status = #{status}) ORDER BY created_at DESC")
    List<Bid> findByDeveloperId(@Param("developerId") Long developerId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM t_bid WHERE task_id = #{taskId} AND developer_id = #{developerId} AND status NOT IN ('CANCELLED','REJECTED')")
    int countActiveBid(@Param("taskId") Long taskId, @Param("developerId") Long developerId);
}
