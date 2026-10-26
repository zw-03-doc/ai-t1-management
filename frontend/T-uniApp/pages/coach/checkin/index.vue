<template>
	<view class="checkin-page">
		<view class="header-card">
			<view>
				<view class="header-title">签到与核销</view>
				<view class="header-desc">可为本人授课的学员代签到；已签到的课程可确认到课。每次操作均留有记录。</view>
			</view>
			<view class="header-badge">{{ pendingCount }} 条待处理</view>
		</view>

		<view class="stats-grid">
			<view class="stats-card">
				<text class="stats-value">{{ pendingCount }}</text>
				<text class="stats-label">待核销</text>
			</view>
			<view class="stats-card">
				<text class="stats-value">{{ confirmedCount }}</text>
				<text class="stats-label">已完成</text>
			</view>
			<view class="stats-card">
				<text class="stats-value">{{ exceptionCount }}</text>
				<text class="stats-label">异常</text>
			</view>
		</view>

		<view class="filter-row">
			<view class="filter-chip" :class="{ active: activeTab === item.value }" v-for="item in tabs" :key="item.value" @tap="changeTab(item.value)">
				{{ item.label }}
			</view>
		</view>

		<view v-if="loading" class="empty">加载中...</view>
		<view v-else-if="errorText" class="empty error" @tap="refreshAll">{{ errorText }}，点击重试</view>
		<block v-else>
			<view class="checkin-list">
				<view class="checkin-card" v-for="item in displayList" :key="item.checkinId || item.reservationId">
					<view class="card-head">
						<view>
							<view class="card-title">{{ item.memberName || '会员' }}</view>
							<view class="card-meta">{{ item.courseName || '课程待定' }} · {{ formatDateTime(item.checkinTime || item.startTime) }}</view>
						</view>
						<text class="status-tag" :class="activeTab">{{ tabLabel(activeTab) }}</text>
					</view>
					<view class="info-row">签到门店：{{ item.storeName || '门店待定' }}</view>
					<view class="info-row">会员卡：{{ item.cardName || '无卡签到' }}</view>
					<view class="info-row">备注：{{ item.remark || '无' }}</view>
					<view class="action-row" v-if="activeTab === 'pending'">
						<view class="action-btn primary" :class="{ disabled: actioningReservationId === item.reservationId }" @tap="item.checkinId ? confirmItem(item) : proxyItem(item)">
							{{ actioningReservationId === item.reservationId ? '处理中...' : (item.checkinId ? '确认核销' : '代签到') }}
						</view>
						<view class="action-btn ghost" :class="{ disabled: actioningReservationId === item.reservationId }" v-if="item.checkinId" @tap="markException(item)">
							{{ actioningReservationId === item.reservationId ? '处理中...' : '标记异常' }}
						</view>
					</view>
				</view>
			</view>
			<view v-if="!displayList.length" class="empty">当前暂无记录</view>
		</block>

		<view class="bottom-nav">
			<view class="nav-item" @tap="goPage('/pages/coach/home/index')">工作台</view>
			<view class="nav-item" @tap="goPage('/pages/coach/schedule/index')">排课</view>
			<view class="nav-item active">核销</view>
			<view class="nav-item" @tap="goPage('/pages/coach/student/index')">学员</view>
			<view class="nav-item" @tap="goPage('/pages/coach/mine/index')">我的</view>
		</view>
	</view>
</template>

