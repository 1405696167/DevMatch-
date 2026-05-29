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

@Tag(name = "消息接口")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final NotificationService notificationService;

    @Operation(summary = "通知列表")
    @GetMapping("/notifications")
    public R<List<Notification>> notifications(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(notificationService.list(SecurityUtil.getCurrentUserId(), limit));
    }

    @Operation(summary = "标记单条通知已读")
    @PutMapping("/notifications/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @PostMapping("/notifications/{id}/read")
    public R<Void> markReadPost(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @Operation(summary = "全部通知已读")
    @PutMapping("/notifications/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtil.getCurrentUserId());
        return R.ok();
    }

    @PostMapping("/notifications/read-all")
    public R<Void> markAllReadPost() {
        notificationService.markAllRead(SecurityUtil.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/notifications/unread-count")
    public R<Integer> unreadCount() {
        return R.ok(notificationService.unreadCount(SecurityUtil.getCurrentUserId()));
    }
}
