package com.devmatch.websocket;

import com.devmatch.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final WebSocketServer webSocketServer;
    private final ObjectMapper objectMapper;

    // sessionId -> userId
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    // sessionId -> WsSession
    private final ConcurrentHashMap<String, WsSession> wsSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || !jwtUtil.isValid(token)) {
            closeSession(session, "Token无效");
            return;
        }
        Long userId = jwtUtil.getUserId(token);
        WsSession wsSession = new WsSession(session);
        sessionUserMap.put(session.getId(), userId);
        wsSessionMap.put(session.getId(), wsSession);
        webSocketServer.registerSession(userId, wsSession);

        // 发送连接成功消息
        try {
            wsSession.sendMessage(objectMapper.writeValueAsString(Map.of("type", "CONNECTED", "userId", userId)));
        } catch (Exception e) {
            log.warn("发送连接确认失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = sessionUserMap.get(session.getId());
        if (userId == null) return;
        try {
            Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");
            if ("PING".equals(type)) {
                WsSession ws = wsSessionMap.get(session.getId());
                if (ws != null) ws.sendMessage("{\"type\":\"PONG\"}");
            }
        } catch (Exception e) {
            log.warn("处理消息失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUserMap.remove(session.getId());
        wsSessionMap.remove(session.getId());
        if (userId != null) {
            webSocketServer.removeSession(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket传输错误: {}", exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
            }
        }
        return null;
    }

    private void closeSession(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
        } catch (IOException e) {
            log.warn("关闭session失败", e);
        }
    }

    public static class WsSession {
        private final WebSocketSession session;

        public WsSession(WebSocketSession session) {
            this.session = session;
        }

        public synchronized void sendMessage(String text) throws IOException {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(text));
            }
        }
    }
}
