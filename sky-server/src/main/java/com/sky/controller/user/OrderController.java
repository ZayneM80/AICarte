package com.sky.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.OrderDetailMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController("userOrdersController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/getEstimatedDeliveryTime")
    public Result<String> getEstimatedDeliveryTime() {
        // 返回当前时间+40分钟作为预计送达时间
        String time = LocalDateTime.now().plusMinutes(40)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Result.success(time);
    }

    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = ordersService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PutMapping("/payment")
    public Result<Map<String, Object>> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("订单支付模拟成功：{}", ordersPaymentDTO);

        // 更新订单状态为已支付
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber());
        Orders orders = ordersService.getOne(wrapper);
        if (orders != null) {
            orders.setStatus(Orders.CONFIRMED);
            orders.setPayStatus(Orders.PAID);
            ordersService.updateById(orders);
        }

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("mock", true);
        mockResult.put("code", "SUCCESS");
        mockResult.put("orderNumber", ordersPaymentDTO.getOrderNumber());
        mockResult.put("transactionId", "mock_" + System.currentTimeMillis());
        return Result.success(mockResult);
    }

    @GetMapping("/historyOrders")
    public Result<PageResult> historyOrders(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) Integer status) {
        log.info("查询历史订单，page: {}, pageSize: {}, status: {}", page, pageSize, status);
        Long userId = BaseContext.getCurrentId();
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getOrderTime);
        ordersService.page(pageInfo, wrapper);

        List<OrderVO> list = new ArrayList<>();
        for (Orders orders : pageInfo.getRecords()) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(orders, vo);
            vo.setDeliveryFee(new BigDecimal("4.0"));
            LambdaQueryWrapper<OrderDetail> dw = new LambdaQueryWrapper<>();
            dw.eq(OrderDetail::getOrderId, orders.getId());
            vo.setOrderDetailList(orderDetailMapper.selectList(dw));
            list.add(vo);
        }

        PageResult pageResult = new PageResult();
        pageResult.setTotal(pageInfo.getTotal());
        pageResult.setRecords(list);
        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("查询订单详情，id: {}", id);
        Orders orders = ordersService.getById(id);
        if (orders == null) {
            return Result.error("订单不存在");
        }
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        orderVO.setOrderDetailList(details);
        orderVO.setDeliveryFee(new BigDecimal("4.0"));
        // 拼接菜品名称用于展示
        StringBuilder dishNames = new StringBuilder();
        for (OrderDetail d : details) {
            if (dishNames.length() > 0) dishNames.append("，");
            dishNames.append(d.getName()).append("×").append(d.getNumber());
        }
        orderVO.setOrderDishes(dishNames.toString());
        return Result.success(orderVO);
    }

    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable Long id) {
        log.info("用户催单，id: {}", id);
        ordersService.reminder(id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id) {
        log.info("用户取消订单，id: {}", id);
        Orders orders = ordersService.getById(id);
        if (orders != null) {
            orders.setStatus(Orders.CANCELLED);
            ordersService.updateById(orders);
        }
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable Long id) {
        log.info("再来一单，id: {}", id);
        // 查询原订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        if (orderDetails == null || orderDetails.isEmpty()) {
            return Result.error("订单不存在");
        }
        // 将原订单的菜品加入购物车
        Long userId = BaseContext.getCurrentId();
        for (OrderDetail detail : orderDetails) {
            ShoppingCart cart = new ShoppingCart();
            cart.setUserId(userId);
            cart.setDishId(detail.getDishId());
            cart.setSetmealId(detail.getSetmealId());
            cart.setDishFlavor(detail.getDishFlavor());
            cart.setNumber(detail.getNumber());
            cart.setAmount(detail.getAmount());
            cart.setImage(detail.getImage());
            cart.setName(detail.getName());
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(cart);
        }
        return Result.success();
    }

}
