package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.save(dishDTO);
        
        return Result.success();
    }

    /**
     * 分页查询菜品
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据 ID 查询菜品（用于修改时回显）
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据 ID 查询菜品，id: {}", id);
        DishVO dishVO = dishService.getWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     */
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.update(dishDTO);
        
        return Result.success();
    }

    /**
     * 启用或禁用菜品
     */
    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id) {
        log.info("启用或禁用菜品，id: {}, status: {}", id, status);
        dishService.updateStatus(id, status);
        
        return Result.success();
    }

    /**
     * 根据分类 ID 查询菜品列表
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类 ID 查询菜品，categoryId: {}", categoryId);
        List<Dish> list = dishService.listByCategoryId(categoryId);
        return Result.success(list);
    }
    /**
     * 根据 ID 删除菜品
     */
    @PostMapping("/delete")
    public Result deleteById(@RequestParam Long id) {
        log.info("删除菜品，id: {}", id);
        dishService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除菜品
     */
    @DeleteMapping
    public Result deleteByIds(@RequestParam String ids) {
        log.info("批量删除菜品，ids: {}", ids);
        dishService.deleteByIds(ids);
        return Result.success();
    }
}
