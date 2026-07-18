package com.sky.ai.vector;

import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.properties.DishMetaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜品向量同步服务
 *
 * 启动时自动将 MySQL 中的菜品数据同步到 pgvector（含 embedding 生成）
 * 面试亮点：CommandLineRunner + 异构数据同步 + 增量更新
 */
@Service
@Slf4j
public class DishEmbeddingSyncService implements CommandLineRunner {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishMetaProperties dishMeta;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DishVectorRepository dishVectorRepository;

    @Override
    public void run(String... args) {
        try {
            syncAll();
        } catch (Exception e) {
            log.warn("启动时向量同步失败（pgvector 可能未就绪），可通过管理端手动触发: {}", e.getMessage());
        }
    }

    /**
     * 全量同步所有菜品到 pgvector
     * 幂等：已存在的菜品会更新，不存在的会插入
     */
    public void syncAll() {
        syncAll(null);
    }

    /**
     * 全量同步，支持收集失败原因
     * @param errorCollector 可选，接收错误信息
     */
    public void syncAll(List<String> errorCollector) {
        List<Dish> dishes = dishMapper.selectList(null);
        if (dishes == null || dishes.isEmpty()) {
            log.warn("MySQL 中无菜品数据，跳过向量同步");
            return;
        }

        log.info("开始同步 {} 道菜品到 pgvector...", dishes.size());
        int success = 0, skipped = 0;

        for (Dish dish : dishes) {
            if (dish.getStatus() != null && dish.getStatus() == 0) {
                skipped++;
                continue; // 跳过停售菜品
            }
            try {
                syncOne(dish);
                success++;
            } catch (Exception e) {
                String err = String.format("同步菜品[%s]失败: %s", dish.getName(), e.getMessage());
                log.error(err, e);
                if (errorCollector != null) {
                    errorCollector.add(err);
                }
            }
        }

        log.info("菜品同步完成：成功 {} 道，跳过 {} 道（停售），共 {} 道",
                success, skipped, dishes.size());
    }

    /**
     * 同步单道菜品
     */
    public void syncOne(Dish dish) {
        // 1. 从 YAML 获取元数据
        String name = dish.getName();
        String category = dishMeta.getCategory(name);
        String taste = dish.getTaste() != null ? dish.getTaste() : dishMeta.getTaste(name);
        String spiciness = dish.getSpiciness() != null ? dish.getSpiciness() : dishMeta.getSpiciness(name);

        // 2. 构建嵌入文本
        double price = dish.getPrice() != null ? dish.getPrice().doubleValue() : 0;
        String desc = dish.getDescription();
        String embedText = EmbeddingService.buildDishText(
                name, taste, spiciness, category, desc, price);

        // 3. 调用 Embedding API 生成向量
        float[] vector = embeddingService.embed(embedText);

        // 4. 写入 pgvector
        DishEmbedding de = DishEmbedding.builder()
                .dishId(dish.getId())
                .dishName(name)
                .category(category)
                .taste(taste)
                .spiciness(spiciness)
                .price(dish.getPrice())
                .description(desc)
                .build();

        dishVectorRepository.upsert(de, vector);

        if (log.isDebugEnabled()) {
            log.debug("已同步菜品[{}]到 pgvector", name);
        }
    }
}
