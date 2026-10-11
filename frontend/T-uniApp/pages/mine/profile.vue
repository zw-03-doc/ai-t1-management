<template>
	<view class="page-wrap">
		<view class="profile-card">
			<image class="avatar" :src="form.avatarUrl || '/static/images/brand-icon.jpg'" mode="aspectFill" />
			<view class="profile-main">
				<view class="profile-title">{{ form.nickname || form.realName || '微信会员' }}</view>
				<view class="profile-desc">可在此完善昵称、姓名、性别、生日与备注信息。</view>
			</view>
		</view>

		<view class="action-row">
			<!-- #ifdef MP-WEIXIN -->
			<button class="ghost-btn" @tap="syncWechatProfile">同步微信头像昵称</button>
			<!-- #endif -->
		</view>

		<view class="form-card">
			<view class="form-item">
				<text class="label">昵称</text>
				<input v-model="form.nickname" class="input" placeholder="请输入昵称" maxlength="20" />
			</view>
			<view class="form-item">
				<text class="label">真实姓名</text>
				<input v-model="form.realName" class="input" placeholder="请输入真实姓名" maxlength="20" />
			</view>
			<view class="form-item readonly">
				<text class="label">手机号</text>
				<text class="value">{{ form.mobile || '暂未绑定，请返回“我的”页面点击微信绑定' }}</text>
			</view>
			<view class="form-item picker-item" @tap="openGenderPicker">
				<text class="label">性别</text>
				<text class="value">{{ currentGenderLabel }}</text>
			</view>
			<picker mode="date" :value="form.birthday" @change="onBirthdayChange">
				<view class="form-item picker-item no-border">
					<text class="label">生日</text>
					<text class="value">{{ form.birthday || '请选择生日' }}</text>
				</view>
			</picker>
		</view>

		<view class="form-card remark-card">
			<view class="remark-title">备注</view>
			<textarea v-model="form.remark" class="remark-input" maxlength="200" placeholder="可填写训练偏好、健康备注等信息" />
		</view>

		<button class="submit-btn" @tap="submitProfile">保存资料</button>
	</view>
</template>

