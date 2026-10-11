<template>
	<view class="mine-page">
		<view class="profile-card">
			<image class="avatar" :src="form.avatarUrl || '/static/images/brand-icon.jpg'" mode="aspectFill" />
			<view class="profile-main">
				<view class="profile-name">{{ displayName }}</view>
				<view class="profile-desc">{{ profileDesc }}</view>
			</view>
		</view>

		<view class="stats-grid">
			<view class="stats-item" v-for="item in stats" :key="item.label">
				<text class="stats-value">{{ item.value }}</text>
				<text class="stats-label">{{ item.label }}</text>
			</view>
		</view>

		<view v-if="errorText" class="error-tip" @tap="loadData">{{ errorText }}，点击重试</view>

		<view class="form-card">
			<view class="form-title">资料维护</view>
			<view class="form-item"><text class="label">昵称</text><input v-model.trim="form.nickName" class="input" placeholder="请输入昵称" /></view>
			<view class="form-item"><text class="label">姓名</text><input v-model.trim="form.coachName" class="input" placeholder="请输入姓名" /></view>
			<view class="form-item"><text class="label">手机号</text><input v-model.trim="form.mobile" class="input" type="number" maxlength="11" placeholder="请输入手机号" /></view>
			<view class="form-item"><text class="label">标签</text><input v-model.trim="form.skillTags" class="input" placeholder="如：增肌,减脂" /></view>
			<view class="form-item no-border"><text class="label">简介</text><textarea v-model.trim="form.intro" class="textarea" maxlength="200" placeholder="请输入简介" /></view>
			<button class="submit-btn" :disabled="savingProfile || loading" @tap="saveProfile">{{ savingProfile ? '保存中...' : '保存资料' }}</button>
		</view>

		<view class="form-card">
			<view class="form-title">密码修改</view>
			<view class="form-item"><text class="label">旧密码</text><input v-model.trim="passwordForm.oldPassword" class="input" password placeholder="请输入旧密码" /></view>
			<view class="form-item"><text class="label">新密码</text><input v-model.trim="passwordForm.newPassword" class="input" password placeholder="请输入新密码" /></view>
			<view class="form-item no-border"><text class="label">确认密码</text><input v-model.trim="passwordForm.confirmPassword" class="input" password placeholder="请再次输入新密码" /></view>
			<button class="submit-btn ghost" :disabled="savingPassword || loading" @tap="savePassword">{{ savingPassword ? '提交中...' : '修改密码' }}</button>
		</view>

		<view class="form-card">
			<view class="form-title">消息提醒</view>
			<view v-if="loading" class="empty">加载中...</view>
			<block v-else>
				<view class="message-item" v-for="(item, index) in messageList" :key="index">
					<view class="message-title">{{ item.title }}</view>
					<view class="message-desc">{{ item.content }}</view>
					<view class="message-time">{{ formatDateTime(item.eventTime) }}</view>
				</view>
				<view v-if="!messageList.length" class="empty">暂无消息</view>
			</block>
		</view>

		<button class="logout-btn" :disabled="loggingOut" @tap="logout">{{ loggingOut ? '退出中...' : '退出登录' }}</button>

		<view class="bottom-nav">
			<view class="nav-item" @tap="goPage('/pages/coach/home/index')">工作台</view>
			<view class="nav-item" @tap="goPage('/pages/coach/schedule/index')">排课</view>
			<view class="nav-item" @tap="goPage('/pages/coach/checkin/index')">核销</view>
			<view class="nav-item" @tap="goPage('/pages/coach/student/index')">学员</view>
			<view class="nav-item active">我的</view>
		</view>
	</view>
</template>

