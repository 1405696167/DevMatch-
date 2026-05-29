package com.devmatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.entity.KycRecord;
import com.devmatch.entity.User;
import com.devmatch.mapper.UserMapper;
import com.devmatch.service.ReviewService;
import com.devmatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    // ==================== 个人资料 ====================

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/profile")
    public R<User> getMyProfile() {
        return R.ok(userService.getProfile());
    }

    // 前端调用: GET /users/{userId}/profile
    @Operation(summary = "获取指定用户资料")
    @GetMapping("/{userId}/profile")
    public R<User> getProfileById(@PathVariable Long userId) {
        return R.ok(userService.getById(userId));
    }

    @Operation(summary = "更新用户资料")
    @PutMapping("/profile")
    public R<User> updateProfile(@RequestBody Map<String, Object> updates) {
        return R.ok(userService.updateProfile(updates));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam MultipartFile file) throws IOException {
        return R.ok(userService.uploadAvatar(file));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    // ==================== KYC 认证 ====================

    @Operation(summary = "提交开发者实名认证")
    @PostMapping("/kyc")
    public R<Void> submitKyc(
            @RequestParam String realName,
            @RequestParam String idNumber,
            @RequestParam MultipartFile idFrontFile,
            @RequestParam MultipartFile idBackFile) throws IOException {
        userService.submitDeveloperKyc(realName, idNumber, idFrontFile, idBackFile);
        return R.ok();
    }

    // 前端调用: POST /enterprise/kyc（映射到此处处理）
    @Operation(summary = "获取KYC状态")
    @GetMapping("/kyc/status")
    public R<KycRecord> getKycStatus() {
        return R.ok(userService.getKycStatus());
    }

    // ==================== 简历/技能/作品集 ====================

    @Operation(summary = "获取简历（技能+作品集）")
    @GetMapping("/resume")
    public R<Map<String, Object>> getResume() {
        return R.ok(userService.getResume());
    }

    @Operation(summary = "更新简历")
    @PutMapping("/resume")
    public R<Void> updateResume(@RequestBody Map<String, Object> data) {
        userService.updateResume(data);
        return R.ok();
    }

    @Operation(summary = "添加技能")
    @PostMapping("/skills")
    public R<Void> addSkill(@RequestBody Map<String, Object> data) {
        userService.addSkill(data);
        return R.ok();
    }

    @Operation(summary = "更新技能（名称、熟练度）")
    @PutMapping("/skills/{skillId}")
    public R<Void> updateSkill(@PathVariable Long skillId, @RequestBody Map<String, Object> data) {
        userService.updateSkill(skillId, data);
        return R.ok();
    }

    @Operation(summary = "删除技能")
    @DeleteMapping("/skills/{skillId}")
    public R<Void> deleteSkill(@PathVariable Long skillId) {
        userService.deleteSkill(skillId);
        return R.ok();
    }

    @Operation(summary = "添加作品集")
    @PostMapping("/projects")
    public R<Void> addPortfolio(@RequestBody Map<String, Object> data) {
        userService.addPortfolio(data);
        return R.ok();
    }

    @Operation(summary = "更新作品集")
    @PutMapping("/projects/{id}")
    public R<Void> updatePortfolio(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        userService.updatePortfolio(id, data);
        return R.ok();
    }

    @Operation(summary = "删除作品集")
    @DeleteMapping("/projects/{id}")
    public R<Void> deletePortfolio(@PathVariable Long id) {
        userService.deletePortfolio(id);
        return R.ok();
    }

    // ==================== 信用 ====================

    // 前端调用: GET /users/{userId}/credit
    @Operation(summary = "获取用户信用档案")
    @GetMapping("/{userId}/credit")
    public R<Map<String, Object>> getCreditInfo(@PathVariable Long userId) {
        return R.ok(reviewService.getCreditInfo(userId));
    }

    // ==================== 管理员 KYC ====================

    @Operation(summary = "KYC列表（管理员）")
    @GetMapping("/admin/kyc")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> listKyc(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return R.ok(userService.listKycWithUser(page, size, status));
    }

    @Operation(summary = "审核KYC（管理员）")
    @PostMapping("/admin/kyc/{kycId}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> auditKycAdmin(@PathVariable Long kycId, @RequestBody Map<String, String> body) {
        userService.auditKyc(kycId, body.get("action"), body.get("remark"));
        return R.ok();
    }

    // ==================== 搜索开发者 ====================

    // 前端调用: GET /users/developers/search?keyword=&skill=
    @Operation(summary = "搜索开发者")
    @GetMapping("/developers/search")
    public R<Map<String, Object>> searchDevelopers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill) {
        return R.ok(userService.searchDevelopers(page, size, keyword, skill));
    }

    // 兼容旧路径: GET /users/developers
    @GetMapping("/developers")
    public R<Map<String, Object>> searchDevelopersOld(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill) {
        return R.ok(userService.searchDevelopers(page, size, keyword, skill));
    }

    @Operation(summary = "获取开发者详情")
    @GetMapping("/developers/{id}")
    public R<Map<String, Object>> getDeveloperProfile(@PathVariable Long id) {
        return R.ok(userService.getDeveloperProfile(id));
    }
}
