package com.sky.controller.admin;

import com.sky.ai.vector.DishEmbeddingSyncService;
import com.sky.ai.vector.DishVectorRepository;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品向量管理 — 管理端控制台
 *
 * 面试亮点：AI 基础设施的可观测性 + 手动触发重建
 */
@RestController("adminVectorController")
@RequestMapping("/admin/vector")
@Api(tags = "菜品向量管理")
@Slf4j
public class VectorAdminController {

    @Autowired
    private DishEmbeddingSyncService syncService;

    @Autowired
    private DishVectorRepository dishVectorRepository;

    @GetMapping("/status")
    @ApiOperation("查看向量库状态")
    public Result<Map<String, Object>> status() {
        int count = dishVectorRepository.count();
        Map<String, Object> info = new HashMap<>();
        info.put("embeddedDishCount", count);
        info.put("vectorEngine", "pgvector");
        info.put("embeddingModel", "text_embedding-v2");
        info.put("dimension", 1024);
        info.put("searchAlgorithm", "cosine_distance (<=>)");
        return Result.success(info);
    }

    @PostMapping("/sync")
    @ApiOperation("全量同步菜品到向量库")
    public Result<Map<String, Object>> sync() {
        log.info("管理端触发全量菜品向量同步");
        List<String> errors = new ArrayList<>();
        try {
            syncService.syncAll(errors);
            int count = dishVectorRepository.count();
            Map<String, Object> result = new HashMap<>();
            result.put("embeddedCount", count);
            result.put("errors", errors);
            result.put("hasError", !errors.isEmpty());
            return Result.success(result);
        } catch (Exception e) {
            log.error("向量同步失败", e);
            errors.add("系统错误: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("embeddedCount", 0);
            result.put("errors", errors);
            result.put("hasError", true);
            return Result.success(result);
        }
    }
}
