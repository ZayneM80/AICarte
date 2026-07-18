package com.sky.ai.vector;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.ZhiPuProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 向量嵌入服务 — 调用智谱 text_embedding-v2 API
 *
 * 面试亮点：对接大模型 Embedding API + 批量处理 + 容错
 */
@Service
@Slf4j
public class EmbeddingService {

    @Autowired
    private ZhiPuProperties zhiPuProperties;

    private static final int TIMEOUT_MSEC = 30_000;
    private static final String EMBEDDING_MODEL = "text_embedding_v2";

    /**
     * 将文本转为向量
     *
     * @param text 要嵌入的文本（最长 512 token）
     * @return 1024 维 float 数组
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[1024];
        }

        try {
            JSONObject body = new JSONObject();
            body.put("model", EMBEDDING_MODEL);
            body.put("input", text);

            String responseJson = doPost(
                    zhiPuProperties.getBaseUrl() + "/embeddings",
                    body.toJSONString()
            );

            JSONObject response = JSONObject.parseObject(responseJson);
            if (response.containsKey("error")) {
                String errMsg = response.getJSONObject("error").getString("message");
                log.error("智谱 Embedding API 错误: {}", errMsg);
                return new float[1024];
            }

            JSONArray data = response.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                log.warn("智谱 Embedding 返回空 data");
                return new float[1024];
            }

            JSONArray embeddingArr = data.getJSONObject(0).getJSONArray("embedding");
            float[] vector = new float[embeddingArr.size()];
            for (int i = 0; i < embeddingArr.size(); i++) {
                vector[i] = embeddingArr.getFloatValue(i);
            }

            log.info("文本嵌入成功，维度: {}", vector.length);
            return vector;

        } catch (Exception e) {
            log.error("调用智谱 Embedding API 失败: {}", e.getMessage(), e);
            return new float[1024];
        }
    }

    /**
     * 批量嵌入（智谱 API 支持一次传入多个文本）
     *
     * @param texts 文本列表
     * @return 向量列表（顺序对应）
     */
    public float[][] embedBatch(String[] texts) {
        if (texts == null || texts.length == 0) {
            return new float[0][];
        }

        try {
            JSONArray inputArr = new JSONArray();
            for (String t : texts) {
                inputArr.add(t);
            }

            JSONObject body = new JSONObject();
            body.put("model", EMBEDDING_MODEL);
            body.put("input", inputArr);

            String responseJson = doPost(
                    zhiPuProperties.getBaseUrl() + "/embeddings",
                    body.toJSONString()
            );

            JSONObject response = JSONObject.parseObject(responseJson);
            JSONArray data = response.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return new float[0][];
            }

            float[][] results = new float[data.size()][];
            for (int i = 0; i < data.size(); i++) {
                JSONArray embArr = data.getJSONObject(i).getJSONArray("embedding");
                float[] vec = new float[embArr.size()];
                for (int j = 0; j < embArr.size(); j++) {
                    vec[j] = embArr.getFloatValue(j);
                }
                results[i] = vec;
            }
            return results;

        } catch (Exception e) {
            log.error("批量嵌入失败: {}", e.getMessage(), e);
            return new float[0][];
        }
    }

    /**
     * 构建菜品嵌入文本（含名称、口味、价格、描述）
     */
    public static String buildDishText(String name, String taste, String spiciness,
                                        String category, String description, double price) {
        StringBuilder sb = new StringBuilder(name);
        if (taste != null && !taste.isEmpty()) sb.append(" ").append(taste);
        if (spiciness != null && !spiciness.isEmpty()) sb.append(" ").append(spiciness);
        if (category != null && !category.isEmpty()) sb.append(" ").append(category);
        sb.append(" ¥").append(String.format("%.0f", price));
        if (description != null && !description.isEmpty()) sb.append(" ").append(description);
        return sb.toString();
    }

    // ============ HTTP 客户端 ============

    private String doPost(String urlStr, String jsonBody) throws Exception {
        URI uri = new URI(urlStr);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + zhiPuProperties.getApiKey());
            conn.setConnectTimeout(TIMEOUT_MSEC);
            conn.setReadTimeout(TIMEOUT_MSEC);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                String result = response.toString();
                if (status >= 400) {
                    log.error("Embedding API HTTP {}: {}", status, result);
                    return "{\"error\":{\"message\":\"HTTP " + status + "\"}}";
                }
                return result;
            }
        } finally {
            conn.disconnect();
        }
    }
}
