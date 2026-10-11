<template>
	<view class="home-page">
		<view class="hero-card">
			<view class="hero-top">
				<view>
					<text class="hero-badge">会员已登录</text>
					<view class="hero-title">你好，{{ displayName }}</view>
					<view class="hero-desc">今日训练安排、会员卡状态和推荐课程已同步到首页。</view>
				</view>
				<view class="hero-avatar">{{ displayName.slice(0, 1) }}</view>
			</view>
			<view class="hero-meta">
				<text class="meta-chip">会员号 {{ memberInfo.memberNo || '--' }}</text>
				<text class="meta-chip">{{ memberInfo.mobile || '未绑定手机号' }}</text>
			</view>
		</view>

		<view class="stats-grid">
			<view class="stats-card" v-for="item in stats" :key="item.label">
				<text class="stats-value">{{ item.value }}</text>
				<text class="stats-label">{{ item.label }}</text>
			</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">快捷入口</text>
				<text class="section-more">会员操作</text>
			</view>
			<view class="quick-grid">
				<view class="quick-item" v-for="item in quickList" :key="item.title" @tap="goPage(item.url)">
					<text class="quick-icon">{{ item.icon }}</text>
					<text class="quick-title">{{ item.title }}</text>
					<text class="quick-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view v-if="bannerList.length" class="section-card">
			<view class="section-head">
				<text class="section-title">推荐场景</text>
				<text class="section-more">门店与课程精选</text>
			</view>
			<swiper class="banner-swiper" circular indicator-dots autoplay interval="3600" duration="500">
				<swiper-item v-for="(item, index) in bannerList" :key="index">
					<image class="banner-image" :src="item" mode="aspectFill" />
				</swiper-item>
			</swiper>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">待上课预约</text>
				<text class="section-more">{{ upcomingList.length }} 条</text>
			</view>
			<view v-if="upcomingList.length" class="info-list">
				<view class="info-item" v-for="item in upcomingList" :key="item.reservationId">
					<view class="info-main">
						<text class="info-title">{{ item.courseName || '待上课课程' }}</text>
						<text class="info-desc">{{ item.storeName || '门店待定' }} · 教练：{{ item.coachName || '待分配' }}</text>
					</view>
					<text class="info-side">{{ formatSchedule(item.scheduleDate, item.startTime) }}</text>
				</view>
			</view>
			<view v-else class="empty-state">今日暂无待上课预约，去活动专区看看吧。</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">我的卡项</text>
				<text class="section-more">{{ cardList.length }} 张</text>
			</view>
			<view v-if="cardList.length" class="card-list">
				<view class="member-card" v-for="item in cardList" :key="item.cardId">
					<view>
						<text class="info-title">{{ item.cardName || '会员卡' }}</text>
						<text class="info-desc">绑定教练：{{ item.coachName || '未绑定' }}</text>
					</view>
					<view class="card-side">
						<text class="card-highlight">{{ formatCardRemain(item) }}</text>
						<text class="card-desc">{{ formatExpire(item.expireTime) }}</text>
					</view>
				</view>
			</view>
			<view v-else class="empty-state">当前还没有激活卡项，可先浏览活动或购卡方案。</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">推荐购卡</text>
				<text class="section-more" @tap="goPage('/pages/activity/index')">查看更多</text>
			</view>
			<view v-if="promotionList.length" class="promotion-list">
				<view class="promotion-card" v-for="item in promotionList" :key="item.planId">
					<view class="promotion-top">
						<text class="promotion-tag">{{ item.planType || '卡项' }}</text>
						<text class="promotion-price">¥{{ formatAmount(item.salePrice) }}</text>
					</view>
					<text class="promotion-title">{{ item.planName }}</text>
					<text class="promotion-desc">{{ item.storeName || '全门店' }} · {{ item.supportCourseType || '通用课程' }}</text>
				</view>
			</view>
			<view v-else class="empty-state">暂无推荐卡项，可稍后再看。</view>
		</view>

		<view class="activity-entry" @tap="goPage('/pages/activity/index')">
			<text class="activity-title">活动专区免登录可访问</text>
			<text class="activity-desc">新客体验、训练福利和限时活动入口都保留在活动专区。</text>
		</view>
	</view>
</template>

