<template>
	<view class="page-wrap">
		<view class="profile-card">
			<image class="avatar" src="/static/images/brand-icon.jpg" mode="aspectFill" />
			<view class="profile-main">
				<view class="profile-name">{{ displayName }}</view>
				<view class="profile-desc">登录后可查看预约、卡项权益、签到记录，并通过微信授权绑定手机号。</view>
			</view>
		</view>

		<view class="stats-grid">
			<view class="stats-item" v-for="item in stats" :key="item.label">
				<text class="stats-value">{{ item.value }}</text>
				<text class="stats-label">{{ item.label }}</text>
			</view>
		</view>

		<view v-if="summaryError" class="error-tip" @tap="loadSummary">{{ summaryError }}，点击重试</view>

		<view class="section-card bind-card">
			<view>
				<view class="menu-title">手机号绑定</view>
				<view class="menu-desc">当前手机号：{{ summary.mobile || '未绑定' }}</view>
			</view>
			<!-- #ifdef MP-WEIXIN -->
			<button
				class="bind-btn"
				:disabled="phoneBinding || loading"
				open-type="getPhoneNumber"
				@getphonenumber="onGetPhoneNumber"
			>
				{{ phoneBinding ? '绑定中...' : (summary.mobile ? '重新绑定' : '微信绑定') }}
			</button>
			<!-- #endif -->
			<!-- #ifndef MP-WEIXIN -->
			<button class="bind-btn" :disabled="phoneBinding || loading" @tap="showWechatOnlyTip">微信绑定</button>
			<!-- #endif -->
		</view>

		<view class="menu-card" v-for="item in menuList" :key="item.title" @tap="handleMenu(item)">
			<view>
				<view class="menu-title">{{ item.title }}</view>
				<view class="menu-desc">{{ item.desc }}</view>
			</view>
			<text class="menu-arrow">›</text>
		</view>
	</view>
</template>

<script>
	import { memberBindPhone, memberMineSummary } from '../../api/auth'
	import { getMemberSession, saveMemberSession } from '../../utils/auth'

	export default {
		data() {
			return {
				loading: false,
				phoneBinding: false,
				summaryError: '',
				summary: {},
				menuList: [
					{ title: '我的资料', desc: '查看昵称、手机号与个人信息', path: '/pages/mine/profile' },
					{ title: '我的预约', desc: '查看课程预约、取消和签到状态', path: '/pages/course/index?tab=reservation' },
					{ title: '我的权益', desc: '查看次卡、月卡、私教课包与余额', path: '/pages/course/index?tab=card' },
					{ title: '我的订单', desc: '查看购卡订单与支付状态', path: '/pages/course/index?tab=order' }
				]
			}
		},
		computed: {
			displayName() {
				return this.summary.realName || this.summary.nickname || 'T1 会员'
			},
			stats() {
				return [
					{ label: '我的预约', value: this.summary.totalReservationCount || 0 },
					{ label: '可用卡项', value: this.summary.activeCardCount || 0 },
					{ label: '待确认', value: this.summary.pendingConfirmCount || 0 }
				]
			}
		},
		onShow() {
			this.loadSummary()
		},
		methods: {
			async loadSummary() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.summaryError = ''
				try {
					this.summary = (await memberMineSummary()) || {}
					const session = getMemberSession()
					saveMemberSession({
						...session,
						memberInfo: {
							...(session.memberInfo || {}),
							mobile: this.summary.mobile,
							realName: this.summary.realName,
							nickname: this.summary.nickname,
							avatarUrl: this.summary.avatarUrl
						}
					})
				} catch (error) {
					this.summaryError = error.message || '加载我的信息失败'
					uni.showToast({ title: this.summaryError, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			handleMenu(item) {
				if (item.path) {
					uni.navigateTo({ url: item.path })
					return
				}
				uni.showToast({ title: '功能完善中', icon: 'none' })
			},
			showWechatOnlyTip() {
				uni.showToast({ title: '请在微信小程序内完成手机号绑定', icon: 'none' })
			},
			async onGetPhoneNumber(event) {
				if (this.phoneBinding) {
					return
				}
				const detail = (event && event.detail) || {}
				if (detail.errMsg !== 'getPhoneNumber:ok' || !detail.code) {
					const message = String(detail.errMsg || '')
					if (/privacy agreement|errno:112/i.test(message)) {
						uni.showModal({ title: '手机号授权失败', content: '小程序后台未配置手机号隐私声明，请检查微信公众平台配置。', showCancel: false })
					} else if (/no permission/i.test(message)) {
						uni.showModal({ title: '当前环境无法获取手机号', content: '当前小程序环境未获得手机号能力，请切换到已开通手机号能力的正式/体验环境或真机后再试。', showCancel: false })
					} else {
						uni.showModal({ title: '手机号授权失败', content: message || '未拿到手机号授权 code，请在真机小程序中重试。', showCancel: false })
					}
					return
				}
				this.phoneBinding = true
				try {
					uni.showLoading({ title: '绑定中...' })
					await memberBindPhone({ phoneCode: detail.code })
					await this.loadSummary()
					uni.showToast({ title: '手机号绑定成功', icon: 'success' })
				} catch (error) {
					uni.showToast({ title: error.message || '手机号绑定失败', icon: 'none' })
				} finally {
					this.phoneBinding = false
					uni.hideLoading()
				}
			}
		}
	}
</script>

<style lang="scss">
	.page-wrap {
		padding: 24rpx;
	}

	.profile-card,
	.menu-card,
	.section-card {
		padding: 28rpx;
		background: #fff;
		border-radius: 28rpx;
		box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05);
	}

	.profile-card {
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.avatar {
		width: 112rpx;
		height: 112rpx;
		border-radius: 28rpx;
	}

	.profile-main {
		flex: 1;
	}

	.profile-name {
		font-size: 32rpx;
		font-weight: 700;
	}

	.profile-desc,
	.menu-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.stats-grid {
		margin: 24rpx 0;
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 18rpx;
	}

	.stats-item {
		padding: 24rpx 12rpx;
		border-radius: 24rpx;
		background: linear-gradient(180deg, #fff4ec 0%, #ffffff 100%);
		text-align: center;
	}

	.stats-value {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ff5a36;
	}

	.stats-label {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #4b5563;
	}

	.error-tip {
		margin-bottom: 18rpx;
		padding: 20rpx;
		border-radius: 20rpx;
		font-size: 24rpx;
		color: #dc2626;
		background: #fef2f2;
	}

	.bind-card,
	.menu-card {
		margin-bottom: 18rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 20rpx;
	}

	.menu-title {
		font-size: 28rpx;
		font-weight: 700;
	}

	.bind-btn {
		margin: 0;
		padding: 0 28rpx;
		height: 76rpx;
		line-height: 76rpx;
		border-radius: 999rpx;
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
		color: #fff;
		font-size: 24rpx;
		font-weight: 600;
	}

	.bind-btn[disabled] {
		opacity: 0.7;
	}

	.bind-btn::after {
		border: none;
	}

	.menu-arrow {
		font-size: 40rpx;
		color: #9ca3af;
	}
</style>
