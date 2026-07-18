package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    @Select("SELECT COUNT(*) FROM setmeal_dish WHERE dish_id = #{dishId}")
    Integer countSetmealByDishId(Long dishId);

    @Select("SELECT COUNT(*) FROM dish WHERE status = #{status}")
    Integer countByStatus(Integer status);

}
