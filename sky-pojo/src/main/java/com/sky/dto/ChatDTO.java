package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ChatDTO implements Serializable {

    private String message;
    private String sessionId;

    // 用户偏好（前端 localStorage 存储，每次请求带上）
    private String taboos;     // 忌口：不吃辣/海鲜过敏/素食 等
    private Integer people;    // 用餐人数，默认1
    private Integer budget;    // 人均预算（元），默认0=不限

}
