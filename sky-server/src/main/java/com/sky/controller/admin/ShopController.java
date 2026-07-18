package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SHOP_STATUS_KEY = "shop:status";

    @GetMapping("/status")
    public Result<Integer> getShopStatus() {
        log.info("获取店铺营业状态");
        
        Object statusObj = redisTemplate.opsForValue().get(SHOP_STATUS_KEY);
        
        Integer status = (statusObj != null) ? (Integer) statusObj : StatusConstant.DISABLE;
        
        log.info("店铺营业状态：{}", status);
        return Result.success(status);
    }

    @PutMapping("/{status}")
    public Result setShopStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态，status: {}", status);
        
        redisTemplate.opsForValue().set(SHOP_STATUS_KEY, status, 1, TimeUnit.DAYS);
        
        return Result.success();
    }
}
