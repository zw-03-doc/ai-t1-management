<template>
	<view class="portal-page">
		<image class="portal-bg" src="/static/images/portal.jpg" mode="aspectFill" />
		<view class="portal-mask"></view>
		<view class="portal-content">
			<view class="brand-card">
				<image class="brand-icon" src="/static/images/brand-icon.jpg" mode="aspectFit" />
				<view class="brand-text">
					<text class="brand-title">T1 健身空间</text>
					<text class="brand-subtitle">会员入口一键微信登录，教练入口首次需手机号 + 激活码完成绑定</text>
				</view>
			</view>

			<view class="hero-copy">
				<text class="hero-title">畅练 · 约课 · 核销</text>
				<text class="hero-desc">会员端通过微信登录换取 openid 后自动注册/自动登录；教练端先探测当前微信，未激活时输入手机号和激活码完成首次绑定。</text>
			</view>

			<view class="status-bar">
				<view class="status-chip" :class="memberLoggedIn ? 'active' : ''">会员{{ memberLoggedIn ? '已登录' : '未登录' }}</view>
				<view class="status-chip" :class="coachLoggedIn ? 'active' : ''">教练{{ coachLoggedIn ? '已登录' : '未登录' }}</view>
			</view>

			<view class="live-preview">
				<image class="preview-image left" src="/static/images/live-1.jpg" mode="aspectFill" />
				<image class="preview-image center" src="/static/images/live-2.jpg" mode="aspectFill" />
				<image class="preview-image right" src="/static/images/live-3.jpg" mode="aspectFill" />
			</view>

			<view class="action-group">
				<!-- #ifdef MP-WEIXIN -->
				<button v-if="memberLoggedIn" class="primary-btn" @tap="enterHome">进入会员首页</button>
				<button v-else class="primary-btn" @tap="handleMemberLogin">微信登录会员首页</button>
				<!-- #endif -->
				<!-- #ifndef MP-WEIXIN -->
				<button class="primary-btn" @tap="goMemberFallback">微信登录会员首页</button>
				<!-- #endif -->
				<button class="secondary-btn" @tap="goActivity">查看活动专区</button>
				<button v-if="coachLoggedIn" class="coach-btn" @tap="goCoachApp">进入教练工作台</button>
				<!-- #ifdef MP-WEIXIN -->
				<button v-else class="coach-btn" @tap="handleCoachEntry">微信登录教练入口</button>
				<!-- #endif -->
				<!-- #ifndef MP-WEIXIN -->
				<button v-else class="coach-btn" @tap="goCoachFallback">教练入口</button>
				<!-- #endif -->
				<text class="action-tip">提示：会员登录不再依赖手机号授权；手机号绑定请到“我的”页单独完成。教练首次登录需输入管理端预留的手机号与激活码。</text>
			</view>
		</view>

		<view v-if="activationPopupVisible" class="activation-mask" @tap="closeActivationPopup">
			<view class="activation-card" @tap.stop>
				<view class="activation-header">
					<text class="activation-title">教练身份激活</text>
					<text class="activation-close" @tap="closeActivationPopup">×</text>
				</view>
				<text class="activation-desc">{{ activationHint || '首次登录请输入管理端预留的手机号和激活码，完成与当前微信绑定。' }}</text>
				<view class="activation-form">
					<view class="form-item">
						<text class="form-label">手机号</text>
						<input v-model.trim="activationForm.mobile" class="form-input" type="number" maxlength="11" placeholder="请输入教练手机号" />
					</view>
					<view class="form-item">
						<text class="form-label">激活码</text>
						<input v-model.trim="activationForm.activationCode" class="form-input" maxlength="32" placeholder="请输入教练激活码" />
					</view>
				</view>
				<view class="activation-actions">
					<button class="ghost-btn" :disabled="activationSubmitting" @tap="closeActivationPopup">稍后再说</button>
					<button class="popup-primary-btn" :disabled="activationSubmitting" @tap="submitCoachActivation">立即激活</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { coachWechatActivate, coachWechatLogin, memberWechatLogin } from '../../api/auth'
	import {
		hasCoachSession,
		hasMemberSession,
		resolveTargetUrl,
		saveCoachSession,
		saveMemberSession
	} from '../../utils/auth'

	function createActivationForm() {
		return {
			mobile: '',
			activationCode: '',
			openid: ''
		}
	}

	export default {
		data() {
			return {
				targetRole: 'member',
				targetRoute: '',
				memberLoggedIn: false,
				coachLoggedIn: false,
				activationPopupVisible: false,
				activationSubmitting: false,
				activationHint: '',
				activationForm: createActivationForm()
			}
		},
		onLoad(options) {
			this.targetRole = (options && options.role) || 'member'
			this.targetRoute = this.decodeTarget(options && options.target)
			this.refreshLoginState()
		},
		onShow() {
			this.refreshLoginState()
		},
		methods: {
			decodeTarget(target) {
				if (!target) {
					return ''
				}
				try {
					return decodeURIComponent(target)
				} catch (error) {
					return target
				}
			},
			refreshLoginState() {
				this.memberLoggedIn = hasMemberSession()
				this.coachLoggedIn = hasCoachSession()
			},
			resolveRoute(role) {
				if (role === this.targetRole && this.targetRoute) {
					return resolveTargetUrl(role, this.targetRoute)
				}
				return resolveTargetUrl(role)
			},
			navigateAfterLogin(role) {
				uni.reLaunch({
					url: this.resolveRoute(role)
				})
			},
			wxLogin() {
				return new Promise((resolve, reject) => {
					uni.login({
						provider: 'weixin',
						success: resolve,
						fail: reject
					})
				})
			},
			getUserProfileSafe() {
				return new Promise((resolve) => {
					if (typeof uni.getUserProfile !== 'function') {
						resolve({})
						return
					}
					uni.getUserProfile({
						desc: '用于完善会员头像与昵称展示',
						success: (res) => resolve(res.userInfo || {}),
						fail: () => resolve({})
					})
				})
			},
			openActivationPopup(loginVo) {
				this.activationPopupVisible = true
				this.activationHint = (loginVo && loginVo.message) || ''
				this.activationForm = {
					...createActivationForm(),
					openid: (loginVo && loginVo.openid) || ''
				}
			},
			closeActivationPopup() {
				if (this.activationSubmitting) {
					return
				}
				this.activationPopupVisible = false
				this.activationHint = ''
				this.activationForm = createActivationForm()
			},
			async doMemberLogin() {
				try {
					uni.showLoading({ title: '登录中...' })
					const loginRes = await this.wxLogin()
					const userInfo = await this.getUserProfileSafe()
					const result = await memberWechatLogin({
						code: loginRes.code,
						nickname: userInfo.nickName,
						avatarUrl: userInfo.avatarUrl
					})
					saveMemberSession(result)
					this.refreshLoginState()
					uni.showToast({ title: '会员登录成功', icon: 'success' })
					this.navigateAfterLogin('member')
				} catch (error) {
					uni.showToast({ title: error.message || '会员登录失败', icon: 'none' })
				} finally {
					uni.hideLoading()
				}
			},
			enterHome() {
				if (this.memberLoggedIn) {
					this.navigateAfterLogin('member')
				}
			},
			async handleMemberLogin() {
				if (this.memberLoggedIn) {
					this.navigateAfterLogin('member')
					return
				}
				await this.doMemberLogin()
			},
			goMemberFallback() {
				uni.showToast({ title: '请在微信小程序内完成会员登录', icon: 'none' })
			},
			goActivity() {
				uni.navigateTo({
					url: '/pages/activity/index'
				})
			},
			goCoachApp() {
				this.navigateAfterLogin('coach')
			},
			goCoachFallback() {
				uni.showToast({ title: '请在微信小程序内完成教练激活与登录', icon: 'none' })
			},
			async handleCoachEntry() {
				if (this.coachLoggedIn) {
					this.goCoachApp()
					return
				}
				try {
					uni.showLoading({ title: '校验中...' })
					const loginRes = await this.wxLogin()
					const result = await coachWechatLogin({ code: loginRes.code })
					if (result && result.needActivation) {
						this.openActivationPopup(result)
						return
					}
					saveCoachSession(result)
					this.refreshLoginState()
					uni.showToast({ title: '教练登录成功', icon: 'success' })
					this.navigateAfterLogin('coach')
				} catch (error) {
					uni.showToast({ title: error.message || '教练登录失败', icon: 'none' })
				} finally {
					uni.hideLoading()
				}
			},
			async submitCoachActivation() {
				if (this.activationSubmitting) {
					return
				}
				if (!this.activationForm.mobile) {
					uni.showToast({ title: '请输入手机号', icon: 'none' })
					return
				}
				if (!this.activationForm.activationCode) {
					uni.showToast({ title: '请输入激活码', icon: 'none' })
					return
				}
				try {
					this.activationSubmitting = true
					uni.showLoading({ title: '激活中...' })
					const result = await coachWechatActivate({
						openid: this.activationForm.openid,
						mobile: this.activationForm.mobile,
						activationCode: this.activationForm.activationCode
					})
					saveCoachSession(result)
					this.activationPopupVisible = false
					this.activationHint = ''
					this.activationForm = createActivationForm()
					this.refreshLoginState()
					uni.showToast({ title: result.message || '激活成功', icon: 'success' })
					this.navigateAfterLogin('coach')
				} catch (error) {
					uni.showToast({ title: error.message || '激活失败', icon: 'none' })
				} finally {
					this.activationSubmitting = false
					uni.hideLoading()
				}
			}
		}
	}
