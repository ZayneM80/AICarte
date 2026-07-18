package com.sky.ai.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品向量嵌入 — 对应 pgvector dish_embeddings 表
 *
 * 面试亮点：ORM 映射 + 向量数据库 schema 设计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishEmbedding {

    private Long id;
    private Long dishId;
    private String dishName;
    private String category;
    private String taste;
    private String spiciness;
    private BigDecimal price;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 向量由数据库自动管理，Java 侧不反序列化 */
}
