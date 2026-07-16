import { chatWithAI } from "../api/api.js"
import { mapMutations } from "vuex"

export default {
	data() {
		return {
			inputMsg: '',
			messages: [],
			loading: false,
			sessionId: '',
			lastMsgId: '',
			scrollTop: 0,
			inputFocus: false,
			showQuickReplies: true,
			hasSafeArea: false,
			chatTop: 0,
		}
	},
	onLoad() {
		uni.getSystemInfo({
			success: (res) => {
				const statusBarH = res.statusBarHeight || 20
				this.chatTop = statusBarH + 44 // status bar + nav bar
				this.hasSafeArea = res.safeAreaInsets ? res.safeAreaInsets.bottom > 0 : false
			}
		})

		const saved = uni.getStorageSync('ai_session_id')
		if (saved) {
			this.sessionId = saved
		}
	},
	onShow() {
		this.scrollToBottom()
	},
	methods: {
		...mapMutations(['setOrderData']),

		// 发送消息
		async sendMessage() {
			const msg = this.inputMsg.trim()
			if (!msg || this.loading) return

			this.showQuickReplies = false
			this.inputMsg = ''
			this.inputFocus = false

			// 添加用户消息
			this.messages.push({ role: 'user', content: msg })
			this.scrollToBottom()
			this.loading = true

			try {
				const params = {
					sessionId: this.sessionId,
					message: msg,
				}

				const res = await chatWithAI(params)
				if (res && res.code === 1) {
					const data = res.data
					// 保存 sessionId
					if (data.sessionId) {
						this.sessionId = data.sessionId
						uni.setStorageSync('ai_session_id', data.sessionId)
					}

					// 添加 AI 回复
					let reply = data.reply || '好的，已处理～'
					this.messages.push({ role: 'assistant', content: reply })
					this.scrollToBottom()

					// 处理下单动作
					if (data.action === 'order_submitted' && data.orderNumber) {
						this.handleOrderSubmit(data)
					}
				} else {
					this.messages.push({
						role: 'assistant',
						content: '抱歉，我暂时无法处理，请稍后再试～'
					})
					this.scrollToBottom()
				}
			} catch (e) {
				console.error('AI chat error:', e)
				this.messages.push({
					role: 'assistant',
					content: '网络开小差了，请稍后再试～'
				})
				this.scrollToBottom()
			} finally {
				this.loading = false
			}
		},

		// 处理订单提交 - 跳转到支付
		handleOrderSubmit(data) {
			const orderData = {
				id: data.orderId || '',
				orderNumber: data.orderNumber,
				orderAmount: data.orderAmount,
				orderTime: new Date().toISOString(),
			}
			this.setOrderData(orderData)

			setTimeout(() => {
				uni.showModal({
					title: '订单已提交',
					content: `订单号：${data.orderNumber}\n金额：¥${data.orderAmount}\n\n是否前往支付？`,
					success: (res) => {
						if (res.confirm) {
							uni.navigateTo({
								url: `/pages/pay/index?orderId=${data.orderId || data.orderNumber}`
							})
						}
					}
				})
			}, 800)
		},

		// 发送快捷回复
		sendQuick(text) {
			this.inputMsg = text
			this.sendMessage()
		},

		// 滚动到底部
		scrollToBottom() {
			this.$nextTick(() => {
				const len = this.messages.length
				this.lastMsgId = len > 0 ? 'msg-' + (len - 1) : 'msg-welcome'
			})
		},

		onScroll(e) {},

		// 返回上一页
		goBack() {
			uni.navigateBack({ delta: 1 })
		},
	}
}
