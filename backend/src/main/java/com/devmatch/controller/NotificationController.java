package com.devmatch.controller;

import com.devmatch.common.R;
import com.devmatch.entity.Notification;
import com.devmatch.security.SecurityUtil;
import com.devmatch.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端调用路径: /notifications (不含 /messages 前缀)
 */
@Tag(name = "通知接口")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "通知列表")
    @GetMapping
    public R<List<Notification>> list(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(notificationService.list(SecurityUtil.getCurrentUserId(), limit));
    }

    // 前端调用: PUT /notifications/{id}/read
    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    // 前端调用: PUT /notifications/read-all
    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtil.getCurrentUserId());
        return R.ok();
    }
}
