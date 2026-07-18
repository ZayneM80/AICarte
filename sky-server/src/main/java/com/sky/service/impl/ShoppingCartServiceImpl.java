package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车服务实现类
 * 实现购物车的添加、查询等业务逻辑
 */
@Service
@Slf4j
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 添加商品到购物车
     * 业务逻辑：
     * 1. 获取当前登录用户ID
     * 2. 判断购物车中是否已存在相同商品
     * 3. 如果存在，数量+1；如果不存在，创建新记录
     *
     * @param shoppingCartDTO 购物车数据传输对象
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);

        if (shoppingCartDTO.getDishId() != null) {
            shoppingCart.setDishId(shoppingCartDTO.getDishId());
            // 仅根据菜品ID匹配，忽略口味，同菜直接叠加
            wrapper.eq(ShoppingCart::getDishId, shoppingCartDTO.getDishId());
        } else {
            shoppingCart.setSetmealId(shoppingCartDTO.getSetmealId());
            wrapper.eq(ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId());
        }

        ShoppingCart existCart = this.getOne(wrapper);

        if (existCart != null) {
            existCart.setNumber(existCart.getNumber() + 1);
            this.updateById(existCart);
            log.info("购物车已存在，数量+1");
        } else {
            if (shoppingCartDTO.getDishId() != null) {
                DishVO dishVO = dishService.getWithFlavor(shoppingCartDTO.getDishId());
                if (dishVO != null) {
                    shoppingCart.setName(dishVO.getName());
                    shoppingCart.setImage(dishVO.getImage());
                    shoppingCart.setAmount(dishVO.getPrice());
                }
            } else {
                Setmeal setmeal = setmealService.getById(shoppingCartDTO.getSetmealId());
                if (setmeal != null) {
                    shoppingCart.setName(setmeal.getName());
                    shoppingCart.setImage(setmeal.getImage());
                    shoppingCart.setAmount(setmeal.getPrice());
                }
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            if (shoppingCart.getAmount() == null) {
                shoppingCart.setAmount(BigDecimal.ZERO);
            }
            this.save(shoppingCart);
            log.info("新增购物车记录");
        }
    }

    /**
     * 查询当前登录用户的购物车列表
     * 按创建时间倒序排列，最新添加的商品显示在前面
     *
     * @return 购物车列表
     */
    @Override
    public List<ShoppingCart> list() {
        log.info("查询当前用户购物车列表");

        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        wrapper.orderByDesc(ShoppingCart::getCreateTime);

        return this.list(wrapper);
    }

    @Override
    public void clean() {
        log.info("清空当前用户购物车");

        Long userId = BaseContext.getCurrentId();


        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);

        this.remove(wrapper);

        log.info("清空购物车成功");
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        log.info("减少购物车中商品的数量：{}", shoppingCartDTO);

        // 空字符串转null，防止查询不匹配
        if (shoppingCartDTO.getDishFlavor() != null && shoppingCartDTO.getDishFlavor().isEmpty()) {
            shoppingCartDTO.setDishFlavor(null);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart existCart;

        // 优先根据购物车id查询（前端编译版本传的是id）
        if (shoppingCartDTO.getId() != null) {
            existCart = this.getById(shoppingCartDTO.getId());
            if (existCart != null && !existCart.getUserId().equals(userId)) {
                return;
            }
        } else {
            // 兼容旧逻辑：通过dishId+口味匹配
            LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ShoppingCart::getUserId, userId);
            if (shoppingCartDTO.getDishId() != null) {
                wrapper.eq(ShoppingCart::getDishId, shoppingCartDTO.getDishId());
                wrapper.eq(ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor());
            } else {
                wrapper.eq(ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId());
            }
            existCart = this.getOne(wrapper);
        }

        if (existCart == null) {
            log.info("未匹配到购物车记录");
            return;
        }
        if (existCart.getNumber() > 1) {
            existCart.setNumber(existCart.getNumber() - 1);
            this.updateById(existCart);
            log.info("购物车数量-1");
        } else if (existCart.getNumber() == 1) {
            this.removeById(existCart.getId());
            log.info("购物车数量为 1，删除该记录");
        }
    }
}
