package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String nickname;
    private String phone;
    private String email;

    @JsonIgnore
    private String passwordHash;

    private String role;
    private String avatar;
    private String bio;
    private String city;
    private String homepage;
    private String companyName;
    private Integer creditScore;
    private String kycStatus;
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
