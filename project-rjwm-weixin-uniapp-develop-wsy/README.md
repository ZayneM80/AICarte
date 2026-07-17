# zayne 外卖小程序

基于 uni-app 开发的微信外卖小程序，配套 Java（Spring Boot）后端。

## 目录结构

```
├── pages/              # 页面文件
│   ├── index/          # 首页（点餐）
│   ├── order/          # 下单页
│   ├── pay/            # 支付页
│   ├── details/        # 订单详情
│   ├── historyOrder/   # 历史订单
│   ├── aiChat/         # AI 点餐助手
│   ├── address/        # 地址管理
│   ├── my/             # 个人中心
│   └── ...
├── utils/              # 工具函数
│   ├── env.js          # 【需配置】后端地址
│   ├── request.js      # HTTP 请求封装
│   └── webscoket.js    # 【需配置】WebSocket / MQ 连接
├── store/              # Vuex 状态管理
├── static/             # 静态资源
├── manifest.json       # 【需配置】微信 AppID
├── pages.json          # 页面路由配置
└── project.config.json # 【需配置】微信 AppID
```

## 环境要求

- [HBuilderX](https://www.dcloud.io/hbuilderx.html)（运行 uni-app）
- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
- Java 后端（sky-take-out-ai）
- Redis
- MySQL

## 配置清单（部署前必改）

以下文件包含占位符，需要替换为你的实际值：

### 1. 后端地址

`utils/env.js`
```js
export const baseUrl = 'http://YOUR_SERVER_IP:8080'
```
- 本地开发：改为 `http://192.168.x.x:8080`（你电脑的局域网IP）
- 服务器部署：改为你的公网 IP 或域名

### 2. 微信 AppID

`manifest.json` 和 `project.config.json`
```
YOUR_WECHAT_APPID → 你小程序的 AppID
```
> 在 [微信公众平台](https://mp.weixin.qq.com) → 开发管理 → 复制 AppID

### 3. WebSocket 地址和 MQ 凭据

`utils/webscoket.js`
```js
url: 'wss://YOUR_WEBSOCKET_SERVER/ws'
client.connect('YOUR_MQ_USER', 'YOUR_MQ_PASSWORD', ...)
```
- WebSocket 地址由后端提供
- MQ 用户/密码由后端 RabbitMQ/ActiveMQ 配置决定

### 4. 店铺/餐桌 ID

`pages/index/index.js`
```js
params: {
    shopId: "YOUR_SHOP_ID",
    storeId: "YOUR_STORE_ID",
    tableId: "YOUR_TABLE_ID",
}
```
> 这些 ID 由后端数据库生成，首次运行时从后端获取

## 运行步骤

### 1. 启动后端

```bash
# 进入后端项目目录，启动 Docker 服务
docker-compose up -d mysql redis

# 启动 Spring Boot 应用（sky-server）
```

### 2. 修改前端配置

按上方「配置清单」替换所有占位符。

### 3. 在 HBuilderX 中运行

1. 用 HBuilderX 打开本项目
2. 菜单栏 → 运行 → 运行到小程序模拟器 → 微信开发者工具
3. 微信开发者工具 → 详情 → 本地设置 → 勾选「不校验合法域名」

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端框架 | uni-app (Vue 2) |
| 后端 | Spring Boot + MyBatis-Plus |
| 数据库 | MySQL + Redis |
| 消息推送 | WebSocket + Stomp |
| AI | 智谱 GLM-4V |
| 文件存储 | MinIO |
