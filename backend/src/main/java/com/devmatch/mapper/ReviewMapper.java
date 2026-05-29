package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    @Select("SELECT * FROM t_review WHERE reviewee_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Review> findReceivedReviews(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(AVG(rating), 5.0) FROM t_review WHERE reviewee_id = #{userId}")
    BigDecimal getAvgRating(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_review WHERE reviewee_id = #{userId} AND rating >= 4")
    int countGoodReviews(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_review WHERE project_id = #{projectId} AND reviewer_id = #{reviewerId}")
    int countByProjectAndReviewer(@Param("projectId") Long projectId, @Param("reviewerId") Long reviewerId);
}
