<template>
	<view class="student-page">
		<view class="header-card">
			<view class="header-title">我的学员</view>
			<view class="header-desc">查看学员剩余课次、最近到店情况和下次课程。</view>
		</view>

		<view class="search-row">
			<input v-model.trim="keyword" class="search-input" placeholder="输入姓名/昵称/手机号" @confirm="loadList" />
			<view class="search-btn" :class="{ disabled: loading }" @tap="loadList">{{ loading ? '搜索中...' : '搜索' }}</view>
		</view>

		<view class="tag-row">
			<view class="tag-chip">总学员 {{ list.length }}</view>
			<view class="tag-chip">有下次排课 {{ nextClassCount }}</view>
			<view class="tag-chip">有剩余课次 {{ remainCount }}</view>
		</view>

		<view v-if="loading" class="empty">加载中...</view>
		<view v-else-if="errorText" class="empty error" @tap="loadList">{{ errorText }}，点击重试</view>
		<block v-else>
			<view class="student-list">
				<view class="student-card" v-for="item in list" :key="`${item.memberId}-${item.cardId || 0}`">
					<view class="student-head">
						<view class="student-main">
							<view class="student-name-row">
								<text class="student-name">{{ item.realName || item.nickname || '会员' }}</text>
								<text class="level-tag">{{ item.cardName || '未绑卡' }}</text>
							</view>
							<view class="student-goal">手机号 {{ item.mobile || '未维护' }}</view>
						</view>
						<view class="remain-box">
							<text class="remain-value">{{ remainTimesText(item.remainTimes) }}</text>
							<text class="remain-label">剩余课次</text>
						</view>
					</view>
					<button size="mini" @tap="goPage('/pages/training/index?role=coach&memberId=' + item.memberId)">定制 / 查看训练计划</button>
					<view class="info-grid">
						<view class="info-item">
							<text class="info-label">最近到店</text>
							<text class="info-value">{{ formatDateTime(item.lastVisitTime) }}</text>
						</view>
						<view class="info-item">
							<text class="info-label">下次课程</text>
							<text class="info-value">{{ item.nextCourseName || '暂无' }}</text>
						</view>
						<view class="info-item">
							<text class="info-label">下次时间</text>
							<text class="info-value">{{ formatDateTime(item.nextClassTime) }}</text>
						</view>
						<view class="info-item">
							<text class="info-label">累计核销</text>
							<text class="info-value">{{ item.totalCheckinCount || 0 }} 次</text>
						</view>
					</view>
				</view>
			</view>
			<view v-if="!list.length" class="empty">暂无学员数据</view>
		</block>

		<view class="bottom-nav">
			<view class="nav-item" @tap="goPage('/pages/coach/home/index')">工作台</view>
			<view class="nav-item" @tap="goPage('/pages/coach/schedule/index')">排课</view>
			<view class="nav-item" @tap="goPage('/pages/coach/checkin/index')">核销</view>
			<view class="nav-item active">学员</view>
			<view class="nav-item" @tap="goPage('/pages/coach/mine/index')">我的</view>
		</view>
	</view>
</template>

<script>
	import { coachStudentList } from '../../../api/auth'

	export default {
		data() {
			return {
				keyword: '',
				list: [],
				loading: false,
				errorText: ''
			}
		},
		computed: {
			nextClassCount() {
				return this.list.filter((item) => !!item.nextClassTime).length
			},
			remainCount() {
				return this.list.filter((item) => Number(item.remainTimes || 0) > 0).length
			}
		},
		onShow() {
			this.loadList()
		},
		methods: {
			async loadList() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					const table = (await coachStudentList({ keyword: this.keyword, pageNum: 1, pageSize: 50 })) || {}
					this.list = table.rows || []
				} catch (error) {
					this.errorText = error.message || '学员加载失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
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
				uni.redirectTo({ url })
			},
			formatDateTime(value) {
				if (!value) return '暂无'
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) return '暂无'
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mm = `${date.getMinutes()}`.padStart(2, '0')
				return `${date.getMonth() + 1}/${date.getDate()} ${hh}:${mm}`
			},
			remainTimesText(value) {
				if (value === 0 || !!value) {
					return value
				}
				return '--'
			}
		}
	}
</script>

<style lang="scss">
	.student-page { padding: 24rpx 24rpx 160rpx; }
	.header-card, .student-card { padding: 28rpx; background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05); }
	.header-title, .student-name { font-size: 32rpx; font-weight: 700; color: #111827; }
	.header-desc, .student-goal, .info-label, .info-value, .remain-label, .empty { font-size: 24rpx; line-height: 1.7; }
	.header-desc, .student-goal, .info-label, .remain-label, .empty { margin-top: 10rpx; color: #6b7280; }
	.search-row { margin: 20rpx 0; display: flex; gap: 12rpx; }
	.search-input { flex: 1; height: 74rpx; border-radius: 999rpx; background: #fff; padding: 0 26rpx; font-size: 24rpx; }
	.search-btn { width: 150rpx; border-radius: 999rpx; background: #111827; color: #fff; font-size: 24rpx; display: flex; align-items: center; justify-content: center; }
	.search-btn.disabled { opacity: 0.65; }
	.tag-row { margin-bottom: 20rpx; display: flex; gap: 12rpx; flex-wrap: wrap; }
	.tag-chip, .level-tag { padding: 10rpx 18rpx; border-radius: 999rpx; font-size: 22rpx; }
	.tag-chip { background: #fff; color: #4b5563; }
	.level-tag { background: #ffedd5; color: #ea580c; }
	.student-list { display: flex; flex-direction: column; gap: 18rpx; }
	.student-head, .student-name-row, .bottom-nav { display: flex; align-items: center; justify-content: space-between; }
	.student-main { flex: 1; }
	.remain-box { padding: 18rpx 22rpx; border-radius: 24rpx; background: #f9fafb; text-align: center; }
	.remain-value { display: block; font-size: 34rpx; font-weight: 700; color: #ff5a36; }
	.info-grid { margin-top: 24rpx; display: grid; grid-template-columns: repeat(2, 1fr); gap: 18rpx; }
	.info-item { padding: 20rpx; border-radius: 24rpx; background: #f9fafb; }
	.info-value { display: block; margin-top: 8rpx; font-weight: 600; color: #111827; }
	.empty { text-align: center; padding: 40rpx 0 10rpx; }
	.empty.error { color: #dc2626; }
	.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; padding: 18rpx 20rpx; border-radius: 999rpx; background: rgba(17, 24, 39, 0.94); color: #cbd5e1; box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.18); }
	.nav-item { flex: 1; text-align: center; font-size: 24rpx; }
	.nav-item.active { color: #fff; font-weight: 700; }
</style>
