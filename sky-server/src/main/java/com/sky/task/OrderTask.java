package com.sky.task;

import com.sky.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrdersService ordersService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void processTimeoutOrders() {
        log.info("定时任务：处理超时订单");
        ordersService.processTimeoutOrders();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processDeliveryOrders() {
        log.info("定时任务：处理派送中超时订单");
        ordersService.processDeliveryOrders();
    }
}