</script>

<style lang="scss">
	.portal-page {
		position: relative;
		min-height: 100vh;
		overflow: hidden;
	}

	.portal-bg,
	.portal-mask,
	.activation-mask {
		position: absolute;
		inset: 0;
		width: 100%;
		height: 100%;
	}

	.portal-mask {
		background: linear-gradient(180deg, rgba(10, 15, 28, 0.18) 0%, rgba(10, 15, 28, 0.7) 52%, rgba(10, 15, 28, 0.92) 100%);
	}

	.portal-content {
		position: relative;
		z-index: 2;
		padding: 96rpx 40rpx 56rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		min-height: 100vh;
	}

	.brand-card {
		display: flex;
		align-items: center;
		gap: 24rpx;
		padding: 24rpx;
		background: rgba(255, 255, 255, 0.14);
		backdrop-filter: blur(10px);
		border: 2rpx solid rgba(255, 255, 255, 0.18);
		border-radius: 28rpx;
	}

	.brand-icon {
		width: 108rpx;
		height: 108rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.92);
	}

	.brand-text {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		color: #fff;
	}

	.brand-title {
		font-size: 42rpx;
		font-weight: 700;
	}

	.brand-subtitle {
		font-size: 24rpx;
		opacity: 0.88;
		line-height: 1.6;
	}

	.hero-copy {
		margin-top: 64rpx;
		display: flex;
		flex-direction: column;
		gap: 18rpx;
		color: #fff;
	}

	.hero-title {
		font-size: 68rpx;
		font-weight: 700;
		line-height: 1.2;
	}

	.hero-desc {
		font-size: 28rpx;
		line-height: 1.8;
		opacity: 0.92;
	}

	.status-bar {
		margin-top: 40rpx;
		display: flex;
		gap: 20rpx;
		flex-wrap: wrap;
	}

	.status-chip {
		padding: 14rpx 24rpx;
		border-radius: 999rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.86);
		background: rgba(255, 255, 255, 0.12);
		border: 2rpx solid rgba(255, 255, 255, 0.16);
	}

	.status-chip.active {
		color: #fff;
		background: rgba(255, 140, 66, 0.24);
		border-color: rgba(255, 140, 66, 0.45);
	}

	.live-preview {
		margin-top: 48rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 18rpx;
	}

	.preview-image {
		flex: 1;
		height: 220rpx;
		border-radius: 28rpx;
		box-shadow: 0 18rpx 36rpx rgba(0, 0, 0, 0.18);
	}

	.preview-image.center {
		height: 260rpx;
	}

	.action-group {
		margin-top: auto;
		padding-top: 60rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	button {
		border: none;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		height: 96rpx;
		line-height: 96rpx;
	}

	button::after {
		border: none;
	}

	.primary-btn {
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
		color: #fff;
	}

	.secondary-btn {
		background: rgba(255, 255, 255, 0.14);
		color: #fff;
		border: 2rpx solid rgba(255, 255, 255, 0.18);
	}

	.coach-btn {
		background: rgba(255, 140, 66, 0.16);
		color: #fff;
		border: 2rpx solid rgba(255, 140, 66, 0.4);
	}

	.action-tip {
		font-size: 24rpx;
		line-height: 1.8;
		color: rgba(255, 255, 255, 0.78);
	}

	.activation-mask {
		z-index: 8;
		background: rgba(15, 23, 42, 0.56);
		display: flex;
		align-items: flex-end;
		justify-content: center;
		padding: 32rpx;
	}

	.activation-card {
		width: 100%;
		padding: 32rpx;
		border-radius: 32rpx;
		background: #fff;
		box-shadow: 0 20rpx 48rpx rgba(15, 23, 42, 0.18);
	}

	.activation-header,
	.activation-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
	}

	.activation-title {
		font-size: 34rpx;
		font-weight: 700;
		color: #111827;
	}

	.activation-close {
		font-size: 44rpx;
		line-height: 1;
		color: #9ca3af;
	}

	.activation-desc,
	.form-label {
		font-size: 24rpx;
		line-height: 1.8;
		color: #6b7280;
	}

	.activation-desc {
		margin-top: 18rpx;
	}

	.activation-form {
		margin-top: 24rpx;
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}

	.form-item {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}

	.form-input {
		width: 100%;
		height: 88rpx;
		padding: 0 24rpx;
		border-radius: 24rpx;
		background: #f9fafb;
		border: 2rpx solid #e5e7eb;
		font-size: 28rpx;
		color: #111827;
	}

	.activation-actions {
		margin-top: 30rpx;
	}

	.ghost-btn,
	.popup-primary-btn {
		flex: 1;
		height: 88rpx;
		line-height: 88rpx;
		font-size: 28rpx;
	}

	.ghost-btn {
		background: #f3f4f6;
		color: #374151;
	}

	.popup-primary-btn {
		background: linear-gradient(135deg, #ff8c42 0%, #ff5a36 100%);
		color: #fff;
	}
</style>