<script>
	import { memberHomeIndex } from '../../api/auth'
	import { getMemberSession } from '../../utils/auth'

	export default {
		data() {
			return {
				loading: false,
				homeData: {},
				quickList: [
					{ title: '课程页', desc: '查看课程与排课', icon: '🗓', url: '/pages/course/index' },
					{ title: '我的', desc: '查看个人信息', icon: '👤', url: '/pages/mine/index' },
					{ title: '活动专区', desc: '新客福利与报名', icon: '🎁', url: '/pages/activity/index' },
					{ title: '回到入口', desc: '切换会员/教练', icon: '🚪', url: '/pages/portal/index' }
				]
			}
		},
		computed: {
			memberInfo() {
				const sessionInfo = getMemberSession().memberInfo || {}
				const summary = this.homeData.mineSummary || {}
				return {
					...sessionInfo,
					...summary
				}
			},
			displayName() {
				return this.memberInfo.realName || this.memberInfo.nickname || '会员'
			},
			stats() {
				const summary = this.homeData.mineSummary || {}
				return [
					{ label: '累计预约', value: summary.totalReservationCount || 0 },
					{ label: '待上课', value: summary.pendingReservationCount || 0 },
					{ label: '有效卡项', value: summary.activeCardCount || 0 },
					{ label: '待确认签到', value: summary.pendingConfirmCount || 0 }
				]
			},
			bannerList() {
				return this.homeData.bannerList || []
			},
			upcomingList() {
				return this.homeData.upcomingReservationList || []
			},
			cardList() {
				return this.homeData.cardList || []
			},
			promotionList() {
				return this.homeData.promotionList || []
			}
		},
		onShow() {
			this.loadHomeData()
		},
		methods: {
			async loadHomeData() {
				if (this.loading) {
					return
				}
				this.loading = true
				try {
					this.homeData = (await memberHomeIndex()) || {}
				} catch (error) {
					uni.showToast({ title: error.message || '加载首页失败', icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			goPage(url) {
				const pages = getCurrentPages()
				const current = pages[pages.length - 1]
				if (current && `/${current.route}` === url) {
					return
				}
				uni.navigateTo({ url })
			},
			formatSchedule(scheduleDate, startTime) {
				const dateText = this.formatDate(scheduleDate)
				const timeText = this.formatTime(startTime)
				return `${dateText} ${timeText}`.trim()
			},
			formatDate(value) {
				if (!value) {
					return '待定'
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return '待定'
				}
				return `${date.getMonth() + 1}/${date.getDate()}`
			},
			formatTime(value) {
				if (!value) {
					return ''
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return ''
				}
				const hour = `${date.getHours()}`.padStart(2, '0')
				const minute = `${date.getMinutes()}`.padStart(2, '0')
				return `${hour}:${minute}`
			},
			formatCardRemain(card) {
				if (card && card.remainTimes !== null && card.remainTimes !== undefined) {
					return `剩余 ${card.remainTimes} 次`
				}
				if (card && card.balanceAmount !== null && card.balanceAmount !== undefined) {
					return `余额 ¥${this.formatAmount(card.balanceAmount)}`
				}
				return '状态待同步'
			},
			formatExpire(value) {
				if (!value) {
					return '长期有效'
				}
				return `到期 ${this.formatDate(value)}`
			},
			formatAmount(value) {
				const amount = Number(value || 0)
				return amount.toFixed(2)
			}
		}
	}
</script>

<style lang="scss">
	.home-page {
		padding: 24rpx 24rpx 48rpx;
	}

	.hero-card,
	.section-card,
	.activity-entry {
		background: #fff;
		border-radius: 30rpx;
		box-shadow: 0 18rpx 48rpx rgba(15, 23, 42, 0.06);
	}

	.hero-card {
		padding: 32rpx;
		background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
		color: #fff;
	}

	.hero-top,
	.section-head,
	.promotion-top,
	.info-item,
	.member-card {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 20rpx;
	}

	.hero-badge,
	.meta-chip,
	.promotion-tag {
		display: inline-flex;
		align-items: center;
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
	}

	.hero-badge,
	.meta-chip {
		background: rgba(255, 255, 255, 0.14);
		color: rgba(255, 255, 255, 0.9);
	}

	.hero-title {
		margin-top: 18rpx;
		font-size: 52rpx;
		font-weight: 700;
	}

	.hero-desc {
		margin-top: 16rpx;
		font-size: 26rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.86);
	}

	.hero-avatar {
		width: 96rpx;
		height: 96rpx;
		border-radius: 28rpx;
		background: rgba(255, 255, 255, 0.14);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 36rpx;
		font-weight: 700;
	}

	.hero-meta {
		margin-top: 26rpx;
		display: flex;
		gap: 16rpx;
		flex-wrap: wrap;
	}

	.stats-grid {
		margin: 24rpx 0;
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 16rpx;
	}

	.stats-card {
		padding: 24rpx 12rpx;
		background: #fff;
		border-radius: 24rpx;
		text-align: center;
		box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05);
	}

	.stats-value {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: #ff5a36;
	}

	.stats-label,
	.section-more,
	.quick-desc,
	.info-desc,
	.card-desc,
	.promotion-desc,
	.activity-desc,
	.empty-state {
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.section-card {
		margin-bottom: 24rpx;
		padding: 28rpx;
	}

	.section-title,
	.quick-title,
	.info-title,
	.promotion-title,
	.activity-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #111827;
	}

	.quick-grid {
		margin-top: 24rpx;
		display: grid;
		grid-template-columns: repeat(2, 1fr);
		gap: 18rpx;
	}

	.quick-item,
	.promotion-card,
	.info-item,
	.member-card {
		padding: 24rpx;
		border-radius: 24rpx;
		background: #f9fafb;
	}

	.quick-icon {
		font-size: 42rpx;
	}

	.quick-title,
	.quick-desc,
	.info-title,
	.info-desc,
	.promotion-title,
	.promotion-desc,
	.card-highlight,
	.card-desc {
		display: block;
	}

	.quick-title,
	.info-title,
	.promotion-title {
		margin-top: 14rpx;
	}

	.banner-swiper {
		margin-top: 24rpx;
		height: 300rpx;
		border-radius: 24rpx;
		overflow: hidden;
	}

	.banner-image {
		width: 100%;
		height: 100%;
	}

	.info-list,
	.card-list,
	.promotion-list {
		margin-top: 24rpx;
		display: flex;
		flex-direction: column;
		gap: 18rpx;
	}

	.info-main {
		flex: 1;
	}

	.info-side,
	.card-highlight,
	.promotion-price {
		font-size: 26rpx;
		font-weight: 700;
		color: #ff5a36;
	}

	.card-side {
		text-align: right;
	}

	.promotion-tag {
		background: #ffedd5;
		color: #ea580c;
	}

	.activity-entry {
		padding: 28rpx;
		background: linear-gradient(135deg, #fff7ed 0%, #ffffff 100%);
	}

	.activity-desc {
		margin-top: 14rpx;
	}
</style>
