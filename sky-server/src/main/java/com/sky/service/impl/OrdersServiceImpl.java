package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.OrderDetailService;
import com.sky.service.OrdersService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        log.info("提交订单：{}", ordersSubmitDTO);

        Long userId = BaseContext.getCurrentId();

        AddressBook addressBook = null;
        if (ordersSubmitDTO.getAddressBookId() != null) {
            addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        } else {
            addressBook = addressBookService.getDefault();
        }

        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(wrapper);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setUserName(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(buildAddress(addressBook));
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddressBookId(addressBook.getId());

        this.save(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailService.saveBatch(orderDetailList);

        shoppingCartService.clean();

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        log.info("下单成功，订单号：{}", orders.getNumber());
        return orderSubmitVO;
    }

    @Override
    @Transactional
    public void payment(OrdersPaymentDTO ordersPaymentDTO) {
        log.info("订单支付：{}", ordersPaymentDTO);

        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber());
        wrapper.eq(Orders::getUserId, userId);

        Orders orders = this.getOne(wrapper);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        orders.setPayStatus(Orders.PAID);
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setCheckoutTime(LocalDateTime.now());

        this.updateById(orders);

        log.info("订单支付成功，订单号：{}", ordersPaymentDTO.getOrderNumber());

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orders.getNumber());

        webSocketServer.sendToAll(JSON.toJSONString(map));
        log.info("来单提醒推送成功，订单号：{}", orders.getNumber());
    }

    @Override
    public void processTimeoutOrders() {
        log.info("开始处理超时订单...");

        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(15);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, Orders.PENDING_PAYMENT);
        wrapper.lt(Orders::getOrderTime, timeLimit);

        List<Orders> timeoutOrders = this.list(wrapper);

        if (timeoutOrders != null && !timeoutOrders.isEmpty()) {
            for (Orders order : timeoutOrders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时未支付，自动取消");
                order.setCancelTime(LocalDateTime.now());
                this.updateById(order);
                log.info("订单超时自动取消，订单号：{}", order.getNumber());
            }
            log.info("共处理{}个超时订单", timeoutOrders.size());
        } else {
            log.info("没有超时订单需要处理");
        }
    }

    @Override
    public void processDeliveryOrders() {
        log.info("开始处理派送中超时订单...");

        LocalDateTime timeLimit = LocalDateTime.now().minusHours(1);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        wrapper.lt(Orders::getOrderTime, timeLimit);

        List<Orders> deliveryOrders = this.list(wrapper);

        if (deliveryOrders != null && !deliveryOrders.isEmpty()) {
            for (Orders order : deliveryOrders) {
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                this.updateById(order);
                log.info("订单自动完成，订单号：{}", order.getNumber());
            }
            log.info("共处理{}个派送中超时订单", deliveryOrders.size());
        } else {
            log.info("没有派送中超时订单需要处理");
        }
    }

    @Override
    public PageResult pageQuery4Admin(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("分页查询订单：{}", ordersPageQueryDTO);

        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();

        if (ordersPageQueryDTO.getNumber() != null && !ordersPageQueryDTO.getNumber().isEmpty()) {
            wrapper.like(Orders::getNumber, ordersPageQueryDTO.getNumber());
        }

        if (ordersPageQueryDTO.getPhone() != null && !ordersPageQueryDTO.getPhone().isEmpty()) {
            wrapper.like(Orders::getPhone, ordersPageQueryDTO.getPhone());
        }

        if (ordersPageQueryDTO.getStatus() != null) {
            wrapper.eq(Orders::getStatus, ordersPageQueryDTO.getStatus());
        }

        if (ordersPageQueryDTO.getBeginTime() != null) {
            wrapper.ge(Orders::getOrderTime, ordersPageQueryDTO.getBeginTime());
        }

        if (ordersPageQueryDTO.getEndTime() != null) {
            wrapper.le(Orders::getOrderTime, ordersPageQueryDTO.getEndTime());
        }

        wrapper.orderByDesc(Orders::getOrderTime);

        Page<Orders> resultPage = this.page(page, wrapper);

        List<OrderVO> orderVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(resultPage.getRecords())) {
            for (Orders orders : resultPage.getRecords()) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
                detailWrapper.eq(OrderDetail::getOrderId, orders.getId());
                List<OrderDetail> orderDetailList = orderDetailService.list(detailWrapper);
                orderVO.setOrderDetailList(orderDetailList);

                if (!CollectionUtils.isEmpty(orderDetailList)) {
                    String orderDishes = orderDetailList.stream()
                            .map(detail -> detail.getName() + "*" + detail.getNumber())
                            .collect(Collectors.joining(", "));
                    orderVO.setOrderDishes(orderDishes);
                }

                orderVOList.add(orderVO);
            }
        }

        return new PageResult(resultPage.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        log.info("统计各状态订单数量");

        LambdaQueryWrapper<Orders> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
        Long toBeConfirmed = this.count(wrapper1);

        LambdaQueryWrapper<Orders> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Orders::getStatus, Orders.CONFIRMED);
        Long confirmed = this.count(wrapper2);

        LambdaQueryWrapper<Orders> wrapper3 = new LambdaQueryWrapper<>();
        wrapper3.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        Long deliveryInProgress = this.count(wrapper3);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed.intValue());
        orderStatisticsVO.setConfirmed(confirmed.intValue());
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress.intValue());

        return orderStatisticsVO;
    }

    @Override
    public OrderVO details(Long id) {
        log.info("查询订单详情，id: {}", id);

        Orders orders = this.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailService.list(detailWrapper);
        orderVO.setOrderDetailList(orderDetailList);

        if (!CollectionUtils.isEmpty(orderDetailList)) {
            String orderDishes = orderDetailList.stream()
                    .map(detail -> detail.getName() + "*" + detail.getNumber())
                    .collect(Collectors.joining(", "));
            orderVO.setOrderDishes(orderDishes);
        }

        return orderVO;
    }

    @Override
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        log.info("商家接单：{}", ordersConfirmDTO);

        Orders orders = this.getById(ordersConfirmDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.CONFIRMED);
        this.updateById(orders);

        log.info("接单成功，订单号：{}", orders.getNumber());
    }

    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        log.info("商家拒单：{}", ordersRejectionDTO);

        Orders orders = this.getById(ordersRejectionDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        this.updateById(orders);

        log.info("拒单成功，订单号：{}", orders.getNumber());
    }

    @Override
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        log.info("商家取消订单：{}", ordersCancelDTO);

        Orders orders = this.getById(ordersCancelDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        this.updateById(orders);

        log.info("取消订单成功，订单号：{}", orders.getNumber());
    }

    @Override
    @Transactional
    public void delivery(Long id) {
        log.info("派送订单，id: {}", id);

        Orders orders = this.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        this.updateById(orders);

        log.info("派送订单成功，订单号：{}", orders.getNumber());
    }

    @Override
    @Transactional
    public void complete(Long id) {
        log.info("完成订单，id: {}", id);

        Orders orders = this.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        this.updateById(orders);

        log.info("完成订单成功，订单号：{}", orders.getNumber());
    }

    @Override
    public void reminder(Long id) {
        log.info("用户催单，id: {}", id);

        Orders orders = this.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED) &&
            !orders.getStatus().equals(Orders.CONFIRMED) &&
            !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setRemindTime(LocalDateTime.now());
        this.updateById(orders);

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orders.getNumber());

        webSocketServer.sendToAll(JSON.toJSONString(map));
        log.info("催单推送成功，订单号：{}", orders.getNumber());
    }

    @Override
    public SalesTop10ReportVO getSalesTop10() {
        log.info("查询菜品销量排行榜");

        List<Map<String, Object>> top10List = orderDetailMapper.getTop10Dishes();

        List<String> nameList = top10List.stream()
                .map(item -> (String) item.get("name"))
                .filter(name -> name != null)
                .collect(Collectors.toList());

        List<String> numberList = top10List.stream()
                .map(item -> item.get("number"))
                .filter(number -> number != null)
                .map(String::valueOf)
                .collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(String.join(",", nameList))
                .numberList(String.join(",", numberList))
                .build();
    }

    private String buildAddress(AddressBook addressBook) {
        String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName();
        if (addressBook.getDetail() != null && !addressBook.getDetail().isEmpty()) {
            address += addressBook.getDetail();
        }
        return address;
    }

}
