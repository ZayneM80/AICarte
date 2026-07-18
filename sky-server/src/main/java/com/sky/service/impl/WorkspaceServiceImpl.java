package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        log.info("查询工作台数据，begin: {}, end: {}", begin, end);

        if (begin == null) {
            begin = LocalDate.now();
        }
        if (end == null) {
            end = LocalDate.now();
        }

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        BigDecimal turnover = ordersMapper.sumByStatusAndOrderTime(beginTime, endTime);
        turnover = turnover == null ? BigDecimal.ZERO : turnover;

        Integer validOrderCount = ordersMapper.countValidByOrderTime(beginTime, endTime);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;

        Integer totalOrderCount = ordersMapper.countByOrderTime(beginTime, endTime);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        Double unitPrice = 0.0;
        if (validOrderCount != 0) {
            unitPrice = turnover.doubleValue() / validOrderCount;
        }

        Integer newUsers = userMapper.countByCreateTimeBetween(beginTime, endTime);
        newUsers = newUsers == null ? 0 : newUsers;

        return BusinessDataVO.builder()
                .turnover(turnover.doubleValue())
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        log.info("查询订单概览数据");

        Integer waitingOrders = ordersMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        waitingOrders = waitingOrders == null ? 0 : waitingOrders;

        Integer deliveredOrders = ordersMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        deliveredOrders = deliveredOrders == null ? 0 : deliveredOrders;

        Integer completedOrders = ordersMapper.countByStatus(Orders.COMPLETED);
        completedOrders = completedOrders == null ? 0 : completedOrders;

        Integer cancelledOrders = ordersMapper.countByStatus(Orders.CANCELLED);
        cancelledOrders = cancelledOrders == null ? 0 : cancelledOrders;

        Integer allOrders = ordersMapper.countByOrderTime(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );
        allOrders = allOrders == null ? 0 : allOrders;

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        log.info("查询菜品概览数据");

        Integer sold = dishMapper.countByStatus(StatusConstant.ENABLE);
        sold = sold == null ? 0 : sold;

        Integer discontinued = dishMapper.countByStatus(StatusConstant.DISABLE);
        discontinued = discontinued == null ? 0 : discontinued;

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        log.info("查询套餐概览数据");

        Integer sold = setmealMapper.countByStatus(StatusConstant.ENABLE);
        sold = sold == null ? 0 : sold;

        Integer discontinued = setmealMapper.countByStatus(StatusConstant.DISABLE);
        discontinued = discontinued == null ? 0 : discontinued;

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
