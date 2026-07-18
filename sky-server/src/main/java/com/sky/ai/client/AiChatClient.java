package com.sky.ai.client;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Spring AI 风格的 ChatClient 抽象接口
 */
public interface AiChatClient {

    ChatClientRequest prompt();

    interface ChatClientRequest {
        ChatClientRequest user(String message);
        ChatClientRequest system(String systemPrompt);
        ChatClientRequest messages(List<Map<String, Object>> history);
        ChatClientRequest temperature(double temperature);
        ChatClientRequest maxTokens(int maxTokens);
        ChatClientRequest model(String model);

        String call();
        void stream(Consumer<String> onChunk, Runnable onDone, Consumer<Throwable> onError);

        default SseEmitter stream() {
            SseEmitter emitter = new SseEmitter(0L);
            new Thread(() ->
                stream(
                    chunk -> { try { emitter.send(chunk); } catch (Exception ignore) {} },
                    emitter::complete,
                    err -> { try { emitter.completeWithError(err); } catch (Exception ignore) {} }
                )
            ).start();
            return emitter;
        }
    }
}
