package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 添加分类
     * @param categoryDTO 分类信息
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 分页查询分类
     * @param categoryPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据 ID 查询分类
     * @param id 分类 ID
     * @return 分类信息
     */
    CategoryVO getById(Long id);

    /**
     * 修改分类
     * @param categoryDTO 分类信息
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 启用或禁用分类
     * @param id 分类 ID
     * @param status 状态（0-禁用，1-启用）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据类型查询分类列表
     * @param type 分类类型（1-菜品分类，2-套餐分类）
     * @return 分类列表
     */
    List<Category> list(Integer type);

    /**
     * 根据 ID 删除分类
     * 删除前检查是否有关联的菜品或套餐
     * @param id 分类 ID
     */
    void deleteById(Long id);

}
