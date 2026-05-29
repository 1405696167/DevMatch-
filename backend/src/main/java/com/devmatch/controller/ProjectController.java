package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.dto.MilestoneRequest;
import com.devmatch.entity.Deliverable;
import com.devmatch.entity.Milestone;
import com.devmatch.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "项目接口")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "我的项目列表")
    @GetMapping
    public R<Map<String, Object>> myProjects(@RequestParam(required = false) String status) {
        List<Map<String, Object>> list = projectService.myProjects(status);
        return R.ok(Map.of("list", list, "total", list.size()));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(projectService.detail(id));
    }

    @Operation(summary = "里程碑列表")
    @GetMapping("/{id}/milestones")
    public R<List<Milestone>> milestones(@PathVariable Long id) {
        return R.ok(projectService.getMilestones(id));
    }

    @Operation(summary = "创建里程碑")
    @PostMapping("/{id}/milestones")
    public R<Milestone> createMilestone(@PathVariable Long id, @RequestBody @Valid MilestoneRequest req) {
        return R.ok(projectService.createMilestone(id, req));
    }

    @Operation(summary = "更新里程碑")
    @PutMapping("/milestones/{milestoneId}")
    public R<Milestone> updateMilestone(@PathVariable Long milestoneId, @RequestBody @Valid MilestoneRequest req) {
        return R.ok(projectService.updateMilestone(milestoneId, req));
    }

    @Operation(summary = "删除里程碑")
    @DeleteMapping("/milestones/{milestoneId}")
    public R<Void> deleteMilestone(@PathVariable Long milestoneId) {
        projectService.deleteMilestone(milestoneId);
        return R.ok();
    }

    @Operation(summary = "开始里程碑（待开始→进行中）")
    @PostMapping("/milestones/{milestoneId}/start")
    public R<Void> startMilestone(@PathVariable Long milestoneId) {
        projectService.startMilestone(milestoneId);
        return R.ok();
    }

    @Operation(summary = "提交里程碑验收")
    @PostMapping("/milestones/{milestoneId}/submit")
    public R<Void> submitMilestone(@PathVariable Long milestoneId) {
        projectService.submitMilestone(milestoneId);
        return R.ok();
    }

    @Operation(summary = "验收通过里程碑")
    @PostMapping("/milestones/{milestoneId}/accept")
    public R<Void> acceptMilestone(@PathVariable Long milestoneId) {
        projectService.acceptMilestone(milestoneId);
        return R.ok();
    }

    @Operation(summary = "驳回里程碑")
    @PostMapping("/milestones/{milestoneId}/reject")
    public R<Void> rejectMilestone(@PathVariable Long milestoneId, @RequestBody Map<String, String> body) {
        projectService.rejectMilestone(milestoneId, body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "上传交付物")
    @PostMapping("/milestones/{milestoneId}/deliverables")
    public R<Deliverable> uploadDeliverable(@PathVariable Long milestoneId,
                                            @RequestParam MultipartFile file) throws IOException {
        return R.ok(projectService.uploadDeliverable(milestoneId, file));
    }

    @Operation(summary = "获取交付物列表")
    @GetMapping("/milestones/{milestoneId}/deliverables")
    public R<List<Deliverable>> getDeliverables(@PathVariable Long milestoneId) {
        return R.ok(projectService.getDeliverables(milestoneId));
    }

    @Operation(summary = "下载交付物（需登录，项目双方或管理员）")
    @GetMapping("/deliverables/{deliverableId}/download")
    public ResponseEntity<Resource> downloadDeliverable(@PathVariable Long deliverableId) throws IOException {
        ProjectService.DeliverableFile df = projectService.resolveDeliverableDownload(deliverableId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(df.downloadFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(df.contentLength())
                .body(df.resource());
    }
}
