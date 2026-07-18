package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.properties.DishMetaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 菜品缓存 + 名称匹配工具
 */
@Component
@Slf4j
public class RecommendEngine {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishMetaProperties dishMeta;

    // 菜品缓存（30秒刷新一次）
    private volatile List<Dish> dishCache;
    private volatile long lastRefresh;
    private static final long CACHE_TTL = 30_000;

    // ============ 缓存管理 ============

    /** 获取所有启用菜品（带刷新缓存） */
    public List<Dish> getAvailableDishes() {
        long now = System.currentTimeMillis();
        if (dishCache == null || (now - lastRefresh) > CACHE_TTL) {
            LambdaQueryWrapper<Dish> w = new LambdaQueryWrapper<>();
            w.eq(Dish::getStatus, StatusConstant.ENABLE);
            dishCache = dishMapper.selectList(w);
            lastRefresh = now;
            log.debug("菜品缓存刷新，共 {} 道菜", dishCache.size());
        }
        return dishCache;
    }

    // ============ 菜品匹配工具 ============

    /** 从消息中匹配菜品名称 */
    public String matchDish(String msg) {
        for (Dish d : getAvailableDishes()) {
            if (msg.contains(d.getName()) || d.getName().contains(msg)) {
                return d.getName();
            }
        }
        return null;
    }

    /** 根据名称查找菜品 */
    public Dish findDish(String name) {
        for (Dish d : getAvailableDishes()) {
            if (d.getName().equals(name)) return d;
        }
        return null;
    }
}
