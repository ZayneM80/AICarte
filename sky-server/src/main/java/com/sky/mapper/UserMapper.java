package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT COUNT(*) FROM user WHERE create_time <= #{end}")
    Integer countByCreateTimeEnd(@Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM user WHERE create_time >= #{begin} AND create_time <= #{end}")
    Integer countByCreateTimeBetween(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
