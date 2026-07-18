package com.sky.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.CartTools;
import com.sky.ai.DishTools;
import com.sky.ai.IntentRouter;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.service.AIAssistantService;
import com.sky.service.OrdersService;
import com.sky.utils.ZhiPuUtil;
import com.sky.vo.ChatVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI点餐助手 — Function Calling 版本
 *
 * 架构：消息 + 工具定义 → AI → AI 决定调工具 → 执行工具（查DB/加购）
 *       → 结果送回 AI → AI 生成自然回复
 *
 * AI 保持上下文，工具保证数据真实
 */
@Service
@Slf4j
public class AIAssistantServiceImpl implements AIAssistantService {

    @Autowired
    private ChatHistoryManager historyManager;

    @Autowired
    private RecommendEngine recommendEngine;

    @Autowired
    private CartTools cartTools;

    @Autowired
    private OrdersService orderService;

    @Autowired
    private IntentRouter intentRouter;

    @Autowired
    private DishTools dishTools;

    @Autowired
    private ZhiPuUtil zhiPuUtil;

    private static final int MAX_TOOL_ROUNDS = 5;

    @Override
    public ChatVO chat(Long uid, String sid, String msg, String taboos, Integer people, Integer budget) {
        if (sid == null || sid.isEmpty()) {
            sid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        List<Map<String, Object>> hist = historyManager.loadHist(uid, sid);
        CartTools.CartSnapshot cart = cartTools.queryCart(uid);

        // 意图路由（仅下单和确认走规则）
        IntentRouter.Intent intent = intentRouter.classify(msg);
        String action = null, orderNo = null, directReply = null;
        BigDecimal orderAmt = null;
        Long orderId = null;

        switch (intent) {
            case CHECKOUT:
                if (cart.getTotalCount() == 0) {
                    directReply = "购物车空的，先点菜吧～";
                } else {
                    OrderSubmitVO ov = submit(uid);
                    if (ov != null) {
                        orderNo = ov.getOrderNumber();
                        orderAmt = ov.getOrderAmount();
                        orderId = ov.getId();
                        action = "order_submitted";
                        directReply = "下单成功！订单号 " + orderNo + "，金额 ¥" + orderAmt;
                        // ✅ 方案 f：下单成功 → 清对话历史，下一单全新开始
                        historyManager.clear(uid, sid);
                        hist.clear(); // 内存也要清，否则后面 saveHist 又写回去了
                    } else {
                        directReply = "请先添加地址再下单。";
                    }
                }
                break;
            case CONFIRM: {
                String lr = (String) historyManager.getRecKey(uid);
                List<CartTools.AddResult> rs = cartTools.confirmFromAIRecommendation(uid, lr);
                if (!rs.isEmpty()) {
                    action = "added_to_cart";
                    directReply = "已加入购物车：" + rs.stream()
                            .map(r -> r.getName() + (r.getCount() > 1 ? "×" + r.getCount() : ""))
                            .collect(Collectors.joining("、"));
                    historyManager.deleteRecKey(uid);
                } else {
                    directReply = "购物车空的，先点菜吧～";
                }
                break;
            }
        }

        String reply;
        if (directReply != null) {
            reply = directReply;
        } else {
            reply = chatWithTools(msg, hist, intent);
        }

        // 存历史
        Map<String, Object> um = new LinkedHashMap<>();
        um.put("role", "user"); um.put("content", msg);
        hist.add(um);
        Map<String, Object> am = new LinkedHashMap<>();
        am.put("role", "assistant"); am.put("content", reply);
        hist.add(am);
        if (hist.size() > 20) hist = new ArrayList<>(hist.subList(hist.size() - 20, hist.size()));
        historyManager.saveHist(uid, sid, hist);

        cart = cartTools.queryCart(uid);
        return ChatVO.builder().reply(reply).sessionId(sid)
                .timestamp(LocalDateTime.now())
                .cartCount(cart.getTotalCount()).cartTotal(cart.getTotalAmount())
                .action(action).orderId(orderId).orderNumber(orderNo).orderAmount(orderAmt)
                .build();
    }

    @Override
    public SseEmitter chatStream(Long uid, String sid, String msg,
                                  String taboos, Integer people, Integer budget) {
        SseEmitter emitter = new SseEmitter(0L);

        if (sid == null || sid.isEmpty()) {
            sid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        final String finalSid = sid;

        new Thread(() -> {
            List<Map<String, Object>> hist = historyManager.loadHist(uid, finalSid);
            CartTools.CartSnapshot cart = cartTools.queryCart(uid);
            IntentRouter.Intent intent = intentRouter.classify(msg);

            try {
                // CHECKOUT / CONFIRM — 直接回复，无需 AI
                if (intent == IntentRouter.Intent.CHECKOUT || intent == IntentRouter.Intent.CONFIRM) {
                    String directReply;
                    if (intent == IntentRouter.Intent.CHECKOUT) {
                        if (cart.getTotalCount() == 0) {
                            directReply = "购物车空的，先点菜吧～";
                        } else {
                            OrderSubmitVO ov = submit(uid);
                            if (ov != null) {
                                directReply = "下单成功！订单号 " + ov.getOrderNumber() + "，金额 ¥" + ov.getOrderAmount();
                                historyManager.clear(uid, finalSid);
                                hist.clear();
                            } else {
                                directReply = "请先添加地址再下单。";
                            }
                        }
                    } else {
                        String lr = (String) historyManager.getRecKey(uid);
                        List<CartTools.AddResult> rs = cartTools.confirmFromAIRecommendation(uid, lr);
                        if (!rs.isEmpty()) {
                            directReply = "已加入购物车：" + rs.stream()
                                    .map(r -> r.getName() + (r.getCount() > 1 ? "×" + r.getCount() : ""))
                                    .collect(Collectors.joining("、"));
                            historyManager.deleteRecKey(uid);
                        } else {
                            directReply = "购物车空的，先点菜吧～";
                        }
                    }
                    emitter.send(SseEmitter.event().name("text").data(directReply));
                    saveHistory(hist, uid, finalSid, msg, directReply);
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                    return;
                }

                // 构建消息列表
                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(createMsg("system",
                        "你是Zayne餐厅的点餐助手。使用工具查询真实菜品数据，不要编造菜单外的菜品。\n"
                        + "推荐时介绍口味和价格。如果用户反馈'就这一个/还有吗'，理解上下文多推荐几个。"));
                if (hist != null) messages.addAll(hist);
                messages.add(createMsg("user", msg));

                List<Map<String, Object>> tools = dishTools.getToolDefinitions();
                boolean forceFirstRound = isForceToolIntent(intent);

                // 工具轮次（同步执行，速度很快）
                for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                    boolean forceTool = forceFirstRound && round == 0;
                    JSONObject response = zhiPuUtil.chatWithHistory(messages, tools, forceTool);
                    JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    messages.add(message);

                    String finishReason = choice.getString("finish_reason");
                    String content = message.getString("content");

                    if (!"tool_calls".equals(finishReason)) {
                        // AI 已生成完整文本，现在用流式重新输出
                        messages.remove(messages.size() - 1); // 去掉刚加的 assistant 消息
                        emitter.send(SseEmitter.event().name("status").data(""));

                        StringBuilder fullReply = new StringBuilder();
                        zhiPuUtil.chatStream(messages, null,
                            chunk -> {
                                fullReply.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event().name("text").data(chunk));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> {
                                String reply = fullReply.toString();
                                saveHistory(hist, uid, finalSid, msg,
                                        reply.isEmpty() && content != null ? content.trim() : reply);
                                try {
                                    emitter.send(SseEmitter.event().name("done").data(""));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.warn("SSE 完成通知失败", e);
                                }
                            },
                            err -> {
                                log.error("流式响应失败，使用同步结果兜底", err);
                                String fallback = content != null ? content.trim() : "";
                                saveHistory(hist, uid, finalSid, msg, fallback);
                                try {
                                    emitter.send(SseEmitter.event().name("text").data(fallback));
                                    emitter.send(SseEmitter.event().name("done").data(""));
                                    emitter.complete();
                                } catch (IOException e2) {
                                    emitter.completeWithError(err);
                                }
                            }
                        );
                        return;
                    }

                    // 执行工具调用
                    JSONArray toolCalls = message.getJSONArray("tool_calls");
                    if (toolCalls != null) {
                        for (int i = 0; i < toolCalls.size(); i++) {
                            JSONObject tc = toolCalls.getJSONObject(i);
                            String toolCallId = tc.getString("id");
                            String funcName = tc.getJSONObject("function").getString("name");
                            String args = tc.getJSONObject("function").getString("arguments");

                            String result;
                            try {
                                result = dishTools.execute(funcName, args);
                            } catch (Exception e) {
                                log.error("工具[{}]执行失败: {}", funcName, e.getMessage());
                                result = "{\"error\":\"" + e.getMessage() + "\"}";
                            }

                            Map<String, Object> toolMsg = new LinkedHashMap<>();
                            toolMsg.put("role", "tool");
                            toolMsg.put("tool_call_id", toolCallId);
                            toolMsg.put("content", result);
                            messages.add(toolMsg);
                        }
                    }
                }

                // 超时兜底
                emitter.send(SseEmitter.event().name("text").data("抱歉，我现在有点忙不过来，请稍后再试～"));
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();

            } catch (Exception e) {
                log.error("AI流式对话异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /** 保存对话历史 */
    private void saveHistory(List<Map<String, Object>> hist, Long uid, String sid,
                              String userMsg, String reply) {
        Map<String, Object> um = new LinkedHashMap<>();
        um.put("role", "user"); um.put("content", userMsg);
        hist.add(um);
        Map<String, Object> am = new LinkedHashMap<>();
        am.put("role", "assistant"); am.put("content", reply);
        hist.add(am);
        if (hist.size() > 20) hist = new ArrayList<>(hist.subList(hist.size() - 20, hist.size()));
        historyManager.saveHist(uid, sid, hist);
    }

    /** 需要强制调工具的意图（必须先查数据库才能回答） */
    private static boolean isForceToolIntent(IntentRouter.Intent intent) {
        return intent == IntentRouter.Intent.RECOMMEND
            || intent == IntentRouter.Intent.QUERY_CATEGORY
            || intent == IntentRouter.Intent.ADD_SPECIFIC;
    }

    /** Function Calling 主循环 */
    @SuppressWarnings("unchecked")
    private String chatWithTools(String userMsg, List<Map<String, Object>> history,
                                  IntentRouter.Intent intent) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(createMsg("system",
                "你是Zayne餐厅的点餐助手。使用工具查询真实菜品数据，不要编造菜单外的菜品。\n"
                + "推荐时介绍口味和价格。如果用户反馈'就这一个/还有吗'，理解上下文多推荐几个。"));

        if (history != null) {
            messages.addAll(history);
        }
        messages.add(createMsg("user", userMsg));

        List<Map<String, Object>> tools = dishTools.getToolDefinitions();
        boolean forceFirstRound = isForceToolIntent(intent);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            boolean forceTool = forceFirstRound && round == 0;
            JSONObject response = zhiPuUtil.chatWithHistory(messages, tools, forceTool);
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            messages.add(message); // 保存 AI 回复到历史

            String finishReason = choice.getString("finish_reason");
            String content = message.getString("content");

            // AI 直接回复文本
            if (!"tool_calls".equals(finishReason)) {
                return content != null ? content.trim() : "";
            }

            // 处理工具调用
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                return content != null ? content.trim() : "";
            }

            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject tc = toolCalls.getJSONObject(i);
                String toolCallId = tc.getString("id");
                String funcName = tc.getJSONObject("function").getString("name");
                String args = tc.getJSONObject("function").getString("arguments");

                String result;
                try {
                    result = dishTools.execute(funcName, args);
                } catch (Exception e) {
                    log.error("工具[{}]执行失败: {}", funcName, e.getMessage());
                    result = "{\"error\":\"" + e.getMessage() + "\"}";
                }

                // tool 角色消息
                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", toolCallId);
                toolMsg.put("content", result);
                messages.add(toolMsg);
            }
        }

        return "抱歉，我现在有点忙不过来，请稍后再试～";
    }

    private Map<String, Object> createMsg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    @Override
    public List<Map<String, Object>> loadChatHistory(Long userId, String sessionId) {
        List<Map<String, Object>> all = historyManager.loadHist(userId, sessionId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> m : all) {
            String role = (String) m.get("role");
            if ("user".equals(role) || "assistant".equals(role)) result.add(m);
        }
        return result;
    }

    private OrderSubmitVO submit(Long uid) {
        try {
            CartTools.CartSnapshot c = cartTools.queryCart(uid);
            if (c.getTotalCount() == 0) return null;
            OrdersSubmitDTO d = new OrdersSubmitDTO();
            d.setPayMethod(1); d.setDeliveryStatus(1);
            d.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(40));
            d.setAmount(c.getTotalAmount()); d.setPackAmount(c.getTotalCount());
            d.setTablewareNumber(c.getItems().size()); d.setTablewareStatus(0);
            d.setRemark("AI代下单");
            return orderService.submitOrder(d);
        } catch (Exception e) {
            log.error("AI下单失败", e);
            return null;
        }
    }
}
