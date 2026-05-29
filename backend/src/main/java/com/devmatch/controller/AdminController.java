package com.devmatch.controller;

import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.entity.Announcement;
import com.devmatch.entity.Complaint;
import com.devmatch.entity.SystemConfig;
import com.devmatch.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台管理接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "仪表盘数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(adminService.getDashboard());
    }

    @Operation(summary = "用户增长图表")
    @GetMapping("/charts/user-growth")
    public R<List<Map<String, Object>>> userGrowthChart() {
        return R.ok(adminService.getUserGrowthChart());
    }

    @Operation(summary = "交易量图表")
    @GetMapping("/charts/transactions")
    public R<List<Map<String, Object>>> transactionChart() {
        return R.ok(adminService.getTransactionChart());
    }

    @Operation(summary = "申诉列表")
    @GetMapping("/complaints")
    public R<PageResult<Complaint>> complaints(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return R.ok(adminService.listComplaints(page, size, status));
    }

    @Operation(summary = "处理申诉")
    @PostMapping("/complaints/{id}/handle")
    public R<Void> handleComplaint(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.handleComplaint(id, body.get("result"), body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "系统配置列表")
    @GetMapping("/configs")
    public R<List<SystemConfig>> configs() {
        return R.ok(adminService.listConfigs());
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/configs/{key}")
    public R<Void> updateConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        adminService.updateConfig(key, body.get("value"));
        return R.ok();
    }

    @Operation(summary = "公告列表")
    @GetMapping("/announcements")
    public R<List<Announcement>> announcements() {
        return R.ok(adminService.listAnnouncements());
    }

    @Operation(summary = "创建公告")
    @PostMapping("/announcements")
    public R<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        return R.ok(adminService.createAnnouncement(announcement));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/announcements/{id}")
    public R<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement data) {
        return R.ok(adminService.updateAnnouncement(id, data));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/announcements/{id}")
    public R<Void> deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
        return R.ok();
    }
}
