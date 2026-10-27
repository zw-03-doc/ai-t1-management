<template>
  <view class="portal-page">
    <view class="portal-hero">
      <image
        class="portal-bg"
        src="/static/images/portal.jpg"
        mode="aspectFill"
      />
      <view class="portal-shade" />
      <view
        class="brand-bar"
        :style="{
          paddingTop: statusBarHeight + 'px',
          height: navBottom + 'px',
        }"
      >
        <image
          class="brand-logo"
          src="/static/images/brand-icon.jpg"
          mode="aspectFill"
        />
        <view
          ><view class="brand-name">T1 FITNESS</view
          ><text class="brand-caption">健身空间</text></view
        >
      </view>
      <view class="hero-copy" :style="{ paddingTop: navBottom + 68 + 'px' }">
        <view class="hero-eyebrow"
          ><view class="orange-line" />YOUR NEXT REP</view
        >
        <view class="hero-title"
          ><view>为下一次<text class="accent">进步</text></view
          ><view>而练。</view></view
        >
        <view class="hero-desc">约一节好课，认真完成每一组。</view>
      </view>
    </view>
    <view class="portal-body">
      <view class="space-head"
        ><text class="section-kicker">在 T1，找到你的训练节奏</text
        ><text class="space-link" @tap="previewSpace">看看空间 ↗</text></view
      >
      <view class="space-photos" @tap="previewSpace">
        <image src="/static/images/live-1.jpg" mode="aspectFill" />
        <image src="/static/images/live-2.jpg" mode="aspectFill" />
        <image src="/static/images/live-3.jpg" mode="aspectFill" />
      </view>
      <button
        class="start-button"
        :loading="loginBusy || coachBusy"
        :disabled="loginBusy || coachBusy"
        @tap="targetRole === 'coach' ? openCoach() : startTraining()"
      >
        <text>{{
          targetRole === "coach"
            ? coachLoggedIn
              ? "返回教练工作台"
              : "教练登录"
            : memberLoggedIn
              ? "继续我的训练"
              : "开始训练"
        }}</text
        ><text class="button-arrow">→</text>
      </button>
      <button class="explore-button" @tap="openGuide">
        先看看训练计划 <text>↗</text>
      </button>
      <view class="portal-footer"
        ><text>T1 · 每一组，都算数</text
        ><button
          v-if="targetRole !== 'coach'"
          class="coach-entry"
          :disabled="coachBusy || loginBusy"
          @tap="openCoach"
        >
          教练工作台 ↗</button
        ><button
          v-else
          class="coach-entry"
          :disabled="coachBusy || loginBusy"
          @tap="
            targetRole = 'member';
            targetRoute = '';
          "
        >
          切换会员入口 ↗
        </button></view
      >
    </view>

    <view v-if="browserLogin" class="modal-mask" @tap="closeBrowserLogin">
      <view class="login-sheet" @tap.stop>
        <view class="sheet-heading"
          ><text>{{ browserRole === "coach" ? "教练登录" : "接着上次练" }}</text
          ><button
            class="close-button"
            :disabled="browserBusy"
            @tap="closeBrowserLogin"
          >
            ×
          </button></view
        >
        <view class="sheet-description">{{
          browserRole === "coach"
            ? "使用门店为你开通的账号。"
            : "在小程序「我的」获取网页登录码，在这里继续记录。"
        }}</view>
        <view v-if="browserRole === 'coach'">
          <text class="field-label">教练账号</text
          ><input
            v-model.trim="browserForm.username"
            class="form-input"
            placeholder="手机号或账号"
            placeholder-class="input-placeholder"
          />
          <text class="field-label">密码</text
          ><input
            v-model="browserForm.password"
            class="form-input"
            password
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
          />
        </view>
        <view v-else
          ><text class="field-label">网页登录码</text
          ><input
            v-model.trim="browserForm.code"
            class="form-input"
            placeholder="粘贴一次性登录码"
            placeholder-class="input-placeholder"
        /></view>
        <button
          class="sheet-submit"
          :disabled="browserBusy"
          :loading="browserBusy"
          @tap="submitBrowserLogin"
        >
          登录
        </button>
      </view>
    </view>
    <view
      v-if="activationPopupVisible"
      class="modal-mask"
      @tap="closeActivationPopup"
    >
      <view class="login-sheet" @tap.stop>
        <view class="sheet-heading"
          ><text>开通教练身份</text
          ><button class="close-button" @tap="closeActivationPopup">
            ×
          </button></view
        >
        <view class="sheet-description"
          >填写门店登记的手机号和激活码，关联你的教练账号。</view
        >
        <text class="field-label">手机号</text
        ><input
          v-model.trim="activationForm.mobile"
          class="form-input"
          type="number"
          maxlength="11"
          placeholder="门店登记的手机号"
          placeholder-class="input-placeholder"
        />
        <text class="field-label">激活码</text
        ><input
          v-model.trim="activationForm.activationCode"
          class="form-input"
          maxlength="32"
          placeholder="由门店管理员提供"
          placeholder-class="input-placeholder"
        />
        <button
          class="sheet-submit"
          :disabled="activationSubmitting"
          :loading="activationSubmitting"
          @tap="submitCoachActivation"
        >
          关联教练账号
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import {
  coachWechatActivate,
  coachWechatLogin,
  memberWechatLogin,
  coachPasswordLogin,
  memberWebLogin,
} from "../../api/auth";
import {
  hasCoachSession,
  hasMemberSession,
  resolveTargetUrl,
  saveCoachSession,
  saveMemberSession,
} from "../../utils/auth";

