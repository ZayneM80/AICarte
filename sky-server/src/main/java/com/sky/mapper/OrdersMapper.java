package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT SUM(amount) FROM orders WHERE status = 5 AND order_time >= #{begin} AND order_time <= #{end}")
    BigDecimal sumByStatusAndOrderTime(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM orders WHERE order_time >= #{begin} AND order_time <= #{end}")
    Integer countByOrderTime(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM orders WHERE status = 5 AND order_time >= #{begin} AND order_time <= #{end}")
    Integer countValidByOrderTime(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM orders WHERE status = #{status}")
    Integer countByStatus(Integer status);


}
