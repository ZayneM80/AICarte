package com.sky.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 对话历史管理器
 * 职责：Redis CRUD、TTL 管理、滑动窗口截断
 */
@Component
@Slf4j
public class ChatHistoryManager {

    @Autowired
    private RedisTemplate<String, Object> redis;

    private static final String KEY_CHAT = "ai:chat:";
    private static final String KEY_REC = "ai:rec:";
    private static final int TTL_MINUTES = 30;
    private static final int MAX_HISTORY = 20;

    // ============ 对话历史 ============

    /** 获取会话历史（ChatMemory 风格） */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> get(Long userId, String sessionId) {
        String key = KEY_CHAT + userId + ":" + sessionId;
        Object cached = redis.opsForValue().get(key);
        return cached instanceof List
                ? new ArrayList<>((List<Map<String, Object>>) cached)
                : new ArrayList<>();
    }

    /** 添加消息到会话历史（ChatMemory 风格） */
    public void add(Long userId, String sessionId, List<Map<String, Object>> messages) {
        String key = KEY_CHAT + userId + ":" + sessionId;
        redis.opsForValue().set(key, messages, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 清除会话历史（ChatMemory 风格，方案 f：下单后触发） */
    public void clear(Long userId, String sessionId) {
        String key = KEY_CHAT + userId + ":" + sessionId;
        redis.delete(key);
        log.info("清除对话历史 userId={} sessionId={}", userId, sessionId);
    }

    /** 加载用户会话历史（旧版，兼容保留） */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadHist(Long userId, String sessionId) {
        return get(userId, sessionId);
    }

    /** 保存用户会话历史（旧版，兼容保留） */
    public void saveHist(Long userId, String sessionId, List<Map<String, Object>> history) {
        add(userId, sessionId, history);
    }

    // ============ 推荐缓存 ============

    /** 获取上次推荐结果 */
    public Object getRecKey(Long userId) {
        return redis.opsForValue().get(KEY_REC + userId);
    }

    /** 保存推荐结果（30分钟过期） */
    public void saveRecKey(Long userId, String rec) {
        redis.opsForValue().set(KEY_REC + userId, rec, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 删除推荐结果 */
    public void deleteRecKey(Long userId) {
        redis.delete(KEY_REC + userId);
    }

}
