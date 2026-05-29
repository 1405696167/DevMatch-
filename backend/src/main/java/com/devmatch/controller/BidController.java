package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 前端调用: GET /bids/my?status=
 */
@Tag(name = "投标接口")
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final TaskService taskService;

    @Operation(summary = "我的投标列表")
    @GetMapping("/my")
    @PreAuthorize("hasRole('DEVELOPER')")
    public R<Map<String, Object>> myBids(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(taskService.getMyBids(status, page, size));
    }
}
