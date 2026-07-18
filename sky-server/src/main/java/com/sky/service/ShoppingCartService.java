package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService extends IService<ShoppingCart> {
    
    void add(ShoppingCartDTO shoppingCartDTO);
    
    List<ShoppingCart> list();
    
    void clean();
    
    void sub(ShoppingCartDTO shoppingCartDTO);
}
