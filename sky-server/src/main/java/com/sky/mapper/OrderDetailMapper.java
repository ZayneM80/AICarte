package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Select("SELECT od.name, SUM(od.number) as number " +
            "FROM order_detail od " +
            "INNER JOIN orders o ON od.order_id = o.id " +
            "WHERE o.status = 5 AND od.dish_id IS NOT NULL " +
            "GROUP BY od.name " +
            "ORDER BY number DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getTop10Dishes();

}
