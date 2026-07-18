package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("建立WebSocket连接，sid: {}", sid);
        sessionMap.put(sid, session);
        log.info("当前在线人数: {}", sessionMap.size());
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("关闭WebSocket连接，sid: {}", sid);
        sessionMap.remove(sid);
        log.info("当前在线人数: {}", sessionMap.size());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到消息，sid: {}, message: {}", sid, message);
    }

    public void sendToAll(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("发送消息成功: {}", message);
            } catch (Exception e) {
                log.error("发送消息失败", e);
            }
        }
    }

    public void sendToClient(String sid, String message) {
        Session session = sessionMap.get(sid);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("发送消息给sid: {}, message: {}", sid, message);
            } catch (Exception e) {
                log.error("发送消息失败，sid: {}", sid, e);
            }
        } else {
            log.warn("会话不存在或已关闭，sid: {}", sid);
        }
    }
}
