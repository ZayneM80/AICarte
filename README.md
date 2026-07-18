# 🍽️ Sky Take-out AI — Zayne 餐厅智能点餐系统

基于 Spring Boot + 智谱 AI 的智能点餐微信小程序后端，支持 **AI 对话点餐、RAG 菜品检索、Function Calling 工具调用**。

---

## ✨ 功能亮点

### AI 点餐助手
- **自然语言点餐** — "推荐几个菜"、"有什么辣的"、"来份老坛酸菜鱼"
- **Function Calling** — AI 主动调工具查数据库，**零幻觉**
- **顺序对话** — "推荐辣的" → "就这一个啊" → "这不都是饮料吗"，AI 理解上下文
- **流式输出（SSE）** — AI 回复逐字显示，不等不卡
- **智能下单** — 说"我要下单"直接提交订单，跳转支付
- **购物车管理** — "看看购物车"、"加一份回锅肉"

### 经典功能
- 菜品浏览、分类筛选、购物车 CRUD
- 下单、支付（模拟）、订单历史、催单、再来一单
- 管理端：菜品管理、订单管理、营业数据统计
- WebSocket 实时订单通知

---

## 🏗️ 架构

```
┌─────────────┐     ┌──────────────────────────────────────┐
│ 微信小程序   │────▶│  Spring Boot 2.7 (sky-server)        │
│ (uniapp)     │     │                                      │
│             │     │  ┌──────────┐  ┌──────────────────┐  │
│             │     │  │ 控制器层  │─▶│ AIAssistantService│  │
│             │     │  └──────────┘  └──────┬───────────┘  │
│             │     │                      │               │
│             │     │  ┌───────────────────▼────────────┐  │
│             │     │  │   IntentRouter (规则路由)        │  │
│             │     │  │   CHECKOUT/CONFIRM → 直接处理   │  │
│             │     │  │   RECOMMEND/QUERY → Function    │  │
│             │     │  │   Calling 循环                   │  │
│             │     │  └────────────────┬───────────────┘  │
│             │     │                   │                  │
│             │     │  ┌────────────────▼───────────────┐  │
│             │     │  │   DishTools (工具函数)           │  │
│             │     │  │   queryMenu / addToCart /       │  │
│             │     │  │   getCart / getAllDishes         │  │
│             │     │  └────────────────┬───────────────┘  │
│             │     │                   │                  │
│             │     │  ┌────────────────▼───────────────┐  │
│             │     │  │   数据层                        │  │
│             │     │  │   MySQL ← 业务数据              │  │
│             │     │  │   pgvector ← 菜品向量检索(RAG)   │  │
│             │     │  │   Redis ← 对话历史 + 缓存       │  │
│             │     │  └────────────────────────────────┘  │
│             │     │                                      │
│             │     │  ┌────────────────────────────────┐  │
│             │     │  │   智谱 AI GLM-4                 │  │
│             │     │  │   chat/completions (对话+工具)   │  │
│             │     │  │   text_embedding_v2 (向量)      │  │
│             │     │  └────────────────────────────────┘  │
└─────────────┘     └──────────────────────────────────────┘
```

### AI 对话流程

```
用户: "推荐几个菜"
  → IntentRouter 分类为 RECOMMEND
  → 第一轮 tool_choice="required" 强制调工具
  → AI 调用 getAllDishes() 查 MySQL → 拿到 24 道真实菜品
  → 第二轮 tool_choice="auto" 生成回复
  → SSE 流式返回"我推荐您尝尝..."
```

---

## 🛠️ 技术栈

| 层级 | 技术 |
|---|---|
| 后端框架 | Spring Boot 2.7.3 / MyBatis-Plus 3.5.2 |
| AI 引擎 | 智谱 GLM-4 (OpenAI 兼容接口) |
| 向量检索 | PostgreSQL 16 + pgvector (IVFFlat 索引) |
| 对话记忆 | Redis (30min TTL, 20 条滑动窗口) |
| 数据库 | MySQL 8 + PostgreSQL 16 |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 部署 | Docker Compose |

---

## 🚀 快速启动

### 前置条件
- JDK 11+
- Docker & Docker Compose
- 智谱 AI API Key（[开放平台](https://open.bigmodel.cn)申请）

### 1. 启动中间件

```bash
docker-compose up -d mysql redis postgres minio
```

### 2. 配置 API Key

编辑 `sky-server/src/main/resources/application-dev.yml`：

```yaml
sky:
  ai:
    zhi-pu:
      api-key: your-zhipu-api-key-here
```

> ⚠️ `application-dev.yml` 已在 `.gitignore`，不会提交到 GitHub。

### 3. 启动应用

用 IntelliJ 打开，运行 `SkyApplication.java`，或：

```bash
./mvnw spring-boot:run -pl sky-server
```

### 4. 访问

| 地址 | 说明 |
|---|---|
| `http://localhost:8080/doc.html` | Swagger 接口文档 |
| `http://localhost:8080/user/chat/stream?message=推荐几个菜` | AI 流式对话 |

---

## 📡 API 端点

### AI 点餐

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/chat/send` | 同步对话 |
| GET | `/user/chat/stream` | 流式对话（SSE） |
| GET | `/user/chat/history` | 加载历史 |

### 用户端

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/user/login` | 微信登录 |
| POST | `/user/shoppingCart` | 加购 |
| GET | `/user/shoppingCart/list` | 购物车列表 |
| POST | `/user/order/submit` | 提交订单 |
| GET | `/user/order/userPage` | 订单历史 |

详见 Swagger: `http://localhost:8080/doc.html`

---

## 🧩 项目结构

```
sky-take-out-ai/
├── sky-common/          # 公共工具类
├── sky-pojo/            # 实体/DTO/VO
│   ├── entity/          # 数据库实体
│   ├── dto/             # 请求参数
│   └── vo/              # 返回结果
├── sky-server/          # 后端服务
│   └── src/main/java/com/sky/
│       ├── ai/          # AI 核心
│       │   ├── client/  # ChatClient 抽象
│       │   ├── vector/  # 向量检索 (RAG)
│       │   ├── DishTools.java     # 工具函数
│       │   ├── CartTools.java     # 购物车工具
│       │   └── IntentRouter.java  # 意图路由
│       ├── controller/  # 控制器
│       ├── service/     # 业务层
│       ├── mapper/      # MyBatis 映射
│       ├── properties/  # 配置类
│       └── utils/       # 工具类 (ZhiPuUtil)
├── db/                  # SQL 初始化脚本
├── docker-compose.yml   # 容器编排
└── Dockerfile           # 应用镜像
```

---

## 💡 面试亮点

| 知识点 | 项目体现 |
|---|---|
| **Function Calling** | 工具定义 → AI 主动调 → 执行 → 结果回传，多轮循环 |
| **RAG 检索增强** | 用户 query → Embedding → pgvector 余弦搜索 → 注入 LLM |
| **ChatMemory** | 对话历史隔离（userId+sessionId）、滑动窗口截断、下单自动清 |
| **意图路由** | 规则引擎分类 → CHECKOUT/CONFIRM 走捷径 → 其他走 AI |
| **SSE 流式** | 工具轮次同步（快）→ 最终生成流式输出，不等不卡 |
| **强制调工具** | `tool_choice: "required"` 针对 RECOMMEND/QUERY 意图 |

---

## 📄 License

MIT
