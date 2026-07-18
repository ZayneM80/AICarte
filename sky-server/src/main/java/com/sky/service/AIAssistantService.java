package com.sky.service;

import com.sky.vo.ChatVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AIAssistantService {

    /**
     * 用户发送消息，获取AI回复
     */
    ChatVO chat(Long userId, String sessionId, String message,
                String taboos, Integer people, Integer budget);

    /**
     * 加载对话历史
     */
    List<Map<String, Object>> loadChatHistory(Long userId, String sessionId);

    /**
     * 流式对话（SSE）
     */
    SseEmitter chatStream(Long userId, String sessionId, String message,
                          String taboos, Integer people, Integer budget);
}