<script>
	import { coachCheckinList, coachConfirmCheckin, coachExceptionCheckin, coachProxyCheckin } from '../../../api/auth'

	export default {
		data() {
			return {
				activeTab: 'pending',
				tabs: [
					{ label: '待核销', value: 'pending' },
					{ label: '已完成', value: 'confirmed' },
					{ label: '异常', value: 'exception' }
				],
				dataMap: {
					pending: [],
					confirmed: [],
					exception: []
				},
				loading: false,
				errorText: '',
				actioningReservationId: null
			}
		},
		computed: {
			displayList() {
				return this.dataMap[this.activeTab] || []
			},
			pendingCount() {
				return (this.dataMap.pending || []).length
			},
			confirmedCount() {
				return (this.dataMap.confirmed || []).length
			},
			exceptionCount() {
				return (this.dataMap.exception || []).length
			}
		},
		onShow() {
			this.refreshAll()
		},
		methods: {
            proxyItem(item) { uni.showModal({title:'为会员代签到',content:'请确认会员已到场。操作人、时间和来源将写入管理员行为记录。',editable:true,placeholderText:'请填写代签到原因',success:async res=>{if(!res.confirm)return; if(!res.content?.trim()){uni.showToast({title:'请填写原因',icon:'none'});return} this.actioningReservationId=item.reservationId; let source='mini';
                // #ifdef H5
                source='web';
                // #endif
                try{await coachProxyCheckin({reservationId:item.reservationId,remark:res.content,source});await this.refreshAll()}catch(e){uni.showToast({title:e.message,icon:'none'})}finally{this.actioningReservationId=null}}}) },
			tabLabel(value) {
				const map = { pending: '待核销', confirmed: '已完成', exception: '异常' }
				return map[value] || '状态'
			},
			async refreshAll() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					const [pending, confirmed, exception] = await Promise.all([
						coachCheckinList({ status: 'pending', pageNum: 1, pageSize: 30 }),
						coachCheckinList({ status: 'confirmed', pageNum: 1, pageSize: 30 }),
						coachCheckinList({ status: 'exception', pageNum: 1, pageSize: 30 })
					])
					this.dataMap = {
						pending: (pending && pending.rows) || [],
						confirmed: (confirmed && confirmed.rows) || [],
						exception: (exception && exception.rows) || []
					}
				} catch (error) {
					this.errorText = error.message || '核销列表加载失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			changeTab(tab) {
				if (this.activeTab === tab) {
					return
				}
				this.activeTab = tab
			},
			goPage(url) {
				const pages = getCurrentPages()
				const current = pages[pages.length - 1]
				if (current && `/${current.route}` === url) return
				uni.redirectTo({ url })
			},
			confirmDialog(content) {
				return new Promise((resolve) => {
					uni.showModal({
						title: '请确认',
						content,
						success: (res) => resolve(!!res.confirm),
						fail: () => resolve(false)
					})
				})
			},
			async confirmItem(item) {
				if (!item || !item.reservationId || this.actioningReservationId) {
					return
				}
				let consumeAmount = 0
				if (item.cardId && item.planType === 'stored_card') {
					const input = await new Promise(resolve => uni.showModal({ title: '储值卡核销金额（元）', editable: true, placeholderText: '请输入本次线下确认的金额', success: resolve, fail: () => resolve({ confirm: false }) }))
					if (!input.confirm) return
					consumeAmount = Number(input.content)
					if (!Number.isFinite(consumeAmount) || consumeAmount <= 0 || !/^\d+(\.\d{1,2})?$/.test(input.content.trim())) {
						uni.showToast({ title: '请输入正确金额，最多两位小数', icon: 'none' })
						return
					}
				}
				const ok = await this.confirmDialog(consumeAmount ? `确认从该储值卡核销 ${consumeAmount} 元吗？` : '确认该签到已完成核销吗？')
				if (!ok) {
					return
				}
				this.actioningReservationId = item.reservationId
				try {
					uni.showLoading({ title: '核销中...' })
					await coachConfirmCheckin({
						reservationId: item.reservationId,
						consumeTimes: item.cardId && !['time_card', 'stored_card'].includes(item.planType) ? 1 : 0,
						consumeAmount,
						remark: '教练端确认核销'
					})

					uni.showToast({ title: '核销成功', icon: 'success' })
					await this.refreshAll()
				} catch (error) {
					uni.showToast({ title: error.message || '核销失败', icon: 'none' })
				} finally {
					this.actioningReservationId = null
					uni.hideLoading()
				}
			},
			async markException(item) {
				if (!item || !item.reservationId || this.actioningReservationId) {
					return
				}
				const ok = await this.confirmDialog('确认将该签到标记为异常吗？')
				if (!ok) {
					return
				}
				this.actioningReservationId = item.reservationId
				try {
					uni.showLoading({ title: '提交中...' })
					await coachExceptionCheckin({
						reservationId: item.reservationId,
						remark: '教练端标记异常'
					})
					uni.showToast({ title: '已标记异常', icon: 'success' })
					await this.refreshAll()
				} catch (error) {
					uni.showToast({ title: error.message || '标记失败', icon: 'none' })
				} finally {
					this.actioningReservationId = null
					uni.hideLoading()
				}
			},
			formatDateTime(value) {
				if (!value) return '待定'
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) return '待定'
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mm = `${date.getMinutes()}`.padStart(2, '0')
				return `${date.getMonth() + 1}/${date.getDate()} ${hh}:${mm}`
			}
		}
	}
</script>

<style lang="scss">
	.checkin-page { padding: 24rpx 24rpx 160rpx; }
	.header-card, .checkin-card, .stats-card { background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05); }
	.header-card, .card-head, .action-row, .bottom-nav { display: flex; align-items: center; justify-content: space-between; }
	.header-card, .checkin-card { padding: 28rpx; }
	.header-title, .card-title { font-size: 32rpx; font-weight: 700; color: #111827; }
	.header-desc, .card-meta, .info-row, .stats-label, .empty { font-size: 24rpx; line-height: 1.7; color: #6b7280; }
	.header-desc, .card-meta, .info-row { margin-top: 10rpx; }
	.header-badge, .status-tag { padding: 10rpx 18rpx; border-radius: 999rpx; font-size: 22rpx; }
	.header-badge, .status-tag.pending { background: #ffedd5; color: #ea580c; }
	.status-tag.confirmed { background: #dcfce7; color: #166534; }
	.status-tag.exception { background: #fee2e2; color: #b91c1c; }
	.stats-grid { margin: 24rpx 0; display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; }
	.stats-card { padding: 24rpx 12rpx; text-align: center; }
	.stats-value { display: block; font-size: 36rpx; font-weight: 700; color: #ff5a36; }
	.stats-label { display: block; margin-top: 12rpx; }
	.filter-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
	.filter-chip { padding: 14rpx 26rpx; border-radius: 999rpx; background: #fff; font-size: 24rpx; color: #4b5563; }
	.filter-chip.active { background: #111827; color: #fff; font-weight: 700; }
	.checkin-list { display: flex; flex-direction: column; gap: 18rpx; }
	.action-row { margin-top: 20rpx; gap: 16rpx; }
	.action-btn { flex: 1; padding: 18rpx 0; border-radius: 999rpx; text-align: center; font-size: 24rpx; font-weight: 700; }
	.action-btn.primary { background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%); color: #fff; }
	.action-btn.ghost { background: #fff7ed; color: #ea580c; }
	.action-btn.disabled { opacity: 0.6; }
	.empty { text-align: center; padding: 40rpx 0 10rpx; }
	.empty.error { color: #dc2626; }
	.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; padding: 18rpx 20rpx; border-radius: 999rpx; background: rgba(17, 24, 39, 0.94); color: #cbd5e1; box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.18); }
	.nav-item { flex: 1; text-align: center; font-size: 24rpx; }
	.nav-item.active { color: #fff; font-weight: 700; }
</style>
