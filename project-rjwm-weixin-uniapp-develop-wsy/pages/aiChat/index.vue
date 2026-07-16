<template>
  <view>
    <!-- 导航 -->
    <uni-nav-bar
      @clickLeft="goBack"
      left-icon="back"
      leftIcon="arrowleft"
      title="AI点餐助手"
      statusBar="true"
      fixed="true"
      color="#ffffff"
      backgroundColor="#E95F3C"
    ></uni-nav-bar>
    <!-- end -->

    <!-- 消息列表 -->
    <scroll-view
      class="chat-list"
      scroll-y="true"
      :style="'top:' + chatTop + 'px'"
      :scroll-top="scrollTop"
      :scroll-into-view="lastMsgId"
      @scroll="onScroll"
    >
      <!-- 欢迎消息 -->
      <view class="msg-row ai-msg" id="msg-welcome">
        <view class="avatar ai-avatar">
          <image src="/static/logo_ruiji.png"></image>
        </view>
        <view class="bubble ai-bubble">
          你好！我是AI点餐助手 🎉 我可以帮你：
          \n• 推荐菜品 — 告诉我你的口味和预算
          \n• 直接点餐 — 说"来份麻婆豆腐"
          \n• 查看购物车 — 说"购物车有什么"
          \n• 下单结账 — 说"我要下单"
          \n\n你想吃什么？
        </view>
      </view>

      <!-- 对话消息 -->
      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        :id="'msg-' + idx"
        class="msg-row"
        :class="msg.role === 'user' ? 'user-msg' : 'ai-msg'"
      >
        <view class="avatar" :class="msg.role === 'user' ? 'user-avatar' : 'ai-avatar'">
          <image v-if="msg.role === 'user'" src="/static/btn_waiter_sel.png"></image>
          <image v-else src="/static/logo_ruiji.png"></image>
        </view>
        <view class="bubble" :class="msg.role === 'user' ? 'user-bubble' : 'ai-bubble'">
          <text>{{ msg.content }}</text>
        </view>
      </view>

      <!-- 加载中 -->
      <view class="msg-row ai-msg" v-if="loading" id="msg-loading">
        <view class="avatar ai-avatar">
          <image src="/static/logo_ruiji.png"></image>
        </view>
        <view class="bubble ai-bubble loading-bubble">
          <text class="dot-pulse">...</text>
        </view>
      </view>

      <view style="height: 20rpx;"></view>
    </scroll-view>

    <!-- 快捷推荐按钮 -->
    <scroll-view
      class="quick-reply-row"
      scroll-x="true"
      v-if="showQuickReplies && !loading"
    >
      <view class="quick-tag" @click="sendQuick('给我推荐几道菜')">🍽 推荐菜品</view>
      <view class="quick-tag" @click="sendQuick('看看购物车')">🛒 购物车</view>
      <view class="quick-tag" @click="sendQuick('我要下单')">📦 下单</view>
      <view class="quick-tag" @click="sendQuick('有什么辣的菜')">🌶 辣菜</view>
      <view class="quick-tag" @click="sendQuick('有什么甜的菜')">🍬 甜口</view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area" :class="{ 'input-area-safe': hasSafeArea }">
      <view class="input-box">
        <input
          class="text-input"
          v-model="inputMsg"
          @confirm="sendMessage"
          :adjust-position="false"
          :focus="inputFocus"
          placeholder="输入你想吃的菜品..."
          placeholder-style="color: #999;"
          confirm-type="send"
        />
      </view>
      <view class="send-btn" @click="sendMessage" :class="{ disabled: loading || !inputMsg.trim() }">
        <text>发送</text>
      </view>
    </view>
  </view>
</template>

<script src="./index.js"></script>

<style scoped>
.chat-list {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 200rpx;
  padding: 24rpx 20rpx 0;
  background: #f5f5f5;
  box-sizing: border-box;
}

.msg-row {
  display: flex;
  margin-bottom: 24rpx;
  align-items: flex-start;
}

.user-msg {
  flex-direction: row-reverse;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar image {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
}

.user-avatar {
  margin-left: 16rpx;
}

.ai-avatar {
  margin-right: 16rpx;
}

.bubble {
  max-width: 520rpx;
  padding: 20rpx 28rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.user-bubble {
  background: #ffc200;
  color: #333;
  border-bottom-right-radius: 4rpx;
}

.ai-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
}

.loading-bubble {
  min-width: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dot-pulse {
  font-size: 48rpx;
  letter-spacing: 4rpx;
  color: #999;
}

/* 快捷回复 */
.quick-reply-row {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 108rpx;
  height: 88rpx;
  background: #f5f5f5;
  padding: 8rpx 20rpx;
  white-space: nowrap;
  box-sizing: border-box;
  z-index: 10;
}

.quick-tag {
  display: inline-block;
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 24rpx;
  margin-right: 16rpx;
  background: #fff;
  border: 1rpx solid #ffc200;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: #E95F3C;
  vertical-align: middle;
}

/* 输入区域 */
.input-area {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 108rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
  box-sizing: border-box;
  z-index: 100;
}

.input-area-safe {
  padding-bottom: 40rpx;
}

.input-box {
  flex: 1;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
}

.text-input {
  flex: 1;
  height: 60rpx;
  font-size: 28rpx;
  color: #333;
}

.send-btn {
  width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: linear-gradient(135deg, #ffc200, #E95F3C);
  color: #fff;
  border-radius: 36rpx;
  margin-left: 16rpx;
  font-size: 28rpx;
  font-weight: 500;
}

.send-btn.disabled {
  opacity: 0.5;
}
</style>
