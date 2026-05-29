package com.devmatch.service;

import com.devmatch.entity.Notification;
import com.devmatch.mapper.NotificationMapper;
import com.devmatch.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final WebSocketServer webSocketServer;

    @Async("asyncExecutor")
    public void send(Long userId, String type, String content, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setLink(link);
        notification.setIsRead(false);
        notificationMapper.insert(notification);

        // 推送WebSocket实时通知
        webSocketServer.sendToUser(userId, Map.of(
                "type", "NOTIFICATION",
                "data", Map.of(
                        "id", notification.getId(),
                        "notifyType", type,
                        "content", content,
                        "link", link != null ? link : ""
                )
        ));
    }

    public List<Notification> list(Long userId, int limit) {
        return notificationMapper.findByUserId(userId, limit);
    }

    public int unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null) {
            n.setIsRead(true);
            notificationMapper.updateById(n);
        }
    }
}
