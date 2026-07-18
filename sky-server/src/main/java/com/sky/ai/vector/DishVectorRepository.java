package com.sky.ai.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * pgvector 菜品向量仓库
 *
 * 核心操作：
 * - 写入/更新菜品向量（upsert）
 * - 余弦距离搜索（ORDER BY embedding <=> ? LIMIT N）
 * - 清空重建
 *
 * 面试亮点：JdbcTemplate + pgvector 向量算子（<=>）的 SQL 级使用
 */
@Repository
@Slf4j
public class DishVectorRepository {

    @Autowired
    @Qualifier("vectorJdbcTemplate")
    private JdbcTemplate jdbc;

    private static final String TABLE = "dish_embeddings";

    /** 插入或更新菜品向量 */
    public void upsert(DishEmbedding de, float[] vector) {
        String vectorStr = toPgVector(vector);
        String sql = "INSERT INTO " + TABLE + " (dish_id, dish_name, category, taste, spiciness, price, description, embedding, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, " + vectorStr + "::vector, NOW()) "
                + "ON CONFLICT (dish_id) DO UPDATE SET "
                + "dish_name = EXCLUDED.dish_name, category = EXCLUDED.category, taste = EXCLUDED.taste, "
                + "spiciness = EXCLUDED.spiciness, price = EXCLUDED.price, description = EXCLUDED.description, "
                + "embedding = EXCLUDED.embedding, updated_at = NOW()";

        jdbc.update(sql,
                de.getDishId(), de.getDishName(), de.getCategory(),
                de.getTaste(), de.getSpiciness(),
                de.getPrice() != null ? de.getPrice() : BigDecimal.ZERO,
                de.getDescription() != null ? de.getDescription() : "");
    }

    /**
     * 语义搜索：余弦距离最近的前 N 道菜
     * @param queryVector  查询向量
     * @param topN         返回条数
     * @return 匹配的菜品元数据列表（不含原始向量）
     */
    public List<DishEmbedding> search(float[] queryVector, int topN) {
        String vectorStr = toPgVector(queryVector);
        String sql = "SELECT dish_id, dish_name, category, taste, spiciness, price, description "
                + "FROM " + TABLE
                + " WHERE embedding IS NOT NULL"
                + " ORDER BY embedding <=> " + vectorStr + "::vector"
                + " LIMIT ?";

        return jdbc.query(sql, new DishEmbeddingRowMapper(), topN);
    }

    /** 统计已嵌入菜品数 */
    public int count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        Integer cnt = jdbc.queryForObject(sql, Integer.class);
        return cnt != null ? cnt : 0;
    }

    /** 清空所有向量（重建时使用） */
    public void clearAll() {
        jdbc.update("DELETE FROM " + TABLE);
        log.info("已清空 dish_embeddings 表");
    }

    /** 检查特定菜品是否已嵌入 */
    public boolean existsByDishId(Long dishId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE dish_id = ?";
        Integer cnt = jdbc.queryForObject(sql, Integer.class, dishId);
        return cnt != null && cnt > 0;
    }

    // ============ 工具方法 ============

    /** 将 float[] 转为 pgvector 文本格式 '[0.1,0.2,...]' */
    public static String toPgVector(float[] vector) {
        StringBuilder sb = new StringBuilder("'[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append("]'");
        return sb.toString();
    }

    private static class DishEmbeddingRowMapper implements RowMapper<DishEmbedding> {
        @Override
        public DishEmbedding mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DishEmbedding.builder()
                    .dishId(rs.getLong("dish_id"))
                    .dishName(rs.getString("dish_name"))
                    .category(rs.getString("category"))
                    .taste(rs.getString("taste"))
                    .spiciness(rs.getString("spiciness"))
                    .price(rs.getBigDecimal("price"))
                    .description(rs.getString("description"))
                    .build();
        }
    }
}
