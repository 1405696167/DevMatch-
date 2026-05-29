package com.devmatch.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端 - 管理在线用户连接与站内通知推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketServer {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, ChatWebSocketHandler.WsSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(Long userId, ChatWebSocketHandler.WsSession session) {
        sessions.put(userId, session);
        log.info("用户 {} 上线，当前在线: {}", userId, sessions.size());
    }

    public void removeSession(Long userId) {
        sessions.remove(userId);
        log.info("用户 {} 下线，当前在线: {}", userId, sessions.size());
    }

    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId);
    }

    public void sendToUser(Long userId, Object message) {
        ChatWebSocketHandler.WsSession session = sessions.get(userId);
        if (session != null) {
            try {
                session.sendMessage(objectMapper.writeValueAsString(message));
            } catch (Exception e) {
                log.warn("发送消息给用户 {} 失败: {}", userId, e.getMessage());
                sessions.remove(userId);
            }
        }
    }

    public void broadcast(Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化失败", e);
            return;
        }
        sessions.forEach((userId, session) -> {
            try {
                session.sendMessage(json);
            } catch (Exception e) {
                sessions.remove(userId);
            }
        });
    }
}
