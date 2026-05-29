package com.devmatch.controller;

import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.entity.KycRecord;
import com.devmatch.entity.User;
import com.devmatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员-用户管理接口
 * 前端调用路径: /admin/users, /admin/users/{id}/status, /admin/kyc/{id}/audit
 */
@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    // 前端调用: GET /admin/users?keyword=&role=&status=&page=&size=
    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public R<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return R.ok(userService.adminSearch(page, size, keyword, role, status));
    }

    // 前端调用: PUT /admin/users/{userId}/status  body: { status }
    @Operation(summary = "启用/禁用用户")
    @PutMapping("/users/{userId}/status")
    public R<Void> toggleStatus(@PathVariable Long userId,
                                @RequestBody(required = false) Map<String, String> body) {
        userService.adminToggleStatus(userId);
        return R.ok();
    }

    // 兼容旧路径: POST /users/admin/{id}/toggle-status
    @PostMapping("/users/{userId}/toggle-status")
    public R<Void> toggleStatusPost(@PathVariable Long userId) {
        userService.adminToggleStatus(userId);
        return R.ok();
    }

    // 前端调用: POST /admin/kyc/{kycId}/audit
    @Operation(summary = "审核KYC")
    @PostMapping("/kyc/{kycId}/audit")
    public R<Void> auditKyc(@PathVariable Long kycId,
                            @RequestBody Map<String, String> body) {
        userService.auditKyc(kycId, body.get("action"), body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "待审核KYC列表")
    @GetMapping("/kyc/pending")
    public R<PageResult<KycRecord>> pendingKyc(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.getPendingKyc(page, size));
    }

    @Operation(summary = "KYC列表（支持状态筛选）")
    @GetMapping("/kyc")
    public R<PageResult<KycRecord>> listKyc(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return R.ok(userService.listKyc(page, size, status));
    }
}
