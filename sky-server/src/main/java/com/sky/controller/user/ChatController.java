package com.sky.controller.user;

import com.sky.ai.client.AiChatClient;
import com.sky.context.BaseContext;
import com.sky.dto.ChatDTO;
import com.sky.result.Result;
import com.sky.service.AIAssistantService;
import com.sky.vo.ChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/chat")
@Slf4j
public class ChatController {

    @Autowired
    private AIAssistantService aiAssistantService;

    @Autowired
    private AiChatClient aiChatClient;

    /**
     * 发送消息给AI助手
     *
     * @param chatDTO 用户消息和会话ID
     * @return AI回复
     */
    @PostMapping("/send")
    public Result<ChatVO> send(@RequestBody ChatDTO chatDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户 {} 发送AI消息: {}", userId, chatDTO.getMessage());

        ChatVO reply = aiAssistantService.chat(
                userId,
                chatDTO.getSessionId(),
                chatDTO.getMessage(),
                chatDTO.getTaboos(),
                chatDTO.getPeople(),
                chatDTO.getBudget()
        );

        return Result.success(reply);
    }

    /**
     * 加载对话历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam String sessionId) {
        Long userId = BaseContext.getCurrentId();
        List<Map<String, Object>> history = aiAssistantService.loadChatHistory(userId, sessionId);
        return Result.success(history);
    }

    /**
     * 流式对话（SSE），完整的 AI 点餐流程，支持工具调用 + 流式输出
     *
     * @param message   用户消息
     * @param sessionId 会话ID（可选）
     * @return SSE 流式响应
     */
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String message,
                             @RequestParam(required = false) String sessionId) {
        Long userId = BaseContext.getCurrentId();
        return aiAssistantService.chatStream(userId, sessionId, message, null, null, null);
    }
}
