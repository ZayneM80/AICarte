package com.sky.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.ai.vector.VectorSearchService;
import com.sky.context.BaseContext;
import com.sky.entity.Dish;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.properties.DishMetaProperties;
import com.sky.service.impl.RecommendEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 可调用的菜品工具函数
 *
 * 这些函数会被定义为 tools 参数传给智谱 API，
 * AI 通过调用它们获取真实数据，避免幻觉
 */
@Component
@Slf4j
public class DishTools {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishMetaProperties dishMeta;

    @Autowired
    private RecommendEngine recommendEngine;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private CartTools cartTools;

    /**
     * 获取工具定义（JSON Schema 格式）
     */
    public List<Map<String, Object>> getToolDefinitions() {
        String json = "[{\"type\":\"function\",\"function\":{" +
            "\"name\":\"queryMenu\"," +
            "\"description\":\"查询菜单，按口味/辣度/类别/关键词筛选菜品\"," +
            "\"parameters\":{\"type\":\"object\",\"properties\":{" +
                "\"keyword\":{\"type\":\"string\",\"description\":\"搜索关键词，如菜名、口味\"}," +
                "\"taste\":{\"type\":\"string\",\"description\":\"口味筛选：麻辣/酸辣/咸鲜/清淡/甜/蒜香\"}," +
                "\"spicy\":{\"type\":\"boolean\",\"description\":\"true=只要辣的，false=要不辣的\"}," +
                "\"category\":{\"type\":\"string\",\"description\":\"类别筛选：主食/肉类/素菜/饮料/汤类\"}," +
                "\"limit\":{\"type\":\"integer\",\"description\":\"返回条数，默认5\"}" +
            "},\"required\":[]}" +
        "}},{\"type\":\"function\",\"function\":{" +
            "\"name\":\"addToCart\"," +
            "\"description\":\"将菜品加入购物车\"," +
            "\"parameters\":{\"type\":\"object\",\"properties\":{" +
                "\"dishName\":{\"type\":\"string\",\"description\":\"菜品名称\"}," +
                "\"quantity\":{\"type\":\"integer\",\"description\":\"数量，默认1\"}" +
            "},\"required\":[\"dishName\"]}" +
        "}},{\"type\":\"function\",\"function\":{" +
            "\"name\":\"getCart\"," +
            "\"description\":\"查看当前购物车内容\"," +
            "\"parameters\":{\"type\":\"object\",\"properties\":{},\"required\":[]}" +
        "}},{\"type\":\"function\",\"function\":{" +
            "\"name\":\"getAllDishes\"," +
            "\"description\":\"获取本店完整菜品列表\"," +
            "\"parameters\":{\"type\":\"object\",\"properties\":{},\"required\":[]}" +
        "}}]";
        return (List<Map<String, Object>>) (List<?>) JSONArray.parseArray(json, Map.class);
    }

    /**
     * 执行工具调用
     * @return JSON 字符串（工具执行结果）
     */
    public String execute(String functionName, String argumentsJson) {
        log.info("执行工具: {}({})", functionName, argumentsJson);
        JSONObject args = JSONObject.parseObject(argumentsJson);
        Long userId = BaseContext.getCurrentId();

        switch (functionName) {
            case "queryMenu":
                return queryMenu(
                    args.getString("keyword"),
                    args.getString("taste"),
                    args.getBoolean("spicy"),
                    args.getString("category"),
                    args.getIntValue("limit") > 0 ? args.getIntValue("limit") : 5
                );
            case "addToCart":
                return addToCart(userId,
                    args.getString("dishName"),
                    args.getIntValue("quantity") > 0 ? args.getIntValue("quantity") : 1
                );
            case "getCart":
                return getCart(userId);
            case "getAllDishes":
                return getAllDishes();
            default:
                return "{\"error\":\"未知工具: " + functionName + "\"}";
        }
    }

