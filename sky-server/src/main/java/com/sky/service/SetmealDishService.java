package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.SetmealDish;

import java.util.List;

public interface SetmealDishService extends IService<SetmealDish> {

    /**
     * 根据套餐 ID 查询套餐菜品关系列表
     * @param setmealId 套餐 ID
     * @return 套餐菜品关系列表
     */
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 根据套餐 ID 删除套餐菜品关系
     * @param setmealId 套餐 ID
     */
    void deleteBySetmealId(Long setmealId);

}
