package com.sky.dto;

import com.sky.entity.DishFlavor;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;
    //菜品名称
    private String name;
    //菜品分类id
    private Long categoryId;
    //菜品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 停售 1 起售
    private Integer status;
    //口味（麻辣/酸辣/咸鲜/清淡/甜/蒜香），用于 AI 推荐
    private String taste;
    //辣度（不辣/微辣/中辣/特辣），用于 AI 推荐
    private String spiciness;
    //可选规格（如"少糖/加辣"等）
    private List<DishFlavor> flavors = new ArrayList<>();

}
