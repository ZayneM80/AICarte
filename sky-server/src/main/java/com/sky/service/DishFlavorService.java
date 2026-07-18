package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.DishFlavor;

import java.util.List;

public interface DishFlavorService extends IService<DishFlavor> {

    /**
     * 根据菜品 ID 查询口味列表
     * @param dishId 菜品 ID
     * @return 口味列表
     */
    List<DishFlavor> getByDishId(Long dishId);

    /**
     * 根据菜品 ID 删除口味
     * @param dishId 菜品 ID
     */
    void deleteByDishId(Long dishId);

}
