package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devmatch.entity.WithdrawRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WithdrawRecordMapper extends BaseMapper<WithdrawRecord> {
    @Select("SELECT * FROM t_withdraw_record WHERE (#{status} IS NULL OR #{status} = '' OR status = #{status}) ORDER BY created_at DESC")
    IPage<WithdrawRecord> findAll(Page<WithdrawRecord> page, @Param("status") String status);
}
