<template>
	<view class="page-wrap">
		<view class="header-card">
			<text class="header-title">活动优惠卡项</text>
			<text class="header-desc">展示门店卡项与活动优惠，请联系教练线下咨询</text>
		</view>

		<scroll-view class="type-scroll" scroll-x>
			<view class="type-chip" :class="{ active: currentType === '' }" @tap="switchType('')">全部</view>
			<view class="type-chip" :class="{ active: currentType === item.value }" v-for="item in planTypeOptions" :key="item.value" @tap="switchType(item.value)">
				{{ item.label }}
			</view>
		</scroll-view>

		<view v-if="loading" class="state-wrap">卡项加载中...</view>
		<view v-else-if="errorText" class="state-wrap error" @tap="loadPlanList">{{ errorText }}，点击重试</view>
		<block v-else>
			<view class="plan-card" v-for="item in filteredPlanList" :key="item.planId">
				<view class="title-row">
					<text class="plan-title">{{ item.planName }}</text>
					<text class="plan-type">{{ planTypeText(item.planType) }}</text>
				</view>
				<view class="price-row">
					<text class="sale-price">¥{{ formatAmount(item.salePrice) }}</text>
					<text class="origin-price" v-if="Number(item.originalPrice || 0) > Number(item.salePrice || 0)">¥{{ formatAmount(item.originalPrice) }}</text>
					<text class="discount-tag" v-if="item.discountLabel">{{ item.discountLabel }}</text>
				</view>
				<view class="desc" v-if="item.totalTimes || item.giftTimes">次数：{{ item.totalTimes || 0 }} + 赠送 {{ item.giftTimes || 0 }}</view>
				<view class="desc" v-if="item.validDays">有效期：{{ item.validDays }} 天</view>
				<view class="desc" v-if="item.ruleNote">规则：{{ item.ruleNote }}</view>
				<view class="desc" v-else>规则：以门店方案为准，可咨询门店获取完整权益说明</view>
				<button class="btn" @tap="goContact">联系门店咨询</button>
			</view>
			<view v-if="!filteredPlanList.length" class="state-wrap">当前活动暂无匹配卡项</view>
		</block>
	</view>
</template>

<script>
	import { memberActivityCardPlanList, memberActivityDetail } from '../../api/auth'

	export default {
		data() {
			return {
				activityId: undefined,
				detail: {},
				planList: [],
				loading: false,
				errorText: '',
				currentType: '',
				planTypeOptions: [
					{ label: '正常卡项', value: 'count_card' },
					{ label: '体验卡', value: 'experience' },
					{ label: '周卡', value: 'week_card' },
					{ label: '月卡', value: 'month_card' },
					{ label: '季卡', value: 'season_card' },
					{ label: '年卡', value: 'year_card' },
					{ label: '团课卡', value: 'group_card' },
					{ label: '时长卡', value: 'time_card' },
					{ label: '次卡', value: 'count_card' },
					{ label: '储值卡', value: 'stored_card' }
				]
			}
		},
		computed: {
			filteredPlanList() {
				if (!this.currentType) {
					return this.planList
				}
				return this.planList.filter((item) => item.planType === this.currentType)
			}
		},
		onLoad(options) {
			this.activityId = options && options.id
			this.loadPlanList()
		},
		methods: {
			async loadPlanList() {
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
					const [detail, plans] = await Promise.all([
						memberActivityDetail(this.activityId),
						memberActivityCardPlanList(this.activityId)
					])
					this.detail = detail || {}
					this.planList = plans || []
				} catch (error) {
					this.errorText = error.message || '活动卡项加载失败'
					this.planList = []
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			switchType(type) {
				if (this.currentType === type) {
					return
				}
				this.currentType = type
			},
			goContact() {
				const phone = this.detail.storeContactPhone
				if (!phone) {
					uni.showToast({ title: '当前门店暂无联系电话', icon: 'none' })
					return
				}
				uni.makePhoneCall({ phoneNumber: phone })
			},
			planTypeText(value) {
				const hit = this.planTypeOptions.find((item) => item.value === value)
				return hit ? hit.label : (value || '卡项')
			},
			formatAmount(value) {
				return Number(value || 0).toFixed(2)
			}
		}
	}
</script>

<style lang="scss">
	.page-wrap {
		padding: 24rpx;
	}

	.header-card,
	.plan-card {
		background: #fff;
		border-radius: 24rpx;
		padding: 24rpx;
	}

	.header-card {
		margin-bottom: 20rpx;
	}

	.header-title {
		font-size: 34rpx;
		font-weight: 700;
		color: #111827;
	}

	.header-desc,
	.desc,
	.state-wrap {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.type-scroll {
		white-space: nowrap;
		margin-bottom: 16rpx;
	}

	.type-chip {
		display: inline-flex;
		align-items: center;
		padding: 12rpx 22rpx;
		margin-right: 12rpx;
		border-radius: 999rpx;
		background: #f3f4f6;
		font-size: 24rpx;
		color: #4b5563;
	}

	.type-chip.active {
		background: #111827;
		color: #fff;
	}

	.plan-card {
		margin-bottom: 16rpx;
	}

	.title-row,
	.price-row {
		display: flex;
		align-items: center;
		gap: 12rpx;
		justify-content: space-between;
	}

	.plan-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #111827;
	}

	.plan-type,
	.discount-tag {
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
	}

	.plan-type {
		background: #eef2ff;
		color: #4338ca;
	}

	.discount-tag {
		background: #ffedd5;
		color: #ea580c;
	}

	.sale-price {
		font-size: 34rpx;
		font-weight: 700;
		color: #dc2626;
	}

	.origin-price {
		font-size: 24rpx;
		color: #9ca3af;
		text-decoration: line-through;
	}

	.btn {
		margin-top: 16rpx;
		border-radius: 999rpx;
		height: 72rpx;
		line-height: 72rpx;
		font-size: 24rpx;
		color: #fff;
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
	}

	.btn::after {
		border: none;
	}

	.state-wrap {
		text-align: center;
		margin-top: 40rpx;
	}

	.state-wrap.error {
		color: #dc2626;
	}
</style>
