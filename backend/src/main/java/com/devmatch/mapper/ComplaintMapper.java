package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
    @Select("SELECT * FROM t_complaint WHERE (#{status} IS NULL OR #{status} = '' OR status = #{status}) ORDER BY created_at DESC")
    IPage<Complaint> findAll(Page<Complaint> page, @Param("status") String status);
}
