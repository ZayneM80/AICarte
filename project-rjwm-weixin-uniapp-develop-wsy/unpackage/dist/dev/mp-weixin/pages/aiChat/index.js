try { var _app = getApp() } catch(e) {}
var _sessionId = ''
try { _sessionId = wx.getStorageSync('ai_session_id') || '' } catch(e) {}

Page({
  data: {
    inputMsg: '',
    messages: [],
    loading: false,
    sessionId: _sessionId,
    lastMsgId: 'msg-welcome',
    showQuickReplies: true,
    statusBarHeight: 20,
    sendBtnClass: 'disabled'
  },

  onLoad: function() {
    var that = this
    try {
      wx.getSystemInfo({
        success: function(res) {
          that.setData({ statusBarHeight: res.statusBarHeight || 20 })
        }
      })
    } catch(e) {}
  },

  onInput: function(e) {
    var val = e.detail.value
    var cls = (val && !this.data.loading) ? '' : 'disabled'
    this.setData({ inputMsg: val, sendBtnClass: cls })
  },

  sendMessage: function() {
    var msg = this.data.inputMsg
    if (!msg || this.data.loading) return

    this.setData({ showQuickReplies: false, inputMsg: '', loading: true, sendBtnClass: 'disabled' })

    var msgs = this.data.messages.slice()
    msgs.push({ role: 'user', content: msg })
    this.setData({ messages: msgs })

    var that = this
    var token = ''
    try {
      var app = getApp()
      if (app && app.$store) token = app.$store.state.token || ''
    } catch(e) {}

    wx.request({
      url: 'http://2ae29966.r10.cpolar.top/user/chat/send',
      method: 'POST',
      header: { 'Content-Type': 'application/json', 'authentication': token },
      data: { sessionId: this.data.sessionId, message: msg },
      success: function(r) {
        var d = r.data
        if (d && d.code === 1 && d.data) {
          var result = d.data
          if (result.sessionId) {
            that.data.sessionId = result.sessionId
            try { wx.setStorageSync('ai_session_id', result.sessionId) } catch(e) {}
          }
          var msgs2 = that.data.messages.slice()
          msgs2.push({ role: 'assistant', content: result.reply || '好的～' })
          that.setData({ messages: msgs2 })
          if (result.action === 'order_submitted' && result.orderNumber) {
            setTimeout(function() { that.payOrder(result) }, 500)
          }
        }
      },
      complete: function() {
        that.setData({ loading: false })
        if (that.data.inputMsg) {
          that.setData({ sendBtnClass: '' })
        }
      }
    })
  },

  payOrder: function(data) {
    wx.showModal({
      title: '订单已提交',
      content: '订单号：' + data.orderNumber + '\n金额：¥' + data.orderAmount + '\n去支付？',
      success: function(r) {
        if (r.confirm) {
          try {
            var app = getApp()
            if (app && app.$store) {
              app.$store.commit('setOrderData', {
                id: data.orderId || '', orderNumber: data.orderNumber,
                orderAmount: data.orderAmount, orderTime: new Date().toISOString()
              })
            }
          } catch(e) {}
          wx.navigateTo({ url: '/pages/pay/index?orderId=' + (data.orderId || data.orderNumber) })
        }
      }
    })
  },

  sendQuick: function(e) {
    this.setData({ inputMsg: e.currentTarget.dataset.text, sendBtnClass: '' })
    this.sendMessage()
  },

  goBack: function() {
    wx.navigateBack({ delta: 1 })
  }
})
