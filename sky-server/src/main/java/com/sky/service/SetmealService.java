package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    
    /**
     * 新增套餐
     * @param setmealDTO 套餐信息
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据 ID 查询套餐（用于修改时回显）
     * @param id 套餐 ID
     * @return 套餐信息
     */
    SetmealVO getWithDishes(Long id);

    /**
     * 修改套餐
     * @param setmealDTO 套餐信息
     */
    void update(SetmealDTO setmealDTO);

    /**
     * 启用或禁用套餐
     * @param id 套餐 ID
     * @param status 状态（0-停售，1-起售）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据分类 ID 查询套餐列表
     * @param categoryId 分类 ID
     * @return 套餐列表
     */
    List<Setmeal> listByCategoryId(Long categoryId);

    /**
     * 根据 ID 删除套餐
     * @param id 套餐 ID
     */
    void deleteById(Long id);

    /**
     * 批量删除套餐
     * @param ids 套餐 ID 列表（逗号分隔）
     */
    void deleteByIds(String ids);
    
}