<script>
	import { memberMineProfile, memberUpdateProfile } from '../../api/auth'

	const genderOptions = [
		{ label: '未知', value: '0' },
		{ label: '男', value: '1' },
		{ label: '女', value: '2' }
	]

	export default {
		data() {
			return {
				loading: false,
				form: {
					nickname: '',
					realName: '',
					mobile: '',
					gender: '0',
					birthday: '',
					avatarUrl: '',
					remark: ''
				}
			}
		},
		computed: {
			currentGenderLabel() {
				const matched = genderOptions.find((item) => item.value === this.form.gender)
				return matched ? matched.label : '未知'
			}
		},
		onShow() {
			this.loadProfile()
		},
		methods: {
			formatDate(value) {
				if (!value) {
					return ''
				}
				if (typeof value === 'string') {
					return value.slice(0, 10)
				}
				const date = new Date(value)
				if (Number.isNaN(date.getTime())) {
					return ''
				}
				const year = date.getFullYear()
				const month = `${date.getMonth() + 1}`.padStart(2, '0')
				const day = `${date.getDate()}`.padStart(2, '0')
				return `${year}-${month}-${day}`
			},
			async loadProfile() {
				if (this.loading) {
					return
				}
				this.loading = true
				try {
					const profile = (await memberMineProfile()) || {}
					this.form = {
						nickname: profile.nickname || '',
						realName: profile.realName || '',
						mobile: profile.mobile || '',
						gender: profile.gender || '0',
						birthday: this.formatDate(profile.birthday),
						avatarUrl: profile.avatarUrl || '',
						remark: profile.remark || ''
					}
				} catch (error) {
					uni.showToast({ title: error.message || '加载资料失败', icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			openGenderPicker() {
				uni.showActionSheet({
					itemList: genderOptions.map((item) => item.label),
					success: ({ tapIndex }) => {
						const option = genderOptions[tapIndex]
						if (option) {
							this.form.gender = option.value
						}
					}
				})
			},
			onBirthdayChange(event) {
				this.form.birthday = event?.detail?.value || ''
			},
			async syncWechatProfile() {
				if (typeof uni.getUserProfile !== 'function') {
					uni.showToast({ title: '当前环境不支持同步微信资料', icon: 'none' })
					return
				}
				try {
					const result = await new Promise((resolve, reject) => {
						uni.getUserProfile({
							desc: '用于同步微信头像与昵称到会员资料',
							success: resolve,
							fail: reject
						})
					})
					const userInfo = result?.userInfo || {}
					this.form.nickname = userInfo.nickName || this.form.nickname
					this.form.avatarUrl = userInfo.avatarUrl || this.form.avatarUrl
				} catch (error) {
					uni.showToast({ title: '已取消微信资料同步', icon: 'none' })
				}
			},
			async submitProfile() {
				try {
					uni.showLoading({ title: '保存中...' })
					await memberUpdateProfile({
						nickname: this.form.nickname,
						realName: this.form.realName,
						gender: this.form.gender,
						birthday: this.form.birthday || undefined,
						avatarUrl: this.form.avatarUrl,
						remark: this.form.remark
					})
					uni.showToast({ title: '资料保存成功', icon: 'success' })
				} catch (error) {
					uni.showToast({ title: error.message || '资料保存失败', icon: 'none' })
				} finally {
					uni.hideLoading()
				}
			}
		}
	}
</script>

<style lang="scss">
	.page-wrap {
		padding: 24rpx;
	}

	.profile-card,
	.form-card {
		background: #fff;
		border-radius: 28rpx;
		box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.05);
	}

	.profile-card {
		padding: 28rpx;
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.avatar {
		width: 116rpx;
		height: 116rpx;
		border-radius: 30rpx;
		background: #f3f4f6;
	}

	.profile-main {
		flex: 1;
	}

	.profile-title {
		font-size: 34rpx;
		font-weight: 700;
		color: #111827;
	}

	.profile-desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.action-row {
		margin: 24rpx 0;
		display: flex;
		justify-content: flex-end;
	}

	.ghost-btn {
		margin: 0;
		padding: 0 28rpx;
		height: 74rpx;
		line-height: 74rpx;
		border-radius: 999rpx;
		border: 2rpx solid rgba(255, 140, 66, 0.38);
		background: rgba(255, 140, 66, 0.08);
		color: #ff5a36;
		font-size: 24rpx;
		font-weight: 600;
	}

	.ghost-btn::after,
	.submit-btn::after {
		border: none;
	}

	.form-card {
		padding: 0 28rpx;
	}

	.form-item {
		min-height: 104rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 24rpx;
		border-bottom: 1rpx solid #f3f4f6;
	}

	.form-item.no-border,
	.form-card .form-item:last-child {
		border-bottom: none;
	}

	.label {
		width: 132rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #111827;
	}

	.input,
	.value {
		flex: 1;
		font-size: 28rpx;
		text-align: right;
		color: #374151;
	}

	.input {
		height: 88rpx;
		line-height: 88rpx;
	}

	.readonly .value,
	.picker-item .value {
		color: #6b7280;
	}

	.remark-card {
		margin-top: 24rpx;
		padding: 28rpx;
	}

	.remark-title {
		font-size: 28rpx;
		font-weight: 700;
		color: #111827;
	}

	.remark-input {
		width: 100%;
		height: 220rpx;
		margin-top: 20rpx;
		font-size: 28rpx;
		line-height: 1.7;
		color: #374151;
	}

	.submit-btn {
		margin-top: 32rpx;
		border: none;
		border-radius: 999rpx;
		height: 92rpx;
		line-height: 92rpx;
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
		color: #fff;
		font-size: 30rpx;
		font-weight: 600;
	}
</style>
