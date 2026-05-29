package com.devmatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    private String username;
    private String password;
    private String smsCode;
    private String loginType; // PASSWORD / SMS
}
