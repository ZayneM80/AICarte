package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.ZhiPuProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ZhiPuUtil {

    @Autowired
    private ZhiPuProperties zhiPuProperties;

    private static final int TIMEOUT_MSEC = 30 * 1000;

    /**
     * 识别图片（通过图片URL）
     *
     * @param imageUrl 图片URL地址
     * @param prompt   提示词，告诉AI要识别什么
     * @return AI识别结果文本
     */
    public String recognizeImage(String imageUrl, String prompt) {
        log.info("智谱AI图片识别，imageUrl: {}, prompt: {}", imageUrl, prompt);

        try {
            JSONObject requestBody = buildVisionRequestBody(imageUrl, prompt);
            String responseJson = doPost(
                    zhiPuProperties.getBaseUrl() + "/chat/completions",
                    requestBody.toJSONString()
            );

            return parseResponse(responseJson);
        } catch (Exception e) {
            log.error("智谱AI图片识别失败：{}", e.getMessage(), e);
            throw new RuntimeException("图片识别失败：" + e.getMessage(), e);
        }
    }

    /**
     * 识别图片（通过Base64编码的图片数据）
     *
     * @param base64Image Base64编码的图片
     * @param mimeType    图片MIME类型，如 "image/jpeg", "image/png"
     * @param prompt      提示词
     * @return AI识别结果文本
     */
    public String recognizeImageFromBase64(String base64Image, String mimeType, String prompt) {
        log.info("智谱AI图片识别(Base64)，mimeType: {}, prompt: {}", mimeType, prompt);

        String dataUrl = "data:" + mimeType + ";base64," + base64Image;
        return recognizeImage(dataUrl, prompt);
    }

    /**
     * 纯文本对话
     *
     * @param prompt 提示词
     * @return AI回复文本
     */
    public String chat(String prompt) {
        log.info("智谱AI文本对话，prompt: {}", prompt);

        try {
            JSONObject requestBody = buildTextRequestBody(prompt);
            String responseJson = doPost(
                    zhiPuProperties.getBaseUrl() + "/chat/completions",
                    requestBody.toJSONString()
            );

            return parseResponse(responseJson);
        } catch (Exception e) {
            log.error("智谱AI对话失败：{}", e.getMessage(), e);
            throw new RuntimeException("AI对话失败：" + e.getMessage(), e);
        }
    }

    /**
     * 多轮对话（支持 Function Calling，自动模式）
     */
    public JSONObject chatWithHistory(List<Map<String, Object>> messages,
                                       List<Map<String, Object>> tools) {
        return chatWithHistory(messages, tools, false);
    }

    /**
     * 多轮对话（支持 Function Calling + 强制工具模式）
     *
     * @param messages  完整对话历史
     * @param tools     工具定义列表
     * @param forceTool true=tool_choice=required 强制调工具，false=auto 自由决定
     * @return API 原始响应 JSON 对象
     */
    public JSONObject chatWithHistory(List<Map<String, Object>> messages,
                                       List<Map<String, Object>> tools,
                                       boolean forceTool) {
        log.info("AI多轮对话，消息数: {}, 工具数: {}, forceTool: {}",
                messages.size(), tools != null ? tools.size() : 0, forceTool);

        try {
            JSONObject requestBody = buildChatRequestBody(messages, tools, forceTool);
            String responseJson = doPost(
                    getChatBaseUrl() + "/chat/completions",
                    requestBody.toJSONString(),
                    getChatApiKey()
            );
            return JSON.parseObject(responseJson);
        } catch (Exception e) {
            log.error("AI多轮对话失败：{}", e.getMessage(), e);
            throw new RuntimeException("AI对话失败：" + e.getMessage(), e);
        }
    }

    /**
     * 流式对话（SSE），支持 Function Calling，仅流式输出文本块
     *
     * @param messages 完整对话历史（含 system/user/assistant/tool）
     * @param tools    工具定义，null 时不调工具
     * @param onChunk  每段文本回调
     * @param onDone   完成回调
     * @param onError  错误回调
     */
    public void chatStream(List<Map<String, Object>> messages,
                            List<Map<String, Object>> tools,
                            java.util.function.Consumer<String> onChunk,
                            Runnable onDone,
                            java.util.function.Consumer<Throwable> onError) {
        try {
            JSONObject body = buildChatRequestBody(messages, tools, false);
            body.put("stream", true);
            // 流式场景用更长 max_tokens 避免截断
            body.put("max_tokens", 2048);

            HttpPost httpPost = new HttpPost(getChatBaseUrl() + "/chat/completions");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + getChatApiKey());
            httpPost.setEntity(new StringEntity(body.toJSONString(), "UTF-8"));
            httpPost.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_MSEC)
                    .setConnectionRequestTimeout(TIMEOUT_MSEC)
                    .setSocketTimeout(0) // 流式不断
                    .build());

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse resp = client.execute(httpPost);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(resp.getEntity().getContent(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    if (data.isEmpty()) continue;

                    try {
                        JSONObject chunk = JSON.parseObject(data);
                        JSONArray choices = chunk.getJSONArray("choices");
                        if (choices == null || choices.isEmpty()) continue;

                        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                        if (delta == null) continue;

                        String content = delta.getString("content");
                        if (content != null && !content.isEmpty()) {
                            onChunk.accept(content);
                        }
                    } catch (Exception e) {
                        log.warn("解析流式块失败: {}", data, e);
                    }
                }
                onDone.run();
            }
        } catch (Exception e) {
            log.error("AI流式调用失败", e);
            onError.accept(e);
        }
    }

    /**
     * 构建多轮对话请求体（支持 Function Calling）
     */
    /** 获取聊天用的base URL和API key */
    private String getChatBaseUrl() {
        String u = zhiPuProperties.getChatBaseUrl();
        return (u != null && !u.isEmpty()) ? u : zhiPuProperties.getBaseUrl();
    }
    private String getChatApiKey() {
        String k = zhiPuProperties.getChatApiKey();
        return (k != null && !k.isEmpty()) ? k : zhiPuProperties.getApiKey();
    }

    private JSONObject buildChatRequestBody(List<Map<String, Object>> messages,
                                             List<Map<String, Object>> tools,
                                             boolean forceTool) {
        JSONObject body = new JSONObject();
        String chatModel = zhiPuProperties.getChatModel();
        body.put("model", chatModel != null && !chatModel.isEmpty()
                ? chatModel : zhiPuProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.9);
        body.put("max_tokens", 1024);

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", forceTool ? "required" : "auto");
        }

        return body;
    }
    private JSONObject buildVisionRequestBody(String imageUrl, String prompt) {
        JSONObject body = new JSONObject();
        body.put("model", zhiPuProperties.getModel());

        // 构建 messages
        JSONArray messages = new JSONArray();

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        JSONArray content = new JSONArray();

        // 文本部分
        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);

        // 图片部分
        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        JSONObject imageUrlObj = new JSONObject();
        imageUrlObj.put("url", imageUrl);
        imagePart.put("image_url", imageUrlObj);
        content.add(imagePart);

        userMessage.put("content", content);
        messages.add(userMessage);

        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);

        return body;
    }

    /**
     * 构建纯文本对话的请求体
     */
    private JSONObject buildTextRequestBody(String prompt) {
        JSONObject body = new JSONObject();
        String chatModel = zhiPuProperties.getChatModel();
        body.put("model", chatModel != null && !chatModel.isEmpty()
                ? chatModel : zhiPuProperties.getModel());

        JSONArray messages = new JSONArray();

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);

        return body;
    }

    /**
     * 发送POST请求
     */
    private String doPost(String url, String jsonBody) throws IOException {
        return doPost(url, jsonBody, zhiPuProperties.getApiKey());
    }
    private String doPost(String url, String jsonBody, String apiKey) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            // 设置请求体
            StringEntity entity = new StringEntity(jsonBody, "UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);

            // 设置超时
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_MSEC)
                    .setConnectionRequestTimeout(TIMEOUT_MSEC)
                    .setSocketTimeout(TIMEOUT_MSEC)
                    .build();
            httpPost.setConfig(config);

            log.debug("智谱AI请求: {}", jsonBody);
            response = httpClient.execute(httpPost);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.debug("智谱AI响应: {}", result);

            return result;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.warn("关闭响应失败", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.warn("关闭HttpClient失败", e);
            }
        }
    }

    /**
     * 解析API响应，提取AI回复内容
     */
    private String parseResponse(String responseJson) {
        JSONObject response = JSON.parseObject(responseJson);

        // 检查是否有错误
        if (response.containsKey("error")) {
            JSONObject error = response.getJSONObject("error");
            String errorMsg = error.getString("message");
            log.error("智谱AI返回错误：{}", errorMsg);
            throw new RuntimeException("智谱AI错误：" + errorMsg);
        }

        // 提取 choices[0].message.content
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            log.warn("智谱AI返回空choices，完整响应：{}", responseJson);
            return "";
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        String content = message.getString("content");

        log.info("智谱AI识别结果：{}", content);
        return content != null ? content.trim() : "";
    }
}
