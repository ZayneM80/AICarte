package com.sky.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 第二层：工具调用层——购物车唯一可信数据源
 * 所有购物车操作必须经过这三个工具，AI不能直接操作数据库
 * 核心：商品已存在时只累加数量，绝不重复插入整条记录
 */
@Component
@Slf4j
public class CartTools {

    @Autowired private ShoppingCartMapper cartMapper;
    @Autowired private DishMapper dishMapper;
    @Autowired private SetmealMapper setmealMapper;

    // ============ 工具1：查询购物车 ============
    /**
     * AI调用此工具获取用户真实购物车数据
     * 返回结构化JSON，AI话术必须100%对齐此数据
     */
    public CartSnapshot queryCart(Long userId) {
        List<ShoppingCart> items = cartMapper.selectList(
            new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)
        );
        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItem> list = new ArrayList<>();
        for (ShoppingCart ci : items) {
            totalCount += ci.getNumber();
            totalAmount = totalAmount.add(
                (ci.getAmount() != null ? ci.getAmount() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(ci.getNumber()))
            );
            list.add(new CartItem(ci.getName(), ci.getNumber(), ci.getAmount()));
        }
        return new CartSnapshot(list, totalCount, totalAmount);
    }

    // ============ 工具2：增量加购（解决翻倍bug的核心） ============
    /**
     * AI调用此工具加购商品
     * 内置逻辑：已存在同口味商品→仅累加数量；不同口味→分别加购
     * @param count 目标数量，1=默认一份
     * @param setMode true=直接设为目标数量(用户说"要两碗"), false=累加(用户说"再来一碗")
     * @param dishFlavor 口味(辣度:中辣/温度:去冰)，无口味传null
     */
    public AddResult addToCart(Long userId, Long dishId, Long setmealId, int count,
                                boolean setMode, String dishFlavor) {
        if (count < 1) count = 1; if (count > 99) count = 99;

        LambdaQueryWrapper<ShoppingCart> w = new LambdaQueryWrapper<>();
        w.eq(ShoppingCart::getUserId, userId);
        if (dishId != null) w.eq(ShoppingCart::getDishId, dishId);
        else w.eq(ShoppingCart::getSetmealId, setmealId);
        // 有口味→按口味匹配；无口味→匹配无口味的
        if (dishFlavor != null && !dishFlavor.isEmpty()) {
            w.eq(ShoppingCart::getDishFlavor, dishFlavor);
        } else {
            w.isNull(ShoppingCart::getDishFlavor);
        }

        ShoppingCart exist = cartMapper.selectOne(w);
        if (exist != null) {
            // 【关键】已存在同口味→只累加数量，绝不insert新行
            int newCount = setMode ? count : exist.getNumber() + count;
            exist.setNumber(newCount);
            cartMapper.updateById(exist);
            log.info("加购(UPDATE) {}[{}] {}→{}", exist.getName(), exist.getDishFlavor(),
                    exist.getNumber() - (setMode ? 0 : count), newCount);
            return new AddResult(true, exist.getName(), newCount, exist.getAmount());
        } else {
            // 不存在→查菜品/套餐信息，插入新行
            String name, image; BigDecimal price;
            if (dishId != null) {
                Dish d = dishMapper.selectById(dishId);
                if (d == null) return new AddResult(false, "菜品不存在", 0, null);
                name = d.getName(); image = d.getImage(); price = d.getPrice();
            } else {
                Setmeal m = setmealMapper.selectById(setmealId);
                if (m == null) return new AddResult(false, "套餐不存在", 0, null);
                name = m.getName(); image = m.getImage(); price = m.getPrice();
            }
            ShoppingCart sc = new ShoppingCart();
            sc.setUserId(userId); sc.setDishId(dishId); sc.setSetmealId(setmealId);
            sc.setName(name); sc.setImage(image != null ? image : ""); sc.setAmount(price);
            sc.setDishFlavor(dishFlavor);
            sc.setNumber(count); sc.setCreateTime(LocalDateTime.now());
            cartMapper.insert(sc);
            String tag = dishFlavor != null ? "[" + dishFlavor + "]" : "";
            log.info("加购(INSERT) {} {} ×{}", name, tag, count);
            return new AddResult(true, name, count, price);
        }
    }

    // ============ 工具3：批量确认加购（从AI推荐中提取菜名） ============
    /**
     * 用户说"好的就这两个"→从AI最后推荐中提取菜名，批量加购
     * 自动跳过购物车已有的商品（不会重复添加）
     */
    public List<AddResult> confirmFromAIRecommendation(Long userId, String aiLastReply) {
        if (aiLastReply == null) return Collections.emptyList();
        Set<String> existing = queryCart(userId).items.stream()
            .map(CartItem::getName).collect(Collectors.toSet());

        List<AddResult> results = new ArrayList<>();
        // 从AI回复中匹配菜品名
        List<Dish> allDishes = dishMapper.selectList(null);
        for (Dish d : allDishes) {
            if (aiLastReply.contains(d.getName()) && !existing.contains(d.getName())) {
                results.add(addToCart(userId, d.getId(), null, 1, false, null));
            }
        }
        List<Setmeal> allMeals = setmealMapper.selectList(null);
        for (Setmeal m : allMeals) {
            if (aiLastReply.contains(m.getName()) && !existing.contains(m.getName())) {
                results.add(addToCart(userId, null, m.getId(), 1, false, null));
            }
        }
        return results;
    }

    // ============ 数据结构（纯手写getter，不依赖Lombok注解处理） ============
    @AllArgsConstructor
    public static class CartItem {
        private String name; private int count; private BigDecimal price;
        public String getName(){return name;} public int getCount(){return count;} public BigDecimal getPrice(){return price;}
    }
    @AllArgsConstructor
    public static class CartSnapshot {
        private List<CartItem> items; private int totalCount; private BigDecimal totalAmount;
        public List<CartItem> getItems(){return items;} public int getTotalCount(){return totalCount;} public BigDecimal getTotalAmount(){return totalAmount;}
    }
    @AllArgsConstructor
    public static class AddResult {
        private boolean success; private String name; private int count; private BigDecimal price;
        public boolean isSuccess(){return success;} public String getName(){return name;} public int getCount(){return count;} public BigDecimal getPrice(){return price;}
    }
}
