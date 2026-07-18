package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.vo.CategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result save(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类：{}", categoryDTO);
        categoryService.save(categoryDTO);
        
        return Result.success();
    }

    /**
     * 分页查询分类
     */
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分页查询分类：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据 ID 查询分类（用于修改时回显）
     */
    @GetMapping("/{id}")
    public Result<CategoryVO> getById(@PathVariable Long id) {
        log.info("根据 ID 查询分类，id: {}", id);
        CategoryVO categoryVO = categoryService.getById(id);
        return Result.success(categoryVO);
    }

    /**
     * 修改分类
     */
    @PutMapping
    public Result update(@RequestBody CategoryDTO categoryDTO) {
        log.info("修改分类：{}", categoryDTO);
        categoryService.update(categoryDTO);
        
        return Result.success();
    }

    /**
     * 启用或禁用分类
     */
    @PatchMapping("/{status}")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("启用或禁用分类，id: {}, status: {}", id, status);
        categoryService.updateStatus(id, status);
        
        return Result.success();
    }

    /**
     * 根据类型查询分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type) {
        log.info("根据类型查询分类，type: {}", type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

    /**
     * 根据 ID 删除分类
     */
    @DeleteMapping
    public Result deleteById(@RequestParam Long id) {
        log.info("删除分类，id: {}", id);
        categoryService.deleteById(id);
        
        return Result.success();
    }
}
