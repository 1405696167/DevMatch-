package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_skill")
public class UserSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private Integer level;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
