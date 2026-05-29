package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_kyc_record")
public class KycRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String realName;
    private String idNumber;
    private String creditCode;
    private String idFrontUrl;
    private String idBackUrl;
    private String licenseUrl;
    private String status;
    private String remark;
    private Long auditorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
