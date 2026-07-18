package com.sky.ai.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.ZhiPuProperties;
import com.sky.utils.ZhiPuUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Component
@Slf4j
public class ZhiPuChatClient implements AiChatClient {

    @Autowired
    private ZhiPuProperties zhiPuProperties;

    @Autowired
    private ZhiPuUtil zhiPuUtil;

    private static final int TIMEOUT_MSEC = 30_000;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 2048;

    @Override
    public ChatClientRequest prompt() {
        return new DefaultChatRequest();
    }

    private class DefaultChatRequest implements ChatClientRequest {
        private String userMessage;
        private String systemPrompt;
        private List<Map<String, Object>> history;
        private double temperature = DEFAULT_TEMPERATURE;
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private String model;

        @Override
        public ChatClientRequest user(String message) { this.userMessage = message; return this; }
        @Override
        public ChatClientRequest system(String s) { this.systemPrompt = s; return this; }
        @Override
        public ChatClientRequest messages(List<Map<String, Object>> h) { this.history = h; return this; }
        @Override
        public ChatClientRequest temperature(double t) { this.temperature = t; return this; }
        @Override
        public ChatClientRequest maxTokens(int m) { this.maxTokens = m; return this; }
        @Override
        public ChatClientRequest model(String m) { this.model = m; return this; }

        @Override
        public String call() {
            List<Map<String, Object>> messages = buildMessages();
            JSONObject response = zhiPuUtil.chatWithHistory(messages, null);
            return extractContent(response);
        }

        @Override
        public void stream(Consumer<String> onChunk, Runnable onDone, Consumer<Throwable> onError) {
            List<Map<String, Object>> messages = buildMessages();
            JSONObject body = new JSONObject();
            body.put("model", resolveModel());
            body.put("messages", messages);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("stream", true);

            String url = zhiPuProperties.getBaseUrl() + "/chat/completions";
            String apiKey = zhiPuProperties.getApiKey();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + apiKey);
                httpPost.setEntity(new StringEntity(body.toJSONString(), StandardCharsets.UTF_8));
                httpPost.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_MSEC)
                        .setConnectionRequestTimeout(TIMEOUT_MSEC)
                        .setSocketTimeout(TIMEOUT_MSEC).build());

                try (CloseableHttpResponse response = httpClient.execute(httpPost);
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) break;
                            JSONObject chunk = JSON.parseObject(data);
                            JSONArray choices = chunk.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                if (delta != null) {
                                    String content = delta.getString("content");
                                    if (content != null) onChunk.accept(content);
                                }
                            }
                        }
                    }
                }
                onDone.run();
            } catch (Exception e) {
                log.error("AI流式调用失败", e);
                onError.accept(e);
            }
        }

        private List<Map<String, Object>> buildMessages() {
            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null) {
                Map<String, Object> sys = new LinkedHashMap<>();
                sys.put("role", "system"); sys.put("content", systemPrompt);
                messages.add(sys);
            }
            if (history != null) {
                for (Map<String, Object> h : history) {
                    if (!"system".equals(h.get("role"))) messages.add(h);
                }
            }
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("role", "user"); user.put("content", userMessage);
            messages.add(user);
            return messages;
        }

        private String extractContent(JSONObject response) {
            try {
                return response.getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message")
                        .getString("content").trim();
            } catch (Exception e) { return ""; }
        }

        private String resolveModel() {
            if (model != null) return model;
            String cm = zhiPuProperties.getChatModel();
            return (cm != null && !cm.isEmpty()) ? cm : zhiPuProperties.getModel();
        }
    }
}
