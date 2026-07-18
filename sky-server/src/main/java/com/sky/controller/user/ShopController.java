package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController("userShopController")
@RequestMapping("/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SHOP_STATUS_KEY = "shop:status";

    @GetMapping("/status")
    public Result<Integer> getShopStatus() {
        log.info("用户端获取店铺营业状态");

        Object statusObj = redisTemplate.opsForValue().get(SHOP_STATUS_KEY);

        Integer status = (statusObj != null) ? (Integer) statusObj : StatusConstant.DISABLE;

        log.info("店铺营业状态：{}", status);
        return Result.success(status);
    }

    @GetMapping("/getMerchantInfo")
    public Result<Map<String, Object>> getMerchantInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("phone", "18500557668");
        return Result.success(info);
    }
}
