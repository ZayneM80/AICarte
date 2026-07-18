package com.sky.controller.user;

import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理（用户端）
 */
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/list")
    public Result<List<Setmeal>> list(Long categoryId) {
        log.info("用户端根据分类 ID 查询套餐，categoryId: {}", categoryId);
        List<Setmeal> list = setmealService.listByCategoryId(categoryId);
        return Result.success(list);
    }
}
