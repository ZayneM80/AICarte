package com.sky.service.impl;

import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        log.info("营业额统计，begin: {}, end: {}", begin, end);

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<BigDecimal> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            BigDecimal turnover = ordersMapper.sumByStatusAndOrderTime(beginTime, endTime);
            turnover = turnover == null ? BigDecimal.ZERO : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(convertToString(dateList))
                .turnoverList(convertToString(turnoverList))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        log.info("用户统计，begin: {}, end: {}", begin, end);

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer totalUser = userMapper.countByCreateTimeEnd(endTime);
            totalUserList.add(totalUser);

            Integer newUser = userMapper.countByCreateTimeBetween(beginTime, endTime);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(convertToString(dateList))
                .totalUserList(convertToString(totalUserList))
                .newUserList(convertToString(newUserList))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        log.info("订单统计，begin: {}, end: {}", begin, end);

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = ordersMapper.countByOrderTime(beginTime, endTime);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);
            totalOrderCount += orderCount;

            Integer validOrderCountDay = ordersMapper.countValidByOrderTime(beginTime, endTime);
            validOrderCountDay = validOrderCountDay == null ? 0 : validOrderCountDay;
            validOrderCountList.add(validOrderCountDay);
            validOrderCount += validOrderCountDay;
        }

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(convertToString(dateList))
                .orderCountList(convertToString(orderCountList))
                .validOrderCountList(convertToString(validOrderCountList))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
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

    private String convertToString(List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