    /**
     * 查询菜单
     */
    private String queryMenu(String keyword, String taste, Boolean spicy, String category, int limit) {
        List<Dish> dishes = recommendEngine.getAvailableDishes();

        // 过滤
        if (category != null && !category.isEmpty()) {
            dishes = dishes.stream()
                .filter(d -> category.equals(dishMeta.getCategory(d.getName())))
                .collect(Collectors.toList());
        }
        if (taste != null && !taste.isEmpty()) {
            dishes = dishes.stream()
                .filter(d -> taste.equals(d.getTaste()) || taste.equals(dishMeta.getTaste(d.getName())))
                .collect(Collectors.toList());
        }
        if (spicy != null && spicy) {
            dishes = dishes.stream()
                .filter(d -> {
                    String s = d.getSpiciness() != null ? d.getSpiciness() : dishMeta.getSpiciness(d.getName());
                    return !"不辣".equals(s);
                })
                .collect(Collectors.toList());
        }
        if (spicy != null && !spicy) {
            dishes = dishes.stream()
                .filter(d -> {
                    String s = d.getSpiciness() != null ? d.getSpiciness() : dishMeta.getSpiciness(d.getName());
                    return "不辣".equals(s);
                })
                .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            // 先精确匹配菜名
            Set<String> matched = dishes.stream()
                .map(Dish::getName)
                .filter(name -> name.contains(keyword) || keyword.contains(name))
                .collect(Collectors.toSet());
            if (!matched.isEmpty()) {
                dishes = dishes.stream().filter(d -> matched.contains(d.getName())).collect(Collectors.toList());
            } else {
                // 无精确匹配 → 向量搜索
                return vectorSearchService.search(keyword, limit);
            }
        }

        // 限制条数
        dishes = dishes.stream().limit(limit).collect(Collectors.toList());

        // 格式化为 JSON
        JSONArray arr = new JSONArray();
        for (Dish d : dishes) {
            JSONObject item = new JSONObject();
            item.put("name", d.getName());
            item.put("price", d.getPrice());
            item.put("spiciness", d.getSpiciness() != null ? d.getSpiciness() : dishMeta.getSpiciness(d.getName()));
            item.put("taste", d.getTaste() != null ? d.getTaste() : dishMeta.getTaste(d.getName()));
            item.put("category", dishMeta.getCategory(d.getName()));
            item.put("description", d.getDescription() != null ? d.getDescription() : "");
            arr.add(item);
        }
        return arr.toJSONString();
    }

    /**
     * 加购
     */
    private String addToCart(Long userId, String dishName, int quantity) {
        if (userId == null) return "{\"error\":\"用户未登录\"}";

        Dish dish = recommendEngine.findDish(dishName);
        if (dish == null) {
            // 尝试模糊匹配
            String matched = recommendEngine.matchDish(dishName);
            if (matched != null) dish = recommendEngine.findDish(matched);
        }
        if (dish == null) {
            return "{\"error\":\"未找到菜品: " + dishName + "\",\"menu\":\"可用菜品列表见 getAllDishes\"}";
        }

        CartTools.AddResult result = cartTools.addToCart(userId, dish.getId(), null, quantity, true, null);
        JSONObject resp = new JSONObject();
        if (result != null && result.isSuccess()) {
            resp.put("success", true);
            resp.put("dishName", dish.getName());
            resp.put("quantity", result.getCount());
            resp.put("price", dish.getPrice());
            resp.put("message", "已加入购物车");
        } else {
            resp.put("success", false);
            resp.put("error", "加购失败");
        }
        return resp.toJSONString();
    }

    /**
     * 查看购物车
     */
    private String getCart(Long userId) {
        if (userId == null) return "{\"error\":\"用户未登录\"}";

        CartTools.CartSnapshot cart = cartTools.queryCart(userId);
        JSONObject resp = new JSONObject();
        resp.put("totalCount", cart.getTotalCount());
        resp.put("totalAmount", cart.getTotalAmount());

        JSONArray items = new JSONArray();
        for (CartTools.CartItem ci : cart.getItems()) {
            JSONObject item = new JSONObject();
            item.put("name", ci.getName());
            item.put("count", ci.getCount());
            item.put("price", ci.getPrice());
            items.add(item);
        }
        resp.put("items", items);
        return resp.toJSONString();
    }

    /**
     * 获取完整菜单
     */
    private String getAllDishes() {
        List<Dish> dishes = recommendEngine.getAvailableDishes();
        JSONArray arr = new JSONArray();
        for (Dish d : dishes) {
            JSONObject item = new JSONObject();
            item.put("name", d.getName());
            item.put("price", d.getPrice());
            item.put("spiciness", d.getSpiciness() != null ? d.getSpiciness() : dishMeta.getSpiciness(d.getName()));
            item.put("taste", d.getTaste() != null ? d.getTaste() : dishMeta.getTaste(d.getName()));
            item.put("category", dishMeta.getCategory(d.getName()));
            item.put("description", d.getDescription() != null ? d.getDescription() : "");
            arr.add(item);
        }
        return arr.toJSONString();
    }
}
