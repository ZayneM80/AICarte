package com.sky.ai;

import com.sky.service.impl.RecommendEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 意图路由（规则引擎，零 LLM 调用）
 *
 * 第一层：正则匹配 → 快速分类
 * 第二层：LLM 回复（仅 CHAT 及复杂场景）
 *
 * 优先级：CHECKOUT > CONFIRM > ADD_SPECIFIC > RECOMMEND > QUERY > ANGRY > CHAT
 */
@Component
public class IntentRouter {

    @Autowired
    private RecommendEngine recommendEngine;

    // 确认指令——覆盖所有口语化确认表达
    public static final Pattern CONFIRM = Pattern.compile(
        "^(好|行|可以|ok|OK|嗯|对|是的|买|买上|买了|要了|需要|就要这些|就这些|就它|就他们|加|加上|加上吧|可以了|够了|确认|确定|下单吧|搞|整|中|行吧|好吧|成|没问题|听你的|按你说的|okok|没错|对的|是的呀|嗯嗯|每道|各来|各一|一样一|全要|全都要|全部|都行|都可|随便来).*"
        + "|.*(来一份吧|来一个吧|各一份|各来一|一样来一|每样来一|每道来一|全部加上|这几道都要|全都要了|都加上|都加进去|加进去吧|一样一份|每样一份|各来一份|一样一个|每样一个|这些都要|这几个都要).*"
    );

    // 下单指令
    public static final Pattern CHECKOUT = Pattern.compile(".*(下单|结账|支付|提交订单|买单|结算).*");

    // 查询指令
    public static final Pattern QUERY_CART = Pattern.compile(".*(购物车|看一下|查|看看|还有啥|几个|多少).*");
    public static final Pattern QUERY_CAT  = Pattern.compile(".*(主食|面|饭|辣菜|饮料|喝的|素菜|酒|下酒|甜品).*");

    // 推荐/点菜意图
    public static final Pattern RECOMMEND = Pattern.compile(".*(推荐|来点|随便|想吃|点菜|帮我选|搭配|辣|不辣|有什么|主食|饮料|来个|给我来).*");

    // 增量加购（消息含菜名+数量+加购词）
    public static final Pattern ADD_SPECIFIC = Pattern.compile(".*([一二两三四五六七八九]|加|来|要|点|再).*(瓶|碗|份|个|罐|杯).*");

    // 用户质疑/生气
    public static final Pattern ANGRY = Pattern.compile(".*(骗|不对|错了|少了|多了|没有|假的|胡|乱|坑|骗人|什么鬼|扯|搞什么|怎么|为什么|咋|哪有|哪去了|没看到).*");

    /** 消息 → 意图分类 */
    public Intent classify(String msg) {
        if (CHECKOUT.matcher(msg).find()) return Intent.CHECKOUT;
        if (CONFIRM.matcher(msg).matches()) return Intent.CONFIRM;
        if (ADD_SPECIFIC.matcher(msg).find()) return Intent.ADD_SPECIFIC;
        if (RECOMMEND.matcher(msg).find()) return Intent.RECOMMEND;
        if (QUERY_CART.matcher(msg).find()) return Intent.QUERY_CART;
        if (QUERY_CAT.matcher(msg).find() && !hasDishName(msg)) return Intent.QUERY_CATEGORY;
        // "有没有XX" → 走 CHAT（用 AI + 真实菜单回答）
        if (msg.contains("有没有")) return Intent.CHAT;
        if (ANGRY.matcher(msg).find()) return Intent.ANGRY;
        return Intent.CHAT;
    }

    /**
     * 检查消息是否包含已知菜品名
     * 若包含 → 不归为类别查询，走通用 CHAT 兜底
     */
    private boolean hasDishName(String msg) {
        return recommendEngine.matchDish(msg) != null;
    }

    public enum Intent {
        CHECKOUT, CONFIRM, ADD_SPECIFIC, QUERY_CART,
        QUERY_CATEGORY, RECOMMEND, ANGRY, CHAT
    }
}
