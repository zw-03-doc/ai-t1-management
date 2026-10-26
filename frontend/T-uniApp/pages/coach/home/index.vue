<template>
	<view class="coach-page">
		<view class="hero-card">
			<view class="hero-top">
				<view>
					<text class="hero-badge">{{ activationLabel }}</text>
					<view class="hero-title">{{ coachName }}，开始今天的带练节奏</view>
					<view class="hero-desc">当前工作台已接入后端实时聚合数据，可直接查看排课、待核销与消息提醒。</view>
				</view>
				<view class="hero-avatar">{{ coachName.slice(0, 1) }}</view>
			</view>
			<view class="hero-meta">
				<text class="meta-chip">{{ coachProfile.storeName || '门店待分配' }}</text>
				<text class="meta-chip">{{ coachProfile.mobile || coachProfile.phonenumber || '手机号待同步' }}</text>
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
				<text class="section-more">教练工作台</text>
			</view>
			<view class="quick-grid">
				<view class="quick-item" v-for="item in quickList" :key="item.title" @tap="goPage(item.url)">
					<text class="quick-icon">{{ item.icon }}</text>
					<text class="quick-title">{{ item.title }}</text>
					<text class="quick-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">待核销提醒</text>
				<text class="section-more">{{ pendingList.length }} 条</text>
			</view>
			<view v-if="pendingList.length" class="list-wrap">
				<view class="list-item" v-for="item in pendingList" :key="item.checkinId || item.reservationId">
					<view class="list-main">
						<text class="list-title">{{ item.memberName || '会员' }} · {{ item.courseName || '待核销课程' }}</text>
						<text class="list-desc">{{ item.storeName || '门店待定' }} · 签到时间 {{ formatTime(item.checkinTime) || '--:--' }}</text>
					</view>
					<text class="status-tag warning">待核销</text>
				</view>
			</view>
			<view v-else class="empty-state">当前没有待核销记录，节奏不错。</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">今日课程</text>
				<text class="section-more">{{ scheduleList.length }} 节</text>
			</view>
			<view v-if="scheduleList.length" class="list-wrap">
				<view class="list-item" v-for="item in scheduleList" :key="item.scheduleId">
					<view class="list-main">
						<text class="list-title">{{ item.courseName || '课程待定' }}</text>
						<text class="list-desc">{{ item.storeName || '门店待定' }} · {{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</text>
					</view>
					<view class="schedule-side">
						<text class="schedule-count">{{ item.signedCount || 0 }}/{{ item.reservedCount || 0 }}</text>
						<text class="schedule-label">已签到/已预约</text>
					</view>
				</view>
			</view>
			<view v-else class="empty-state">今日暂无排课，可查看消息提醒。</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">消息提醒</text>
				<text class="section-more">最新 10 条</text>
			</view>
			<view v-if="messageList.length" class="list-wrap">
				<view class="message-item" v-for="(item, index) in messageList" :key="index">
					<text class="message-title">{{ item.title }}</text>
					<text class="message-desc">{{ item.content }}</text>
					<text class="message-time">{{ formatDateTime(item.eventTime) }}</text>
				</view>
			</view>
			<view v-else class="empty-state">暂无消息提醒。</view>
		</view>
	</view>
</template>

<script>
	import { coachHomeOverview } from '../../../api/auth'
	import { getCoachSession } from '../../../utils/auth'

	export default {
		data() {
			return {
				loading: false,
				overview: {},
				quickList: [
                    {title:'课程兴趣',desc:'查看学员关注的课程',icon:'↗',url:'/pages/coach/interest/index'},
					{ title: '我的排课', desc: '查看今日与本周课程', icon: '🗓', url: '/pages/coach/schedule/index' },
					{ title: '待核销', desc: '处理签到与消课', icon: '✅', url: '/pages/coach/checkin/index' },
					{ title: '我的学员', desc: '查看学员和卡项', icon: '👥', url: '/pages/coach/student/index' },
					{ title: '个人资料', desc: '查看教练信息', icon: '📋', url: '/pages/coach/mine/index' }
				]
			}
		},
		computed: {
			coachSession() {
				return getCoachSession() || {}
			},
			coachProfile() {
				return this.coachSession.profile || {}
			},
			coachName() {
				return this.coachProfile.coachName || this.coachProfile.nickName || '教练'
			},
			activationLabel() {
				return this.coachSession.activated ? '微信已激活' : '教练已登录'
			},
			stats() {
				const stats = this.overview.stats || {}
				return [
					{ label: '今日排课', value: stats.todayScheduleCount || 0 },
					{ label: '待核销', value: stats.pendingCheckinCount || 0 },
					{ label: '已确认', value: stats.confirmedCount || 0 },
					{ label: '本周课时', value: stats.weekSessionCount || 0 }
				]
			},
			pendingList() {
				return this.overview.pendingList || []
			},
			scheduleList() {
				return this.overview.todayScheduleList || []
			},
			messageList() {
				return this.overview.messageList || []
			}
		},
		onShow() {
			this.loadOverview()
		},
		methods: {
			async loadOverview() {
				if (this.loading) {
					return
				}
				this.loading = true
				try {
					this.overview = (await coachHomeOverview()) || {}
				} catch (error) {
					uni.showToast({ title: error.message || '加载教练首页失败', icon: 'none' })
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
			formatDateTime(value) {
				if (!value) {
					return '刚刚'
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return '刚刚'
				}
				return `${date.getMonth() + 1}/${date.getDate()} ${this.formatTime(value)}`
			}
		}
	}
</script>

<style lang="scss">
	.coach-page {
		padding: 24rpx 24rpx 48rpx;
	}

	.hero-card,
	.section-card {
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
	.list-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 20rpx;
	}

	.hero-badge,
	.meta-chip,
	.status-tag {
		display: inline-flex;
		align-items: center;
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
	}

	.hero-badge,
	.meta-chip {
		background: rgba(255, 255, 255, 0.14);
		color: rgba(255, 255, 255, 0.92);
	}

	.hero-title {
		margin-top: 18rpx;
		font-size: 48rpx;
		font-weight: 700;
		line-height: 1.3;
	}

	.hero-desc {
		margin-top: 16rpx;
		font-size: 26rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.84);
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
	.list-desc,
	.message-desc,
	.message-time,
	.empty-state,
	.schedule-label {
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.section-card {
		margin-bottom: 24rpx;
		padding: 28rpx;
		background: #fff;
	}

	.section-title,
	.quick-title,
	.list-title,
	.message-title {
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
	.list-item,
	.message-item {
		padding: 24rpx;
		border-radius: 24rpx;
		background: #f9fafb;
	}

	.quick-icon {
		font-size: 42rpx;
	}

	.quick-title,
	.quick-desc,
	.list-title,
	.list-desc,
	.message-title,
	.message-desc,
	.message-time,
	.schedule-count,
	.schedule-label {
		display: block;
	}

	.quick-title,
	.list-title,
	.message-title {
		margin-top: 14rpx;
	}

	.list-wrap {
		margin-top: 24rpx;
		display: flex;
		flex-direction: column;
		gap: 18rpx;
	}

	.list-main {
		flex: 1;
	}

	.status-tag.warning {
		background: #ffedd5;
		color: #ea580c;
	}

	.schedule-side {
		text-align: right;
	}

	.schedule-count {
		font-size: 26rpx;
		font-weight: 700;
		color: #ff5a36;
	}

	.message-desc,
	.message-time {
		margin-top: 10rpx;
	}
</style>
