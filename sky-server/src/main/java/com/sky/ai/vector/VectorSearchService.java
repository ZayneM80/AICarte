package com.sky.ai.vector;

import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.properties.DishMetaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量搜索编排服务
 *
 * 流程：用户 query → Embedding → pgvector 余弦搜索 → 注入 LLM
 *
 * 面试亮点：RAG 完整链路 + 多数据源协作（MySQL + pgvector + LLM）
 */
@Service
@Slf4j
public class VectorSearchService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DishVectorRepository dishVectorRepository;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishMetaProperties dishMeta;

    /**
     * 语义搜索菜品
     *
     * @param userQuery 用户自然语言查询（如"有点酸不太辣的菜"）
     * @param topN      返回条数
     * @return 匹配的菜品格式文本（可直接注入 LLM prompt）
     */
    public String search(String userQuery, int topN) {
        // 1. 用户查询 → 向量
        float[] queryVector = embeddingService.embed(userQuery);

        // 2. pgvector 余弦距离搜索
        List<DishEmbedding> results = dishVectorRepository.search(queryVector, topN);

        if (results.isEmpty()) {
            log.info("向量搜索无结果，query: {}", userQuery);
            return "";
        }

        // 3. 格式化为文本
        return results.stream()
                .map(this::formatDish)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 获取所有已嵌入的菜品作为上下文
     */
    public String getAllDishesAsContext() {
        // 获取所有启用菜品
        List<Dish> dishes = dishMapper.selectList(null);
        if (dishes == null || dishes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("【本店菜品列表（请只推荐以下菜品）】\n");
        for (Dish d : dishes) {
            if (d.getStatus() != null && d.getStatus() == 0) continue; // 跳过停售
            String cat = dishMeta.getCategory(d.getName());
            String taste = d.getTaste() != null ? d.getTaste() : dishMeta.getTaste(d.getName());
            String spiciness = d.getSpiciness() != null ? d.getSpiciness() : dishMeta.getSpiciness(d.getName());
            sb.append("- ").append(d.getName())
                    .append(" ¥").append(d.getPrice())
                    .append(" [").append(cat != null ? cat : "其他").append("]");
            if (taste != null && !taste.isEmpty()) sb.append(" ").append(taste);
            if (spiciness != null && !spiciness.isEmpty()) sb.append(" ").append(spiciness);
            if (d.getDescription() != null && !d.getDescription().isEmpty()) {
                sb.append(" ").append(d.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 语义搜索 + 全部菜品兜底
     * 先尝试向量搜索找最匹配的，再附上完整菜单
     * 如果 pgvector 不可用，自动降级为仅返回 MySQL 菜品列表
     */
    public String buildContextWithSearch(String userQuery) {
        StringBuilder ctx = new StringBuilder();

        // 1. 向量搜索结果（最相关的 5 道菜）— 容错：失败则降级
        if (userQuery != null && !userQuery.isEmpty()) {
            try {
                String matched = search(userQuery, 5);
                if (!matched.isEmpty()) {
                    ctx.append("【语义匹配菜品】\n").append(matched).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("向量搜索降级（pgvector 可能未就绪），使用全量菜单: {}", e.getMessage());
            }
        }

        // 2. 完整菜单兜底（从 MySQL 读取）
        ctx.append(getAllDishesAsContext());

        return ctx.toString();
    }

    /** 格式化单道菜 */
    private String formatDish(DishEmbedding de) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(de.getDishName())
                .append(" ¥").append(de.getPrice());
        if (de.getTaste() != null && !de.getTaste().isEmpty()) {
            sb.append(" ").append(de.getTaste());
        }
        if (de.getSpiciness() != null && !de.getSpiciness().isEmpty()) {
            sb.append(" ").append(de.getSpiciness());
        }
        if (de.getCategory() != null && !de.getCategory().isEmpty()) {
            sb.append(" [").append(de.getCategory()).append("]");
        }
        return sb.toString();
    }
}
