<template>
	<view class="schedule-page">
		<view class="summary-card">
			<view>
				<view class="summary-title">我的排课</view>
				<view class="summary-desc">按今日、本周快速查看排课和签到状态。</view>
			</view>
			<view class="summary-tag">{{ list.length }} 节</view>
		</view>

		<view class="filter-row">
			<view class="filter-chip" :class="{ active: activeTab === item.value }" v-for="item in tabs" :key="item.value" @tap="changeTab(item.value)">
				{{ item.label }}
			</view>
		</view>

		<view class="schedule-list">
			<view class="schedule-card" v-for="item in list" :key="item.scheduleId">
				<view class="schedule-head">
					<view>
						<view class="card-title">{{ item.courseName || '课程待定' }}</view>
						<view class="card-meta">{{ item.storeName || '门店待定' }} · {{ statusText(item.scheduleStatus) }}</view>
					</view>
					<text class="status-tag plain">{{ item.courseType || '课程' }}</text>
				</view>
				<view class="detail-grid">
					<view class="detail-item">
						<text class="detail-label">上课时间</text>
						<text class="detail-value">{{ formatDateTime(item.startTime) }} - {{ formatTime(item.endTime) }}</text>
					</view>
					<view class="detail-item">
						<text class="detail-label">签到进度</text>
						<text class="detail-value">{{ item.signedCount || 0 }}/{{ item.reservedCount || 0 }}</text>
					</view>
				</view>
			</view>
		</view>
		<view v-if="!list.length" class="empty">当前筛选下暂无排课</view>

		<view class="bottom-nav">
			<view class="nav-item" @tap="goPage('/pages/coach/home/index')">工作台</view>
			<view class="nav-item active">排课</view>
			<view class="nav-item" @tap="goPage('/pages/coach/checkin/index')">核销</view>
			<view class="nav-item" @tap="goPage('/pages/coach/student/index')">学员</view>
			<view class="nav-item" @tap="goPage('/pages/coach/mine/index')">我的</view>
		</view>
	</view>
</template>

<script>
	import { coachScheduleList } from '../../../api/auth'

	export default {
		data() {
			return {
				activeTab: 'today',
				tabs: [
					{ label: '今日', value: 'today' },
					{ label: '明日', value: 'tomorrow' },
					{ label: '本周', value: 'week' },
					{ label: '全部', value: 'all' }
				],
				list: []
			}
		},
		onShow() {
			this.loadData()
		},
		methods: {
			async changeTab(tab) {
				if (this.activeTab === tab) return
				this.activeTab = tab
				await this.loadData()
			},
			async loadData() {
				try {
					const table = (await coachScheduleList({ scope: this.activeTab, pageNum: 1, pageSize: 30 })) || {}
					this.list = table.rows || []
				} catch (error) {
					uni.showToast({ title: error.message || '排课加载失败', icon: 'none' })
				}
			},
			goPage(url) {
				const pages = getCurrentPages()
				const current = pages[pages.length - 1]
				if (current && `/${current.route}` === url) {
					return
				}
				uni.redirectTo({ url })
			},
			statusText(value) {
				const map = {
					draft: '草稿',
					open: '可预约',
					closed: '已关闭',
					cancelled: '已取消',
					finished: '已结束'
				}
				return map[value] || value || '未知'
			},
			formatDateTime(value) {
				if (!value) return '待定'
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) return '待定'
				return `${date.getMonth() + 1}/${date.getDate()} ${this.formatTime(value)}`
			},
			formatTime(value) {
				if (!value) return '--:--'
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) return '--:--'
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mm = `${date.getMinutes()}`.padStart(2, '0')
				return `${hh}:${mm}`
			}
		}
	}
</script>

<style lang="scss">
	.schedule-page { padding: 24rpx 24rpx 160rpx; }
	.summary-card, .schedule-card { padding: 28rpx; background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05); }
	.summary-card, .schedule-head, .bottom-nav { display: flex; align-items: center; justify-content: space-between; }
	.summary-title, .card-title { font-size: 32rpx; font-weight: 700; color: #111827; }
	.summary-desc, .card-meta, .detail-label, .detail-value, .empty { font-size: 24rpx; line-height: 1.7; }
	.summary-desc, .card-meta, .detail-label, .empty { margin-top: 8rpx; color: #6b7280; }
	.summary-tag, .status-tag { padding: 10rpx 18rpx; border-radius: 999rpx; font-size: 22rpx; }
	.summary-tag { background: #ffedd5; color: #ea580c; }
	.status-tag.plain { background: #e5e7eb; color: #374151; }
	.filter-row { margin: 24rpx 0; display: flex; gap: 16rpx; flex-wrap: wrap; }
	.filter-chip { padding: 14rpx 26rpx; border-radius: 999rpx; background: #fff; font-size: 24rpx; color: #4b5563; }
	.filter-chip.active { background: #111827; color: #fff; font-weight: 700; }
	.schedule-list { display: flex; flex-direction: column; gap: 18rpx; }
	.detail-grid { margin-top: 20rpx; display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
	.detail-item { padding: 20rpx; border-radius: 24rpx; background: #f9fafb; }
	.detail-value { display: block; margin-top: 8rpx; font-weight: 600; color: #111827; }
	.empty { text-align: center; padding: 40rpx 0 10rpx; }
	.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; padding: 18rpx 20rpx; border-radius: 999rpx; background: rgba(17, 24, 39, 0.94); color: #cbd5e1; box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.18); }
	.nav-item { flex: 1; text-align: center; font-size: 24rpx; }
	.nav-item.active { color: #fff; font-weight: 700; }
</style>
