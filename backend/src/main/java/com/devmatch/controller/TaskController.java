package com.devmatch.controller;

import com.devmatch.common.PageResult;
import com.devmatch.common.R;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.dto.BidRequest;
import com.devmatch.dto.TaskCreateRequest;
import com.devmatch.entity.Bid;
import com.devmatch.entity.Project;
import com.devmatch.entity.Task;
import com.devmatch.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "任务接口")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "任务列表")
    @GetMapping
    public R<PageResult<Task>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Double budgetMin,
            @RequestParam(required = false) Double budgetMax,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(taskService.list(page, size, keyword, status, companyId, budgetMin, budgetMax, sort, mine));
    }

    @Operation(summary = "发布押金预览（企业，按预算上限与系统配置比例）")
    @GetMapping("/publish-deposit-preview")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Map<String, Object>> publishDepositPreview(@RequestParam BigDecimal budgetMax) {
        if (budgetMax == null || budgetMax.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请提供大于 0 的预算上限");
        }
        return R.ok(taskService.publishDepositPreview(budgetMax));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public R<Task> detail(@PathVariable Long id) {
        return R.ok(taskService.detail(id));
    }

    @Operation(summary = "创建任务")
    @PostMapping
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Task> create(@RequestBody @Valid TaskCreateRequest req) {
        return R.ok(taskService.create(req));
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Task> update(@PathVariable Long id, @RequestBody @Valid TaskCreateRequest req) {
        return R.ok(taskService.update(id, req));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    @Operation(summary = "发布任务（提交审核）")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Void> publish(@PathVariable Long id) {
        taskService.publish(id);
        return R.ok();
    }

    @Operation(summary = "关闭招募")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Void> close(@PathVariable Long id) {
        taskService.close(id);
        return R.ok();
    }

    // ==================== 投标 ====================

    // 前端调用: POST /tasks/{taskId}/bids  body: { amount, days, proposal }
    @Operation(summary = "投标")
    @PostMapping("/{taskId}/bids")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Bid> bid(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        BidRequest req = new BidRequest();
        req.setTaskId(taskId);
        req.setAmount(new BigDecimal(body.get("amount").toString()));
        req.setDays(Integer.parseInt(body.get("days").toString()));
        req.setProposal((String) body.get("proposal"));
        return R.ok(taskService.submitBid(req));
    }

    // 兼容旧路径: POST /tasks/bid
    @PostMapping("/bid")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Bid> submitBidOld(@RequestBody @Valid BidRequest req) {
        return R.ok(taskService.submitBid(req));
    }

    // 前端调用: GET /tasks/{id}/bids
    @Operation(summary = "获取投标列表（企业）")
    @GetMapping("/{id}/bids")
    @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
    public R<List<Map<String, Object>>> getBids(@PathVariable Long id) {
        return R.ok(taskService.getBids(id));
    }

    // 前端调用: POST /tasks/{taskId}/bids/{bidId}/select
    @Operation(summary = "选标")
    @PostMapping("/{taskId}/bids/{bidId}/select")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Project> selectBid(@PathVariable Long taskId, @PathVariable Long bidId) {
        return R.ok(taskService.selectBid(bidId));
    }

    // 兼容旧路径: POST /tasks/bids/{bidId}/select
    @PostMapping("/bids/{bidId}/select")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Project> selectBidOld(@PathVariable Long bidId) {
        return R.ok(taskService.selectBid(bidId));
    }

    // 前端调用: DELETE /tasks/{taskId}/bids/{bidId}
    @Operation(summary = "撤回投标")
    @DeleteMapping("/{taskId}/bids/{bidId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Void> cancelBid(@PathVariable Long taskId, @PathVariable Long bidId) {
        taskService.cancelBid(bidId);
        return R.ok();
    }

    // 兼容旧路径: DELETE /tasks/bids/{bidId}
    @DeleteMapping("/bids/{bidId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Void> cancelBidOld(@PathVariable Long bidId) {
        taskService.cancelBid(bidId);
        return R.ok();
    }

    // 前端调用: GET /bids/my?status=
    // 独立映射到 BidController 中，此处保留兼容
    @Operation(summary = "我的投标")
    @GetMapping("/my-bids")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Map<String, Object>> myBids(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(taskService.getMyBids(status, page, size));
    }

    // ==================== 管理员 ====================

    // 前端调用: POST /tasks/{id}/audit
    @Operation(summary = "审核任务（管理员）")
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> auditTask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        taskService.auditTask(id, body.get("action"), body.get("reason"));
        return R.ok();
    }

    // 兼容旧路径: POST /tasks/admin/{id}/audit
    @PostMapping("/admin/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> auditTaskAdmin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        taskService.auditTask(id, body.get("action"), body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "待审核任务列表（管理员）")
    @GetMapping("/admin/auditing")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<Task>> auditingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(taskService.adminListAuditing(page, size));
    }

    @Operation(summary = "上传任务附件")
    @PostMapping("/upload-attachment")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public R<Map<String, String>> uploadAttachment(@RequestParam MultipartFile file) throws IOException {
        return R.ok(taskService.uploadAttachment(file));
    }
}