<script>
	import { coachMessageList, coachProfile, coachUpdatePassword, coachUpdateProfile, coachLogout } from '../../../api/auth'
	import { clearCoachSession, getCoachSession, saveCoachSession } from '../../../utils/auth'

	export default {
		data() {
			return {
				profile: {},
				form: {
					nickName: '',
					coachName: '',
					mobile: '',
					avatarUrl: '',
					skillTags: '',
					intro: ''
				},
				passwordForm: {
					oldPassword: '',
					newPassword: '',
					confirmPassword: ''
				},
				messageList: [],
				loading: false,
				errorText: '',
				savingProfile: false,
				savingPassword: false,
				loggingOut: false
			}
		},
		computed: {
			displayName() {
				return this.profile.coachName || this.profile.nickName || 'T1 教练'
			},
			profileDesc() {
				const pieces = [
					this.profile.levelCode || '教练档案已同步',
					this.profile.storeName || '门店待分配',
					this.profile.skillTags || '擅长标签待完善'
				]
				return pieces.filter(Boolean).join(' · ')
			},
			stats() {
				const session = getCoachSession() || {}
				return [
					{ label: '身份状态', value: session.activated ? '已激活' : '待激活' },
					{ label: '所属门店', value: this.profile.storeName || '待分配' },
					{ label: '联系方式', value: this.profile.mobile || this.profile.phonenumber || '未维护' }
				]
			}
		},
		onShow() {
			this.loadData()
		},
		methods: {
			async loadData() {
				if (this.loading) {
					return
				}
				this.loading = true
				this.errorText = ''
				try {
					const [profile, messageTable] = await Promise.all([
						coachProfile(),
						coachMessageList({ pageNum: 1, pageSize: 10 })
					])
					this.profile = profile || {}
					this.form = {
						nickName: (profile && profile.nickName) || '',
						coachName: (profile && profile.coachName) || '',
						mobile: (profile && profile.mobile) || '',
						avatarUrl: (profile && profile.avatarUrl) || '',
						skillTags: (profile && profile.skillTags) || '',
						intro: (profile && profile.intro) || ''
					}
					this.messageList = (messageTable && messageTable.rows) || []
					const session = getCoachSession()
					saveCoachSession({
						...session,
						profile: {
							...(session.profile || {}),
							...(profile || {})
						}
					})
				} catch (error) {
					this.errorText = error.message || '教练资料加载失败'
					uni.showToast({ title: this.errorText, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			validateProfileForm() {
				if (!this.form.coachName) {
					uni.showToast({ title: '请输入姓名', icon: 'none' })
					return false
				}
				if (this.form.mobile && !/^1\d{10}$/.test(this.form.mobile)) {
					uni.showToast({ title: '手机号格式不正确', icon: 'none' })
					return false
				}
				return true
			},
			async saveProfile() {
				if (this.savingProfile || this.loading) {
					return
				}
				if (!this.validateProfileForm()) {
					return
				}
				this.savingProfile = true
				try {
					uni.showLoading({ title: '保存中...' })
					await coachUpdateProfile({ ...this.form })
					uni.showToast({ title: '资料已保存', icon: 'success' })
					await this.loadData()
				} catch (error) {
					uni.showToast({ title: error.message || '保存失败', icon: 'none' })
				} finally {
					this.savingProfile = false
					uni.hideLoading()
				}
			},
			async savePassword() {
				if (this.savingPassword || this.loading) {
					return
				}
				if (!this.passwordForm.oldPassword || !this.passwordForm.newPassword || !this.passwordForm.confirmPassword) {
					uni.showToast({ title: '请填写完整密码信息', icon: 'none' })
					return
				}
				if (this.passwordForm.newPassword.length < 6) {
					uni.showToast({ title: '新密码至少6位', icon: 'none' })
					return
				}
				if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
					uni.showToast({ title: '两次新密码不一致', icon: 'none' })
					return
				}
				this.savingPassword = true
				try {
					uni.showLoading({ title: '提交中...' })
					await coachUpdatePassword({
						oldPassword: this.passwordForm.oldPassword,
						newPassword: this.passwordForm.newPassword
					})
					this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
					uni.showToast({ title: '密码修改成功', icon: 'success' })
				} catch (error) {
					uni.showToast({ title: error.message || '密码修改失败', icon: 'none' })
				} finally {
					this.savingPassword = false
					uni.hideLoading()
				}
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
			async logout() {
				if (this.loggingOut) {
					return
				}
				const ok = await this.confirmDialog('确认退出当前教练账号吗？')
				if (!ok) {
					return
				}
				this.loggingOut = true
				try {
					await coachLogout()
				} catch (error) {
					// 忽略后端失败，保证前端状态可退出
				} finally {
					clearCoachSession()
					this.loggingOut = false
					uni.reLaunch({ url: '/pages/portal/index?role=coach' })
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
				if (!value) return '刚刚'
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) return '刚刚'
				const hh = `${date.getHours()}`.padStart(2, '0')
				const mm = `${date.getMinutes()}`.padStart(2, '0')
				return `${date.getMonth() + 1}/${date.getDate()} ${hh}:${mm}`
			}
		}
	}
</script>

<style lang="scss">
	.mine-page { padding: 24rpx 24rpx 170rpx; }
	.profile-card, .form-card { padding: 28rpx; background: #fff; border-radius: 28rpx; box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05); margin-bottom: 18rpx; }
	.profile-card, .bottom-nav { display: flex; align-items: center; justify-content: space-between; }
	.profile-card { gap: 20rpx; }
	.avatar { width: 112rpx; height: 112rpx; border-radius: 28rpx; }
	.profile-main { flex: 1; }
	.profile-name { font-size: 32rpx; font-weight: 700; color: #111827; }
	.profile-desc, .stats-label, .message-desc, .message-time, .empty { margin-top: 10rpx; font-size: 24rpx; line-height: 1.7; color: #6b7280; }
	.stats-grid { margin-bottom: 18rpx; display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; }
	.stats-item { padding: 24rpx 12rpx; border-radius: 24rpx; background: linear-gradient(180deg, #fff4ec 0%, #ffffff 100%); text-align: center; }
	.stats-value { display: block; font-size: 30rpx; font-weight: 700; color: #ff5a36; word-break: break-all; }
	.error-tip { margin-bottom: 18rpx; padding: 20rpx; border-radius: 20rpx; font-size: 24rpx; color: #dc2626; background: #fef2f2; }
	.form-title, .message-title { font-size: 30rpx; font-weight: 700; color: #111827; }
	.form-item { min-height: 88rpx; display: flex; align-items: center; gap: 16rpx; border-bottom: 1rpx solid #f3f4f6; }
	.form-item.no-border { border-bottom: none; }
	.label { width: 110rpx; font-size: 26rpx; color: #374151; }
	.input { flex: 1; height: 74rpx; font-size: 26rpx; color: #111827; }
	.textarea { flex: 1; min-height: 130rpx; font-size: 26rpx; color: #111827; padding: 16rpx 0; }
	.submit-btn, .logout-btn { margin-top: 20rpx; border-radius: 999rpx; height: 86rpx; line-height: 86rpx; font-size: 28rpx; font-weight: 700; border: none; }
	.submit-btn[disabled], .logout-btn[disabled] { opacity: 0.65; }
	.submit-btn::after, .logout-btn::after { border: none; }
	.submit-btn { background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%); color: #fff; }
	.submit-btn.ghost { background: #fff7ed; color: #ea580c; }
	.message-item { margin-top: 16rpx; padding: 20rpx; border-radius: 20rpx; background: #f9fafb; }
	.logout-btn { background: #111827; color: #fff; }
	.empty { text-align: center; padding: 18rpx 0 4rpx; }
	.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; padding: 18rpx 20rpx; border-radius: 999rpx; background: rgba(17, 24, 39, 0.94); color: #cbd5e1; box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.18); }
	.nav-item { flex: 1; text-align: center; font-size: 24rpx; }
	.nav-item.active { color: #fff; font-weight: 700; }
</style>
