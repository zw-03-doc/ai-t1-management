<template>
	<view class="page-wrap">
		<view class="panel">
			<view class="title">会员课程中心</view>
			<view class="chips">
				<text
					class="chip"
					:class="{ active: currentTab === item.value }"
					v-for="item in tabs"
					:key="item.value"
					@tap="switchTab(item.value)"
				>
					{{ item.label }}
				</text>
			</view>
		</view>

		<view v-if="currentTab === 'course'" class="panel">
			<view class="sub-head">
				<text class="sub-title">课程列表</text>
				<text class="sub-more">{{ courseList.length }} 门</text>
			</view>
			<view class="chips">
				<text class="chip" :class="{ active: courseType === '' }" @tap="changeCourseType('')">全部</text>
				<text class="chip" :class="{ active: courseType === 'group' }" @tap="changeCourseType('group')">团课</text>
				<text class="chip" :class="{ active: courseType === 'private' }" @tap="changeCourseType('private')">私教</text>
				<text class="chip" :class="{ active: courseType === 'experience' }" @tap="changeCourseType('experience')">体验课</text>
			</view>
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData">{{ errorText }}，点击重试</view>
			<block v-else>
				<view class="card" v-for="item in courseList" :key="item.courseId" @tap="openCourse(item)">
					<view class="card-head">
						<text class="card-title">{{ item.courseName }}</text>
						<text class="card-tag">{{ item.courseType || '课程' }}</text>
					</view>
					<view class="card-desc">{{ item.storeName || '门店待定' }} · 教练 {{ item.coachName || '待安排' }}</view>
					<view class="card-desc">展示价 ¥{{ item.displayPrice || 0 }} · 时长 {{ item.durationMinutes || 0 }} 分钟 · 容量 {{ item.capacity || 0 }} 人</view>
				</view>
				<view v-if="!courseList.length" class="empty">暂无课程数据</view>
			</block>
		</view>

		<view v-if="currentTab === 'schedule'" class="panel">
			<view class="sub-head">
				<text class="sub-title">可预约排课</text>
				<text class="sub-more">{{ scheduleList.length }} 条</text>
			</view>
			<view class="actions" style="margin-top: 12rpx; justify-content: flex-start;">
				<button class="btn ghost" @tap="toggleBatchDateEditor">{{ batchDateEditorVisible ? '收起批量日期行' : '批量日期行预约' }}</button>
			</view>
			<view v-if="batchDateEditorVisible" class="batch-editor">
				<view class="batch-title">批量日期行编辑器</view>
				<view class="batch-tip">先选门店/课程/教练（可选），再按行填写日期和时间段。</view>
				<view class="batch-filter-row">
					<input class="batch-input" v-model="batchFilters.storeName" placeholder="筛选门店（可选）" />
					<input class="batch-input" v-model="batchFilters.courseName" placeholder="筛选课程（可选）" />
					<input class="batch-input" v-model="batchFilters.coachName" placeholder="筛选教练（可选）" />
				</view>
				<view class="batch-row" v-for="(row, idx) in batchDateRows" :key="`row-${idx}`">
					<picker mode="date" :value="row.scheduleDate" @change="(e) => updateBatchRowDate(idx, e.detail.value)">
						<view class="picker-box">{{ row.scheduleDate || '选择日期' }}</view>
					</picker>
					<picker mode="time" :value="row.startTime" @change="(e) => updateBatchRowStart(idx, e.detail.value)">
						<view class="picker-box">{{ row.startTime || '开始时间' }}</view>
					</picker>
					<picker mode="time" :value="row.endTime" @change="(e) => updateBatchRowEnd(idx, e.detail.value)">
						<view class="picker-box">{{ row.endTime || '结束时间' }}</view>
					</picker>
					<text class="row-del" @tap="removeBatchRow(idx)">删除</text>
				</view>
				<view class="actions" style="margin-top: 12rpx; justify-content: flex-start;">
					<button class="btn ghost" @tap="addBatchRow">新增日期行</button>
					<button class="btn primary" :disabled="isActioning('batchReserve', 0)" @tap="batchReserveByDateRows">
						{{ isActioning('batchReserve', 0) ? '批量预约中...' : '提交批量预约' }}
					</button>
				</view>
			</view>
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData">{{ errorText }}，点击重试</view>
			<block v-else>
				<view class="card" v-for="item in scheduleList" :key="item.scheduleId">
					<view class="card-head">
						<text class="card-title">{{ item.courseName }}</text>
						<text class="card-tag">余位 {{ item.availableCount || 0 }}</text>
					</view>
					<view class="card-desc">{{ item.storeName || '门店待定' }} · 教练 {{ item.coachName || '待安排' }}</view>
					<view class="card-desc">{{ formatDateTime(item.startTime) }} - {{ formatTime(item.endTime) }}</view>
					<view class="actions">
						<button class="btn ghost" :disabled="item.reservedByMe || isActioning('reserve', item.scheduleId)" @tap="reserveSchedule(item)">
							{{ item.reservedByMe ? '已预约' : (isActioning('reserve', item.scheduleId) ? '预约中...' : '立即预约') }}
						</button>
					</view>
				</view>
				<view v-if="!scheduleList.length" class="empty">暂无可预约排课</view>
			</block>
		</view>

		<view v-if="currentTab === 'reservation'" class="panel">
			<view class="sub-head">
				<text class="sub-title">我的预约</text>
				<text class="sub-more">{{ reservationList.length }} 条</text>
			</view>
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData">{{ errorText }}，点击重试</view>
			<block v-else>
				<view class="card" v-for="item in reservationList" :key="item.reservationId">
					<view class="card-head">
						<text class="card-title">{{ item.courseName }}</text>
						<text class="card-tag">{{ reservationText(item.reservationStatus) }}</text>
					</view>
					<view class="card-desc">{{ item.storeName || '门店待定' }} · 教练 {{ item.coachName || '待安排' }}</view>
					<view class="card-desc">{{ formatDateTime(item.startTime) }} - {{ formatTime(item.endTime) }}</view>
					<view class="card-desc">签到状态：{{ checkinText(item.derivedCheckinStatus) }}</view>
					<view class="actions">
						<button
							class="btn ghost"
							v-if="item.reservationStatus === 'reserved'"
							:disabled="isActioning('cancel', item.reservationId)"
							@tap="cancelReservation(item)"
						>
							{{ isActioning('cancel', item.reservationId) ? '取消中...' : '取消预约' }}
						</button>
						<button
							class="btn primary"
							v-if="item.reservationStatus === 'reserved'"
							:disabled="isActioning('checkin', item.reservationId)"
							@tap="memberCheckin(item)"
						>
							{{ isActioning('checkin', item.reservationId) ? '签到中...' : '我要签到' }}
						</button>
					</view>
				</view>
				<view v-if="!reservationList.length" class="empty">暂无预约记录</view>
			</block>
		</view>

		<view v-if="currentTab === 'card'" class="panel">
			<view class="sub-head">
				<text class="sub-title">我的卡项</text>
				<text class="sub-more">{{ cardList.length }} 张</text>
			</view>
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData">{{ errorText }}，点击重试</view>
			<block v-else>
				<view class="card" v-for="item in cardList" :key="item.cardId">
					<view class="card-head">
						<text class="card-title">{{ item.cardName }}</text>
						<text class="card-tag">{{ item.cardStatus }}</text>
					</view>
					<view class="card-desc">教练 {{ item.coachName || '未绑定' }}</view>
					<view class="card-desc">剩余 {{ remainTimesText(item.remainTimes) }} 次 · 余额 ¥{{ formatAmount(item.balanceAmount) }}</view>
				</view>
				<view v-if="!cardList.length" class="empty">暂无卡项权益</view>
			</block>
		</view>

		<view v-if="currentTab === 'order'" class="panel">
			<view class="sub-head">
				<text class="sub-title">我的订单</text>
				<text class="sub-more">{{ orderList.length }} 笔</text>
			</view>
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="errorText" class="empty error" @tap="loadCurrentTabData">{{ errorText }}，点击重试</view>
			<block v-else>
				<view class="card" v-for="item in orderList" :key="item.orderId">
					<view class="card-head">
						<text class="card-title">{{ item.orderNo }}</text>
						<text class="card-tag">{{ item.orderStatus }}</text>
					</view>
					<view class="card-desc">{{ item.storeName || '门店待定' }} · {{ item.orderType || 'buy_card' }}</view>
					<view class="card-desc">实付 ¥{{ formatAmount(item.paidAmount) }} / 应付 ¥{{ formatAmount(item.payableAmount) }}</view>
				</view>
				<view v-if="!orderList.length" class="empty">暂无订单记录</view>
			</block>
		</view>
	</view>
