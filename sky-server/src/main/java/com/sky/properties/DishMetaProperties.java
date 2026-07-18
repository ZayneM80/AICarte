package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 菜品元数据配置（辣度、分类、口味）
 * 加载自 application-{profile}.yml → sky.ai.dish-meta
 *
 * 格式：
 * <pre>
 * sky:
 *   ai:
 *     dish-meta:
 *       "蜀味水煮草鱼":
 *         spiciness: 特辣
 *         taste: 麻辣
 *         category: 鱼类
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "sky.ai")
@Data
public class DishMetaProperties {

    /** 菜品名称 → {spiciness, taste, category} */
    private Map<String, Map<String, String>> dishMeta = new LinkedHashMap<>();

    /** 获取菜品辣度，不在映射表中返回 "?" */
    public String getSpiciness(String dishName) {
        Map<String, String> meta = dishMeta.get(dishName);
        return meta != null ? meta.getOrDefault("spiciness", "?") : "?";
    }

    /** 获取菜品口味（麻辣/酸辣/咸鲜/清淡/甜等），不在映射表中返回 "" */
    public String getTaste(String dishName) {
        Map<String, String> meta = dishMeta.get(dishName);
        return meta != null ? meta.getOrDefault("taste", "") : "";
    }

    /** 获取菜品分类，不在映射表中返回 "" */
    public String getCategory(String dishName) {
        Map<String, String> meta = dishMeta.get(dishName);
        return meta != null ? meta.getOrDefault("category", "") : "";
    }
}
