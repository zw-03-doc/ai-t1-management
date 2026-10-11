<template>
	<view class="page-wrap">
		<view class="hero-card">
			<image class="hero-image" src="/static/images/live-2.jpg" mode="aspectFill" />
			<view class="hero-mask"></view>
			<view class="hero-text">
				<text class="hero-badge">活动专区</text>
				<text class="hero-title">本周主推与门店活动</text>
				<text class="hero-desc">活动内容由后台运营配置维护，支持门店筛选与详情查看。</text>
			</view>
		</view>

		<scroll-view class="store-scroll" scroll-x>
			<view
				class="store-chip"
				:class="{ active: selectedStoreId === '', disabled: loading || switchingStore }"
				@tap="switchStore('')"
			>
				全部门店
			</view>
			<view
				class="store-chip"
				:class="{ active: selectedStoreId === item.storeId, disabled: loading || switchingStore }"
				v-for="item in stores"
				:key="item.storeId"
				@tap="switchStore(item.storeId)"
			>
				{{ item.storeName }}
			</view>
		</scroll-view>

		<view v-if="loading" class="state-wrap">活动数据加载中...</view>
		<view v-else-if="errorText" class="state-wrap error" @tap="loadStoresAndActivities">{{ errorText }}，点击重试</view>
		<block v-else>
			<view class="poster-card" v-for="item in activityList" :key="item.activityId" @tap="goActivityDetail(item)">
				<image v-if="item.coverUrl" class="poster-cover" :src="item.coverUrl" mode="aspectFill" />
				<view class="poster-top">
					<text class="poster-title">{{ item.title }}</text>
					<text class="featured-tag" v-if="item.weeklyFeatured === '1'">本周主推</text>
				</view>
				<view class="poster-subtitle" v-if="item.subTitle">{{ item.subTitle }}</view>
				<view class="poster-desc" v-if="item.priceHighlight">价格亮点：{{ item.priceHighlight }}</view>
				<view class="poster-desc" v-if="item.targetAudience">适用人群：{{ item.targetAudience }}</view>
				<view class="poster-footer">
					<text>{{ item.storeName || '全门店' }} · {{ formatCountdown(item.countdownEndTime || item.endTime) }}</text>
					<text class="poster-action">查看详情</text>
				</view>
			</view>

			<view v-if="!activityList.length" class="empty-wrap">当前门店暂无上架活动</view>
		</block>
	</view>
</template>

<script>
	import { memberActivityList, memberStoreList } from '../../api/auth'

	export default {
		data() {
			return {
				stores: [],
				selectedStoreId: '',
				activityList: [],
				loading: false,
				switchingStore: false,
				errorText: ''
			}
		},
		onShow() {
			this.loadStoresAndActivities()
		},
		methods: {
			async loadStoresAndActivities() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					const [stores, activities] = await Promise.all([
						memberStoreList(''),
						memberActivityList()
					])
					this.stores = stores || []
					this.activityList = activities || []
				} catch (error) {
					this.errorText = error.message || '活动数据加载失败'
					this.activityList = []
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			async switchStore(storeId) {
				if (this.selectedStoreId === storeId || this.loading || this.switchingStore) {
					return
				}
				const prevStoreId = this.selectedStoreId
				this.selectedStoreId = storeId
				this.switchingStore = true
				this.errorText = ''
				try {
					this.activityList = (await memberActivityList(storeId)) || []
				} catch (error) {
					this.selectedStoreId = prevStoreId
					this.errorText = error.message || '门店切换失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.switchingStore = false
				}
			},
			goActivityDetail(item) {
				if (!item || !item.activityId) {
					return
				}
				uni.navigateTo({ url: `/pages/activity/detail?id=${item.activityId}` })
			},
			formatCountdown(value) {
				if (!value) {
					return '长期有效'
				}
				const end = new Date(value)
				if (Number.isNaN(end.getTime())) {
					return '长期有效'
				}
				const diff = end.getTime() - Date.now()
				if (diff <= 0) {
					return '活动已结束'
				}
				const day = Math.floor(diff / (24 * 60 * 60 * 1000))
				const hour = Math.floor((diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000))
				if (day > 0) {
					return `倒计时 ${day}天${hour}小时`
				}
				return `倒计时 ${hour}小时`
			}
		}
	}
</script>

<style lang="scss">
	.page-wrap {
		padding: 24rpx;
	}

	.hero-card {
		position: relative;
		height: 320rpx;
		border-radius: 28rpx;
		overflow: hidden;
		margin-bottom: 24rpx;
	}

	.hero-image,
	.hero-mask {
		position: absolute;
		inset: 0;
		width: 100%;
		height: 100%;
	}

	.hero-mask {
		background: linear-gradient(135deg, rgba(17, 24, 39, 0.2) 0%, rgba(17, 24, 39, 0.75) 100%);
	}

	.hero-text {
		position: relative;
		z-index: 2;
		height: 100%;
		padding: 28rpx;
		display: flex;
		flex-direction: column;
		justify-content: flex-end;
		color: #fff;
	}

	.hero-badge,
	.poster-action,
	.featured-tag {
		padding: 8rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
	}

	.hero-badge,
	.poster-action {
		background: #ffedd5;
		color: #ea580c;
	}

	.featured-tag {
		background: #fee2e2;
		color: #dc2626;
	}

	.hero-title {
		margin-top: 12rpx;
		font-size: 40rpx;
		font-weight: 700;
	}

	.hero-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.7;
	}

	.store-scroll {
		white-space: nowrap;
		margin-bottom: 18rpx;
	}

	.store-chip {
		display: inline-flex;
		align-items: center;
		padding: 12rpx 22rpx;
		margin-right: 14rpx;
		border-radius: 999rpx;
		background: #fff;
		font-size: 24rpx;
		color: #4b5563;
	}

	.store-chip.active {
		background: #111827;
		color: #fff;
		font-weight: 700;
	}

	.store-chip.disabled {
		opacity: 0.55;
	}

	.poster-card {
		padding: 24rpx;
		border-radius: 28rpx;
		background: #fff;
		margin-bottom: 18rpx;
	}

	.poster-cover {
		width: 100%;
		height: 220rpx;
		border-radius: 20rpx;
		margin-bottom: 14rpx;
	}

	.poster-top,
	.poster-footer {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 16rpx;
	}

	.poster-title {
		font-size: 32rpx;
		font-weight: 700;
	}

	.poster-subtitle,
	.poster-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.poster-footer {
		margin-top: 18rpx;
		font-size: 24rpx;
		color: #4b5563;
	}

	.empty-wrap,
	.state-wrap {
		margin-top: 40rpx;
		text-align: center;
		font-size: 24rpx;
		color: #9ca3af;
	}

	.state-wrap.error {
		color: #dc2626;
	}
</style>
