package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.dto.LoginRequest;
import com.devmatch.dto.RegisterRequest;
import com.devmatch.entity.User;
import com.devmatch.service.AuthService;
import com.devmatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody @Valid RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    // 前端调用: POST /auth/sms/send  body: { phone }
    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public R<Void> sendSms(@RequestBody Map<String, String> body) {
        authService.sendSmsCode(body.get("phone"));
        return R.ok();
    }

    // 兼容旧路径: POST /auth/sms?phone=xxx
    @PostMapping("/sms")
    public R<Void> sendSmsParam(@RequestParam(required = false) String phone,
                                @RequestBody(required = false) Map<String, String> body) {
        String p = phone != null ? phone : (body != null ? body.get("phone") : null);
        if (p == null) return R.fail("手机号不能为空");
        authService.sendSmsCode(p);
        return R.ok();
    }

    // 前端调用: POST /auth/email/send  body: { email }
    @Operation(summary = "发送邮箱验证码（预留）")
    @PostMapping("/email/send")
    public R<Void> sendEmail(@RequestBody Map<String, String> body) {
        // 预留：开发模式下直接返回成功，验证码固定 123456
        return R.ok();
    }

    // 前端调用: POST /auth/refresh  body: { refreshToken } 或 query param
    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(
            @RequestParam(required = false) String refreshToken,
            @RequestBody(required = false) Map<String, String> body) {
        String token = refreshToken != null ? refreshToken : (body != null ? body.get("refreshToken") : null);
        return R.ok(authService.refreshToken(token));
    }

    // 前端调用: POST /auth/password/reset  body: { phone, code, newPassword }
    @Operation(summary = "重置密码")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("phone"), body.get("code"), body.get("newPassword"));
        return R.ok();
    }

    // 兼容旧路径
    @PostMapping("/reset-password")
    public R<Void> resetPasswordOld(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("phone"), body.get("code"), body.get("newPassword"));
        return R.ok();
    }

    // 前端调用: PUT /auth/password/change
    @Operation(summary = "修改密码（已登录）")
    @PutMapping("/password/change")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        // JWT 无状态，客户端清除 token 即可，服务端直接返回成功
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<User> me() {
        return R.ok(userService.getProfile());
    }
}
