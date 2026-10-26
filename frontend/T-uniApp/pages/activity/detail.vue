<template>
	<view class="page-wrap">
		<view v-if="loading" class="state-wrap">活动详情加载中...</view>
		<view v-else-if="errorText" class="state-wrap error" @tap="loadDetail">{{ errorText }}，点击重试</view>
		<block v-else>
			<image v-if="detail.coverUrl" class="hero-image" :src="detail.coverUrl" mode="aspectFill" />
			<view class="panel">
				<view class="title-row">
					<text class="title">{{ detail.title || '活动详情' }}</text>
					<text class="featured-tag" v-if="detail.weeklyFeatured === '1'">本周主推</text>
				</view>
				<view class="sub-title" v-if="detail.subTitle">{{ detail.subTitle }}</view>
				<view class="desc" v-if="detail.priceHighlight">价格亮点：{{ detail.priceHighlight }}</view>
				<view class="desc" v-if="detail.targetAudience">适用人群：{{ detail.targetAudience }}</view>
				<view class="desc" v-if="detail.discountLabel">活动折扣：{{ detail.discountLabel }}</view>
				<view class="desc" v-if="detail.ruleNote">使用规则：{{ detail.ruleNote }}</view>
				<view class="desc">适用门店：{{ detail.storeName || '全门店' }}</view>
				<view class="desc">活动时间：{{ formatDateTime(detail.startTime) }} - {{ formatDateTime(detail.endTime) }}</view>
				<view class="countdown">{{ formatCountdown(detail.countdownEndTime || detail.endTime) }}</view>
			</view>

			<image v-if="detail.detailImageUrl" class="detail-image" :src="detail.detailImageUrl" mode="widthFix" />

			<view class="panel" v-if="detail.detailContent">
				<view class="section-title">活动说明</view>
				<view class="content">{{ detail.detailContent }}</view>
			</view>

			<view class="panel action-panel">
				<view class="action-tip">按活动类型查看优惠卡项，联系门店或负责教练线下咨询。</view>
				<button class="btn primary" @tap="handlePrimaryAction">{{ primaryActionText }}</button>
				<button class="btn ghost" @tap="goBack">返回活动列表</button>
			</view>
		</block>
	</view>
</template>

<script>
	import { memberActivityDetail } from '../../api/auth'

	export default {
		data() {
			return {
				activityId: undefined,
				detail: {},
				loading: false,
				errorText: ''
			}
		},
		computed: {
			primaryActionText() {
				return this.detail.actionType === 'contact' ? '联系门店' : '查看优惠卡项'
			}
		},
		onLoad(options) {
			this.activityId = options && options.id
			this.loadDetail()
		},
		methods: {
			async loadDetail() {
				if (!this.activityId) {
					this.errorText = '活动参数缺失'
					return
				}
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					this.detail = (await memberActivityDetail(this.activityId)) || {}
				} catch (error) {
					this.errorText = error.message || '活动详情加载失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			handlePrimaryAction() {
				if (this.detail.actionType === 'contact') {
					this.contactStore()
					return
				}
				this.goPlanList()
			},
			goPlanList() {
				uni.navigateTo({ url: `/pages/activity/plan?id=${this.activityId}` })
			},
			contactStore() {
				const phone = this.detail.storeContactPhone
				if (!phone) {
					uni.showToast({ title: '当前门店暂无联系电话', icon: 'none' })
					return
				}
				uni.makePhoneCall({ phoneNumber: phone })
			},
			goBack() {
				uni.navigateBack({ delta: 1 })
			},
			formatDateTime(value) {
				if (!value) {
					return '--'
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return '--'
				}
				const yyyy = date.getFullYear()
				const mm = `${date.getMonth() + 1}`.padStart(2, '0')
				const dd = `${date.getDate()}`.padStart(2, '0')
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mi = `${date.getMinutes()}`.padStart(2, '0')
				return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
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

	.hero-image {
		width: 100%;
		height: 360rpx;
		border-radius: 28rpx;
		margin-bottom: 18rpx;
	}

	.panel {
		margin-bottom: 18rpx;
		padding: 24rpx;
		background: #fff;
		border-radius: 24rpx;
	}

	.title-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12rpx;
	}

	.title {
		font-size: 34rpx;
		font-weight: 700;
		color: #111827;
	}

	.featured-tag {
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		background: #fee2e2;
		color: #dc2626;
	}

	.sub-title,
	.desc,
	.content,
	.action-tip,
	.state-wrap {
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.sub-title,
	.desc,
	.countdown,
	.content,
	.action-tip {
		margin-top: 10rpx;
	}

	.countdown {
		font-size: 24rpx;
		font-weight: 600;
		color: #ea580c;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #111827;
	}

	.detail-image {
		width: 100%;
		border-radius: 24rpx;
		margin-bottom: 18rpx;
	}

	.btn {
		margin-top: 16rpx;
		border-radius: 999rpx;
		height: 80rpx;
		line-height: 80rpx;
		font-size: 26rpx;
	}

	.btn::after {
		border: none;
	}

	.btn.primary {
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
		color: #fff;
	}

	.btn.ghost {
		background: #fff7ed;
		color: #ea580c;
	}

	.state-wrap {
		text-align: center;
		margin-top: 50rpx;
	}

	.state-wrap.error {
		color: #dc2626;
	}
</style>
