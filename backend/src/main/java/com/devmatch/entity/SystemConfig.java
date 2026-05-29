package com.devmatch.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_system_config")
public class SystemConfig {
    @TableId
    private Integer id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
