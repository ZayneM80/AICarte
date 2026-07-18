package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    
    /**
     * 新增菜品
     * @param dishDTO 菜品信息
     */
    void save(DishDTO dishDTO);

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据 ID 查询菜品（用于修改时回显）
     * @param id 菜品 ID
     * @return 菜品信息
     */
    DishVO getWithFlavor(Long id);

    /**
     * 修改菜品
     * @param dishDTO 菜品信息
     */
    void update(DishDTO dishDTO);

    /**
     * 启用或禁用菜品
     * @param id 菜品 ID
     * @param status 状态（0-停售，1-起售）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据分类 ID 查询菜品列表
     * @param categoryId 分类 ID
     * @return 菜品列表
     */
    List<Dish> listByCategoryId(Long categoryId);

    /**
     * 根据 ID 删除菜品
     * @param id 菜品 ID
     */
    void deleteById(Long id);

    /**
     * 批量删除菜品
     * @param ids 菜品 ID 列表（逗号分隔）
     */
    void deleteByIds(String ids);
    
}
