package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.vo.CategoryVO;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dish", allEntries = true)
    public void save(DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        this.save(dish);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
                dishFlavorService.save(flavor);
            }
        }

        log.info("新增菜品成功，id: {}", dish.getId());
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品：{}", dishPageQueryDTO);

        int pageNum = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();

        Page<Dish> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        
        if (dishPageQueryDTO.getName() != null && !dishPageQueryDTO.getName().isEmpty()) {
            wrapper.like(Dish::getName, dishPageQueryDTO.getName());
        }

        if (dishPageQueryDTO.getCategoryId() != null) {
            wrapper.eq(Dish::getCategoryId, dishPageQueryDTO.getCategoryId());
        }

        if (dishPageQueryDTO.getStatus() != null) {
            wrapper.eq(Dish::getStatus, dishPageQueryDTO.getStatus());
        }

        Page<Dish> resultPage = this.page(page, wrapper);

        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish dish : resultPage.getRecords()) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);

            CategoryVO categoryVO = categoryService.getById(dish.getCategoryId());
            if (categoryVO != null) {
                dishVO.setCategoryName(categoryVO.getName());
            }

            dishVOList.add(dishVO);
        }

        return new PageResult(resultPage.getTotal(), dishVOList);
    }

    @Override
    public DishVO getWithFlavor(Long id) {
        log.info("根据 ID 查询菜品，id: {}", id);

        Dish dish = this.getById(id);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);

        CategoryVO categoryVO = categoryService.getById(dish.getCategoryId());
        if (categoryVO != null) {
            dishVO.setCategoryName(categoryVO.getName());
        }

        List<DishFlavor> flavors = dishFlavorService.getByDishId(id);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dish", allEntries = true)
    public void update(DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        this.updateById(dish);

        dishFlavorService.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
                dishFlavorService.save(flavor);
            }
        }

        log.info("修改菜品成功，id: {}", dishDTO.getId());
    }

    @Override
    @CacheEvict(value = "dish", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        log.info("启用或禁用菜品，id: {}, status: {}", id, status);

        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);

        this.updateById(dish);
    }

    @Override
    @Cacheable(value = "dish", key = "'categoryId:' + #categoryId")
    public List<Dish> listByCategoryId(Long categoryId) {
        log.info("根据分类 ID 查询菜品，categoryId: {}", categoryId);

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getCategoryId, categoryId)
               .eq(Dish::getStatus, 1);

        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dish", allEntries = true)
    public void deleteByIds(String ids) {
        log.info("批量删除菜品，ids: {}", ids);

        String[] idArray = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String id : idArray) {
            idList.add(Long.parseLong(id));
        }

        for (Long id : idList) {
            deleteById(id);
        }

        log.info("批量删除菜品成功，ids: {}", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dish", allEntries = true)
    public void deleteById(Long id) {
        log.info("删除菜品，id: {}", id);

        Dish dish = this.getById(id);
        if (dish == null) {
            throw new DeletionNotAllowedException("菜品不存在");
        }

        if (dish.getStatus() == 1) {
            throw new DeletionNotAllowedException("正在销售中的菜品不能删除");
        }

        Integer setmealCount = dishMapper.countSetmealByDishId(id);
        if (setmealCount != null && setmealCount > 0) {
            throw new DeletionNotAllowedException("菜品被套餐关联，不能删除");
        }

        this.removeById(id);

        dishFlavorService.deleteByDishId(id);

        log.info("删除菜品成功，id: {}", id);
    }

}