</template>

<script>
	import {
		memberCourseList,
		memberScheduleList,
		memberCreateReservation,
		memberReservationList,
		memberCancelReservation,
		memberCreateCheckin,
		memberMyCards,
		memberMyOrders
	} from '../../api/auth'

	export default {
		data() {
			return {
				tabs: [
					{ label: '课程', value: 'course' },
					{ label: '排课', value: 'schedule' },
					{ label: '预约', value: 'reservation' },
					{ label: '卡项', value: 'card' },
					{ label: '订单', value: 'order' }
				],
				currentTab: 'course',
				courseType: '',
				courseList: [],
				scheduleList: [],
				reservationList: [],
				cardList: [],
				orderList: [],
				loading: false,
				errorText: '',
				actioningKey: '',
				batchDateEditorVisible: false,
				batchFilters: {
					storeName: '',
					courseName: '',
					coachName: ''
				},
				batchDateRows: [
					{ scheduleDate: '', startTime: '', endTime: '' }
				]
			}
		},
		onLoad(options) {
			const tab = options && options.tab
			if (['course', 'schedule', 'reservation', 'card', 'order', 'plan'].includes(tab)) {
				this.currentTab = tab === 'plan' ? 'card' : tab
			}
		},
		onShow() {
			this.loadCurrentTabData()
		},
		methods: {
            openCourse(item) { uni.navigateTo({url:'/pages/course/detail?id='+item.courseId}) },
			switchTab(tab) {
				if (this.currentTab === tab || this.loading) {
					return
				}
				this.currentTab = tab
				this.loadCurrentTabData()
			},
			async loadCurrentTabData() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					if (this.currentTab === 'course') {
						this.courseList = (await memberCourseList({ courseType: this.courseType })) || []
						return
					}
					if (this.currentTab === 'schedule') {
						const table = (await memberScheduleList({ pageNum: 1, pageSize: 20 })) || {}
						this.scheduleList = table.rows || []
						return
					}
					if (this.currentTab === 'reservation') {
						const table = (await memberReservationList({ pageNum: 1, pageSize: 20 })) || {}
						this.reservationList = table.rows || []
						return
					}
					if (this.currentTab === 'card') {
						this.cardList = (await memberMyCards()) || []
						return
					}
					if (this.currentTab === 'order') {
						const table = (await memberMyOrders({ pageNum: 1, pageSize: 20 })) || {}
						this.orderList = table.rows || []
					}
				} catch (error) {
					this.errorText = error.message || '数据加载失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			async changeCourseType(value) {
				if (this.courseType === value || this.loading) {
					return
				}
				this.courseType = value
				await this.loadCurrentTabData()
			},
			buildActionKey(type, id) {
				return `${type}-${id}`
			},
			isActioning(type, id) {
				return this.actioningKey === this.buildActionKey(type, id)
			},
			setActioning(type, id) {
				this.actioningKey = this.buildActionKey(type, id)
			},
			clearActioning() {
				this.actioningKey = ''
			},
			confirmAction(content) {
				return new Promise((resolve) => {
					uni.showModal({
						title: '请确认',
						content,
						success: (res) => resolve(!!res.confirm),
						fail: () => resolve(false)
					})
				})
			},
			toggleBatchDateEditor() {
				this.batchDateEditorVisible = !this.batchDateEditorVisible
			},
			addBatchRow() {
				this.batchDateRows.push({ scheduleDate: '', startTime: '', endTime: '' })
			},
			removeBatchRow(index) {
				if (this.batchDateRows.length <= 1) {
					uni.showToast({ title: '至少保留1行', icon: 'none' })
					return
				}
				this.batchDateRows.splice(index, 1)
			},
			updateBatchRowDate(index, value) {
				this.batchDateRows[index].scheduleDate = value
			},
			updateBatchRowStart(index, value) {
				this.batchDateRows[index].startTime = value
			},
			updateBatchRowEnd(index, value) {
				this.batchDateRows[index].endTime = value
			},
			normalizeText(value) {
				return (value || '').toString().trim().toLowerCase()
			},
			matchBatchFilter(item) {
				const storeFilter = this.normalizeText(this.batchFilters.storeName)
				const courseFilter = this.normalizeText(this.batchFilters.courseName)
				const coachFilter = this.normalizeText(this.batchFilters.coachName)
				const storeName = this.normalizeText(item.storeName)
				const courseName = this.normalizeText(item.courseName)
				const coachName = this.normalizeText(item.coachName)
				if (storeFilter && !storeName.includes(storeFilter)) {
					return false
				}
				if (courseFilter && !courseName.includes(courseFilter)) {
					return false
				}
				if (coachFilter && !coachName.includes(coachFilter)) {
					return false
				}
				return true
			},
			findScheduleForBatchRow(row) {
				if (!row || !row.scheduleDate || !row.startTime || !row.endTime) {
					return null
				}
				return this.scheduleList.find((item) => {
					if (!item || !item.scheduleId || item.reservedByMe || !this.matchBatchFilter(item)) {
						return false
					}
					const startDate = this.formatDateOnly(item.startTime)
					const startClock = this.formatTime(item.startTime)
					const endClock = this.formatTime(item.endTime)
					return startDate === row.scheduleDate && startClock === row.startTime && endClock === row.endTime
				}) || null
			},
			formatDateOnly(value) {
				if (!value) {
					return ''
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return ''
				}
				const y = date.getFullYear()
				const m = `${date.getMonth() + 1}`.padStart(2, '0')
				const d = `${date.getDate()}`.padStart(2, '0')
				return `${y}-${m}-${d}`
			},
			async buildReservationPayload(item, cardCache = {}) {
				const payload = { scheduleId: item.scheduleId }
				if (item.courseType === 'private' || item.courseType === 'small') {
					const coachKey = `${item.storeId || 'none'}:${item.coachId || 'none'}`
					let cardId = cardCache[coachKey]
					if (!cardId) {
						cardId = await this.pickReservationCard(item)
						if (!cardId) {
							throw new Error('已取消选择，剩余课程未预约')
						}
						cardCache[coachKey] = cardId
					}
					if (cardId !== 'without-card') payload.cardId = cardId
				}
				return payload
			},
			async batchReserveByDateRows() {
				if (this.actioningKey || !this.batchDateRows.length) {
					return
				}
				const invalidIndex = this.batchDateRows.findIndex((row) => !row.scheduleDate || !row.startTime || !row.endTime)
				if (invalidIndex >= 0) {
					uni.showToast({ title: `第${invalidIndex + 1}行未填写完整`, icon: 'none' })
					return
				}
				const matched = []
				const unmatched = []
				this.batchDateRows.forEach((row, idx) => {
					const found = this.findScheduleForBatchRow(row)
					if (found) {
						matched.push(found)
					} else {
						unmatched.push(idx + 1)
					}
				})
				if (!matched.length) {
					uni.showToast({ title: '未匹配到可预约排课', icon: 'none' })
					return
				}
				if (unmatched.length) {
					uni.showToast({ title: `第${unmatched.join(',')}行未匹配到排课`, icon: 'none' })
					return
				}
				const uniqueSchedules = Array.from(new Map(matched.map((item) => [item.scheduleId, item])).values())
				const confirmed = await this.confirmAction(`确认批量预约 ${uniqueSchedules.length} 条排课吗？`)
				if (!confirmed) {
					return
				}
				this.setActioning('batchReserve', 0)
				let success = 0
				try {
					uni.showLoading({ title: '批量预约中...' })
					const cardCache = {}
					for (const item of uniqueSchedules) {
						const payload = await this.buildReservationPayload(item, cardCache)
						await memberCreateReservation(payload)
						success += 1
					}
					uni.showToast({ title: `批量预约成功 ${success} 条`, icon: 'success' })
					await Promise.all([
						this.currentTab === 'schedule' ? this.loadCurrentTabData() : Promise.resolve(),
						this.refreshReservations()
					])
				} catch (error) {
					uni.showToast({ title: error.message || `批量预约失败（成功${success}条）`, icon: 'none' })
				} finally {
					this.clearActioning()
					uni.hideLoading()
				}
			},
			async reserveSchedule(item) {

				if (!item || !item.scheduleId || this.actioningKey) {
					return
				}
				this.setActioning('reserve', item.scheduleId)
				try {
					uni.showLoading({ title: '预约中...' })
					const payload = { scheduleId: item.scheduleId }
					if (item.courseType === 'private' || item.courseType === 'small') {
						const cardId = await this.pickReservationCard(item)
						if (!cardId) {
							return
						}
						if (cardId !== 'without-card') payload.cardId = cardId
					}
					await memberCreateReservation(payload)
					uni.showToast({ title: '预约成功', icon: 'success' })
					await Promise.all([
						this.currentTab === 'schedule' ? this.loadCurrentTabData() : Promise.resolve(),
						this.refreshReservations()
					])
				} catch (error) {
					uni.showToast({ title: error.message || '预约失败', icon: 'none' })
				} finally {
					this.clearActioning()
					uni.hideLoading()
				}
			},
			async pickReservationCard(scheduleItem) {
				const cards = (await memberMyCards()) || []
				const availableCards = cards.filter((card) => {
					if (!card || !card.cardId) {
						return false
					}
					if (card.planType === 'time_card' || card.planType === 'stored_card') {
						return false
					}
					if (card.bindCoachId && scheduleItem.coachId && card.bindCoachId !== scheduleItem.coachId) {
						return false
					}
					return true
				})
				uni.hideLoading()
				return new Promise((resolve) => {
					uni.showActionSheet({
						itemList: ['无卡预约 · 线下咨询', ...availableCards.map((card) => `${card.cardName || '卡项'}（${card.coachName || '未绑定教练'}）`)],
						success: (res) => resolve(res.tapIndex === 0 ? 'without-card' : availableCards[res.tapIndex - 1].cardId),
						fail: () => resolve(null)
					})
				})
			},

			async cancelReservation(item) {
				if (!item || !item.reservationId || this.actioningKey) {
					return
				}
				const confirmed = await this.confirmAction('确认取消本次预约吗？')
				if (!confirmed) {
					return
				}
				this.setActioning('cancel', item.reservationId)
				try {
					uni.showLoading({ title: '取消中...' })
					await memberCancelReservation(item.reservationId)
					uni.showToast({ title: '已取消预约', icon: 'success' })
					await this.refreshReservations()
				} catch (error) {
					uni.showToast({ title: error.message || '取消失败', icon: 'none' })
				} finally {
					this.clearActioning()
					uni.hideLoading()
				}
			},
			async memberCheckin(item) {
				if (!item || !item.reservationId || this.actioningKey) {
					return
				}
				this.setActioning('checkin', item.reservationId)
				try {
					uni.showLoading({ title: '签到中...' })
					await memberCreateCheckin({ reservationId: item.reservationId, cardId: item.cardId })
					uni.showToast({ title: '签到成功，待教练核销', icon: 'success' })
					await this.refreshReservations()
				} catch (error) {
					uni.showToast({ title: error.message || '签到失败', icon: 'none' })
				} finally {
					this.clearActioning()
					uni.hideLoading()
				}
			},
			async refreshReservations() {
				const table = (await memberReservationList({ pageNum: 1, pageSize: 20 })) || {}
				this.reservationList = table.rows || []
				if (this.currentTab === 'schedule') {
					const scheduleTable = (await memberScheduleList({ pageNum: 1, pageSize: 20 })) || {}
					this.scheduleList = scheduleTable.rows || []
				}
			},
			reservationText(value) {
				const map = { reserved: '已预约', cancelled: '已取消', signed: '已签到', absent: '缺席', leave: '请假' }
				return map[value] || value || '未知'
			},
			checkinText(value) {
				const map = {
					not_started: '未签到',
					pending_confirm: '待核销',
					confirmed: '已核销',
					exception: '异常'
				}
				return map[value] || value || '未开始'
			},
			formatDateTime(value) {
				if (!value) {
					return '待定'
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return '待定'
				}
				return `${date.getMonth() + 1}/${date.getDate()} ${this.formatTime(value)}`
			},
			formatTime(value) {
				if (!value) {
					return '--:--'
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return '--:--'
				}
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mm = `${date.getMinutes()}`.padStart(2, '0')
				return `${hh}:${mm}`
			},
			formatAmount(value) {
				return Number(value || 0).toFixed(2)
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
	.page-wrap {
		padding: 24rpx;
	}

	.panel {
		margin-bottom: 20rpx;
		padding: 24rpx;
		background: #fff;
		border-radius: 28rpx;
	}

	.title,
	.sub-title,
	.card-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #111827;
	}

	.sub-head,
	.card-head,
	.actions {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 14rpx;
	}

	.sub-more,
	.card-desc,
	.empty {
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.chips {
		margin-top: 18rpx;
		display: flex;
		gap: 12rpx;
		flex-wrap: wrap;
	}

	.chip,
	.card-tag {
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
	}

	.chip {
		background: #f3f4f6;
		color: #4b5563;
	}

	.chip.active {
		background: #111827;
		color: #fff;
	}

	.card {
		margin-top: 14rpx;
		padding: 22rpx;
		border-radius: 24rpx;
		background: #f9fafb;
	}

	.card-tag {
		background: #ffedd5;
		color: #ea580c;
	}

	.card-desc {
		margin-top: 10rpx;
	}

	.actions {
		margin-top: 16rpx;
	}

	.btn {
		margin: 0;
		border-radius: 999rpx;
		height: 68rpx;
		line-height: 68rpx;
		font-size: 24rpx;
		padding: 0 28rpx;
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

	.empty {
		text-align: center;
		padding: 32rpx 0 10rpx;
	}

	.empty.error {
		color: #dc2626;
	}

	.batch-editor {
		margin-top: 14rpx;
		padding: 20rpx;
		border-radius: 20rpx;
		background: #fff7ed;
	}

	.batch-title {
		font-size: 26rpx;
		font-weight: 600;
		color: #111827;
	}

	.batch-tip {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #6b7280;
	}

	.batch-filter-row {
		margin-top: 12rpx;
		display: grid;
		grid-template-columns: 1fr;
		gap: 10rpx;
	}

	.batch-input,
	.picker-box {
		height: 64rpx;
		line-height: 64rpx;
		padding: 0 18rpx;
		border-radius: 14rpx;
		background: #fff;
		font-size: 24rpx;
		color: #374151;
	}

	.batch-row {
		margin-top: 12rpx;
		display: grid;
		grid-template-columns: 1fr 1fr 1fr auto;
		gap: 10rpx;
		align-items: center;
	}

	.row-del {
		font-size: 24rpx;
		color: #dc2626;
	}
</style>

