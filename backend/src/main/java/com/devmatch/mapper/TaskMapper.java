package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Update("UPDATE t_task SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    @Update("UPDATE t_task SET bid_count = bid_count + #{delta} WHERE id = #{id}")
    void updateBidCount(@Param("id") Long id, @Param("delta") int delta);

    @Select("""
        SELECT * FROM t_task
        WHERE deleted = 0
        AND (#{status} IS NULL OR #{status} = '' OR status = #{status})
        AND (#{keyword} IS NULL OR #{keyword} = '' OR MATCH(title, description) AGAINST(#{keyword} IN BOOLEAN MODE) OR title LIKE CONCAT('%', #{keyword}, '%'))
        AND (#{companyId} IS NULL OR company_id = #{companyId})
        AND (#{budgetMin} IS NULL OR budget_max >= #{budgetMin})
        AND (#{budgetMax} IS NULL OR budget_min <= #{budgetMax})
        ORDER BY
          CASE #{sort}
            WHEN 'BUDGET_DESC' THEN budget_max
            ELSE NULL
          END DESC,
          CASE #{sort}
            WHEN 'BUDGET_ASC' THEN budget_min
            ELSE NULL
          END ASC,
          CASE #{sort}
            WHEN 'DEADLINE' THEN UNIX_TIMESTAMP(deadline)
            ELSE NULL
          END ASC,
          created_at DESC
        """)
    IPage<Task> searchTasks(Page<Task> page,
                            @Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("companyId") Long companyId,
                            @Param("budgetMin") Double budgetMin,
                            @Param("budgetMax") Double budgetMax,
                            @Param("sort") String sort);

}