function createActivationForm() {
  return {
    mobile: "",
    activationCode: "",
    openid: "",
  };
}

export default {
  data() {
    return {
      statusBarHeight: 24,
      navBottom: 74,
      loginBusy: false,
      coachBusy: false,
      targetRole: "member",
      browserLogin: false,
      browserRole: "coach",
      browserForm: { username: "", password: "", code: "" },
      browserBusy: false,
      targetRoute: "",
      memberLoggedIn: false,
      coachLoggedIn: false,
      activationPopupVisible: false,
      activationSubmitting: false,
      activationHint: "",
      activationForm: createActivationForm(),
    };
  },
  onLoad(options) {
    const info = uni.getSystemInfoSync();
    this.statusBarHeight = info.statusBarHeight || 24;
    this.navBottom = this.statusBarHeight + 52;
    // #ifdef MP-WEIXIN
    const menu = uni.getMenuButtonBoundingClientRect();
    this.navBottom = menu.bottom + 12;
    // #endif
    this.targetRole = options && options.role === "coach" ? "coach" : "member";
    this.targetRoute = this.decodeTarget(options && options.target);
    this.refreshLoginState();
  },
  onShow() {
    this.refreshLoginState();
  },
  methods: {
    openGuide() {
      uni.navigateTo({ url: "/pages/guide/index" });
    },
    previewSpace() {
      uni.previewImage({
        urls: [
          "/static/images/live-1.jpg",
          "/static/images/live-2.jpg",
          "/static/images/live-3.jpg",
        ],
      });
    },
    async startTraining() {
      if (this.coachBusy || this.browserBusy) return;
      if (this.memberLoggedIn) {
        this.enterHome();
        return;
      }
      // #ifdef MP-WEIXIN
      if (this.loginBusy) return;
      this.loginBusy = true;
      try {
        await this.handleMemberLogin();
      } finally {
        this.loginBusy = false;
      }
      // #endif
      // #ifndef MP-WEIXIN
      this.goMemberFallback();
      // #endif
    },
    async openCoach() {
      if (this.coachBusy || this.loginBusy || this.browserBusy) return;
      if (this.coachLoggedIn) {
        this.goCoachApp();
        return;
      }
      // #ifdef MP-WEIXIN
      this.coachBusy = true;
      try {
        await this.handleCoachEntry();
      } finally {
        this.coachBusy = false;
      }
      // #endif
      // #ifndef MP-WEIXIN
      this.goCoachFallback();
      // #endif
    },
    closeBrowserLogin() {
      if (!this.browserBusy) this.browserLogin = false;
    },
    async submitBrowserLogin() {
      if (this.browserBusy) return;
      const role = this.browserRole;
      const coach = role === "coach";
      const payload = coach
        ? {
            username: this.browserForm.username,
            password: this.browserForm.password,
          }
        : { code: this.browserForm.code };
      this.browserBusy = true;
      try {
        const result = await (coach
          ? coachPasswordLogin(payload)
          : memberWebLogin(payload));
        (coach ? saveCoachSession : saveMemberSession)(result);
        this.browserForm = { username: "", password: "", code: "" };
        this.browserLogin = false;
        this.navigateAfterLogin(role);
      } catch (e) {
        uni.showToast({ title: e.message || "登录失败", icon: "none" });
      } finally {
        this.browserBusy = false;
      }
    },
    decodeTarget(target) {
      if (!target) {
        return "";
      }
      try {
        const decoded = decodeURIComponent(target);
        return /^\/pages\/[\w/-]+(?:\?.*)?$/.test(decoded) ? decoded : "";
      } catch (error) {
        return "";
      }
    },
    refreshLoginState() {
      this.memberLoggedIn = hasMemberSession();
      this.coachLoggedIn = hasCoachSession();
    },
    resolveRoute(role) {
      if (role === this.targetRole && this.targetRoute) {
        return resolveTargetUrl(role, this.targetRoute);
      }
      return resolveTargetUrl(role);
    },
    navigateAfterLogin(role) {
      uni.reLaunch({
        url: this.resolveRoute(role),
      });
    },
    wxLogin() {
      return new Promise((resolve, reject) => {
        uni.login({
          provider: "weixin",
          success: resolve,
          fail: reject,
        });
      });
    },
    getUserProfileSafe() {
      return new Promise((resolve) => {
        if (typeof uni.getUserProfile !== "function") {
          resolve({});
          return;
        }
        uni.getUserProfile({
          desc: "用于完善会员头像与昵称展示",
          success: (res) => resolve(res.userInfo || {}),
          fail: () => resolve({}),
        });
      });
    },
    openActivationPopup(loginVo) {
      this.activationPopupVisible = true;
      this.activationHint = (loginVo && loginVo.message) || "";
      this.activationForm = {
        ...createActivationForm(),
        openid: (loginVo && loginVo.openid) || "",
      };
    },
    closeActivationPopup() {
      if (this.activationSubmitting) {
        return;
      }
      this.activationPopupVisible = false;
      this.activationHint = "";
      this.activationForm = createActivationForm();
    },
    async doMemberLogin() {
      try {
        uni.showLoading({ title: "登录中..." });
        const loginRes = await this.wxLogin();
        const userInfo = await this.getUserProfileSafe();
        const result = await memberWechatLogin({
          code: loginRes.code,
          nickname: userInfo.nickName,
          avatarUrl: userInfo.avatarUrl,
        });
        saveMemberSession(result);
        this.refreshLoginState();
        uni.showToast({ title: "会员登录成功", icon: "success" });
        this.navigateAfterLogin("member");
      } catch (error) {
        uni.showToast({ title: error.message || "会员登录失败", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },
    enterHome() {
      if (this.memberLoggedIn) {
        this.navigateAfterLogin("member");
      }
    },
    async handleMemberLogin() {
      if (this.memberLoggedIn) {
        this.navigateAfterLogin("member");
        return;
      }
      await this.doMemberLogin();
    },
    goMemberFallback() {
      this.browserRole = "member";
      this.browserLogin = true;
    },
    goActivity() {
      uni.navigateTo({
        url: "/pages/activity/index",
      });
    },
    goCoachApp() {
      this.navigateAfterLogin("coach");
    },
    goCoachFallback() {
      this.browserRole = "coach";
      this.browserLogin = true;
    },
    async handleCoachEntry() {
      if (this.coachLoggedIn) {
        this.goCoachApp();
        return;
      }
      try {
        uni.showLoading({ title: "校验中..." });
        const loginRes = await this.wxLogin();
        const result = await coachWechatLogin({ code: loginRes.code });
        if (result && result.needActivation) {
          this.openActivationPopup(result);
          return;
        }
        saveCoachSession(result);
        this.refreshLoginState();
        uni.showToast({ title: "教练登录成功", icon: "success" });
        this.navigateAfterLogin("coach");
      } catch (error) {
        uni.showToast({ title: error.message || "教练登录失败", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },
    async submitCoachActivation() {
      if (this.activationSubmitting) {
        return;
      }
      if (!this.activationForm.mobile) {
        uni.showToast({ title: "请输入手机号", icon: "none" });
        return;
      }
      if (!this.activationForm.activationCode) {
        uni.showToast({ title: "请输入激活码", icon: "none" });
        return;
      }
      try {
        this.activationSubmitting = true;
        uni.showLoading({ title: "激活中..." });
        const loginRes = await this.wxLogin();
        const result = await coachWechatActivate({
          code: loginRes.code,
          mobile: this.activationForm.mobile,
          activationCode: this.activationForm.activationCode,
        });
        saveCoachSession(result);
        this.activationPopupVisible = false;
        this.activationHint = "";
        this.activationForm = createActivationForm();
        this.refreshLoginState();
        uni.showToast({ title: result.message || "激活成功", icon: "success" });
        this.navigateAfterLogin("coach");
      } catch (error) {
        uni.showToast({ title: error.message || "激活失败", icon: "none" });
      } finally {
        this.activationSubmitting = false;
        uni.hideLoading();
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.portal-page {
  display: flex;
  flex-direction: column;
  max-width: 760px;
  margin: 0 auto;
  background: #111211;
  min-height: 100vh;
  color: #f6f4ef;
}
.portal-hero {
  flex: 1;
  position: relative;
  min-height: 790rpx;
  overflow: hidden;
}
.portal-bg,
.portal-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.portal-bg {
  opacity: 0.84;
}
.portal-shade {
  background: linear-gradient(
    180deg,
    rgba(17, 18, 17, 0.25) 0%,
    rgba(17, 18, 17, 0.12) 24%,
    rgba(17, 18, 17, 0.48) 56%,
    #111211 99%
  );
}
.brand-bar {
  position: absolute;
  z-index: 2;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-left: 36rpx;
  padding-right: 210rpx;
}
.brand-logo {
  width: 66rpx;
  height: 66rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
}
.brand-name {
  font-size: 27rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  line-height: 1.2;
}
.brand-caption {
  font-size: 20rpx;
  color: #cacbc5;
  letter-spacing: 4rpx;
}
.hero-copy {
  position: relative;
  padding: 0 40rpx 40rpx;
}
.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 14rpx;
  font-size: 19rpx;
  letter-spacing: 4rpx;
  color: #dedfd9;
  font-weight: 600;
  margin-bottom: 24rpx;
}
.orange-line {
  height: 3rpx;
  width: 36rpx;
  background: #ff7a35;
}
.hero-title {
  font-size: 76rpx;
  font-weight: 800;
  letter-spacing: -3rpx;
  line-height: 1.2;
}
.accent {
  color: #ff7a35;
}
.hero-desc {
  color: #d1d2cc;
  font-size: 27rpx;
  margin-top: 26rpx;
}
.portal-body {
  flex-shrink: 0;
  position: relative;
  margin-top: -95rpx;
  padding: 0 40rpx calc(28rpx + env(safe-area-inset-bottom));
}
.space-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 18rpx;
}
.section-kicker {
  font-size: 23rpx;
  color: #bbbeb5;
}
.space-link {
  font-size: 22rpx;
  color: #e1e2db;
  padding: 12rpx 0;
}
.space-photos {
  display: flex;
  gap: 12rpx;
  margin-bottom: 38rpx;
}
.space-photos image {
  width: calc((100% - 24rpx) / 3);
  height: 144rpx;
  border-radius: 12rpx;
}
.start-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 100rpx;
  padding: 0 30rpx;
  border-radius: 16rpx;
  background: #ff7a35;
  color: #20160f;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 1.4;
}
.button-arrow {
  font-size: 42rpx;
  font-weight: 400;
}
.explore-button {
  width: 100%;
  margin-top: 14rpx;
  min-height: 86rpx;
  border-radius: 16rpx;
  border: 1px solid #363830;
  background: #1b1d19;
  color: #e7e8df;
  font-size: 26rpx;
  line-height: 86rpx;
}
.explore-button text {
  margin-left: 12rpx;
  color: #a2a49c;
}
.portal-footer {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 20rpx;
  color: #7d8177;
}
.coach-entry {
  padding: 14rpx 0 14rpx 20rpx;
  margin: 0;
  background: transparent;
  color: #bfc1b8;
  font-size: 22rpx;
  line-height: 1.5;
}
.modal-mask {
  position: fixed;
  z-index: 80;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.login-sheet {
  width: 100%;
  max-width: 760px;
  padding: 36rpx 36rpx calc(42rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #1c1d1b;
}
.sheet-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 38rpx;
  font-weight: 700;
}
.close-button {
  margin: 0;
  padding: 0;
  width: 64rpx;
  height: 64rpx;
  line-height: 58rpx;
  background: #2b2d28;
  color: #a2a49c;
  border-radius: 50%;
  font-size: 42rpx;
  font-weight: 400;
}
.sheet-description {
  margin: 16rpx 0 28rpx;
  color: #a2a49c;
  font-size: 25rpx;
  line-height: 1.7;
}
.field-label {
  display: block;
  color: #c5c7be;
  font-size: 24rpx;
  margin: 24rpx 0 12rpx;
}
.form-input {
  height: 94rpx;
  padding: 0 24rpx;
  border: 1px solid #383b32;
  border-radius: 14rpx;
  color: #f6f4ef;
  background: #131411;
  font-size: 29rpx;
}
.input-placeholder {
  color: #72776b;
}
.sheet-submit {
  margin-top: 32rpx;
  min-height: 94rpx;
  line-height: 94rpx;
  background: #ff7a35;
  color: #20160f;
  font-size: 29rpx;
  font-weight: 700;
  border-radius: 14rpx;
}
@media (min-width: 600px) {
  .portal-hero {
    min-height: 720px;
  }
  .portal-body {
    margin-top: -140px;
  }
  .hero-copy {
    padding-top: 250px !important;
  }
  .hero-title {
    font-size: 72px;
  }
  .space-photos image {
    height: 150px;
  }
}
</style>
