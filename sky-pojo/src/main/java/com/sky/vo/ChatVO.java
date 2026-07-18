package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatVO implements Serializable {

    /** AI回复内容 */
    private String reply;

    /** 会话ID */
    private String sessionId;

    /** 回复时间 */
    private LocalDateTime timestamp;

    /** 购物车商品数量 */
    private Integer cartCount;

    /** 购物车总金额 */
    private BigDecimal cartTotal;

    /** 触发的动作：added_to_cart / order_submitted */
    private String action;

    /** 订单ID（如果下单成功） */
    private Long orderId;

    /** 订单号（如果下单成功） */
    private String orderNumber;

    /** 订单金额（如果下单成功） */
    private BigDecimal orderAmount;

}
