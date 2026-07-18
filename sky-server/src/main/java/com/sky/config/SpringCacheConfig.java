package com.sky.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache 配置类
 * 配置基于 Redis 的缓存管理器，支持 @Cacheable 等注解
 * @date 2026-04-07
 */
@Configuration
@EnableCaching
public class SpringCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 创建 JSON 序列化器，用于将对象转为 JSON 存入 Redis
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        
        // 创建 ObjectMapper，负责 JSON 序列化配置
        ObjectMapper om = new ObjectMapper();
        
        // 注册 Java 8 时间模块，支持 LocalDateTime 等时间类型序列化
        om.registerModule(new JavaTimeModule());
        
        // 禁用时间戳格式，使用字符串格式输出时间
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 设置可见性：允许序列化所有字段（包括私有字段）
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用类型信息：反序列化时能识别具体类型，避免类型丢失
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // 将配置好的 ObjectMapper 绑定到 JSON 序列化器
        jsonSerializer.setObjectMapper(om);

        // 创建字符串序列化器，用于序列化 Redis 的 key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 配置 Redis 缓存默认规则
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // key 使用字符串序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                // value 使用 JSON 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                // 默认过期时间 30 分钟
                .entryTtl(Duration.ofMinutes(30))
                // 不缓存 null 值
                .disableCachingNullValues();

        // 自定义各个缓存区域的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // 菜品缓存：2 小时
        cacheConfigs.put("dish", defaultConfig.entryTtl(Duration.ofHours(2)));
        // 套餐缓存：2 小时
        cacheConfigs.put("setmeal", defaultConfig.entryTtl(Duration.ofHours(2)));
        // 分类缓存：1 小时
        cacheConfigs.put("category", defaultConfig.entryTtl(Duration.ofHours(1)));
        // 店铺状态缓存：1 天
        cacheConfigs.put("shop", defaultConfig.entryTtl(Duration.ofDays(1)));

        // 构建并返回 Redis 缓存管理器
        return RedisCacheManager.builder(connectionFactory)
                // 设置默认配置
                .cacheDefaults(defaultConfig)
                // 加载自定义的缓存区域配置
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
