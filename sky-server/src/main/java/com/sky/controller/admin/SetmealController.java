package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        setmealService.save(setmealDTO);

        return Result.success();
    }

    /**
     * 分页查询套餐
     */
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据 ID 查询套餐（用于修改时回显）
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据 ID 查询套餐，id: {}", id);
        SetmealVO setmealVO = setmealService.getWithDishes(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     */
    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);

        return Result.success();
    }

    @PostMapping("status/{status}")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("启用或禁用套餐，id: {}, status: {}", id, status);
        setmealService.updateStatus(id, status);

        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Setmeal>> list(Long categoryId) {
        log.info("根据分类 ID 查询套餐，categoryId: {}", categoryId);
        List<Setmeal> list = setmealService.listByCategoryId(categoryId);
        return Result.success(list);
    }

    @PostMapping("/delete")
    public Result deleteById(@RequestParam Long id) {
        log.info("删除套餐，id: {}", id);
        setmealService.deleteById(id);

        return Result.success();
    }

    @DeleteMapping
    public Result deleteByIds(@RequestParam String ids) {
        log.info("批量删除套餐，ids: {}", ids);
        setmealService.deleteByIds(ids);

        return Result.success();
    }
}
