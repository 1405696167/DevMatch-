package com.devmatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devmatch.entity.KycRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KycRecordMapper extends BaseMapper<KycRecord> {
    @Select("SELECT * FROM t_kyc_record WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    KycRecord findLatestByUserId(@Param("userId") Long userId);
}
