<template>
  <view class="portal-page">
    <view class="portal-hero">
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
        <text class="brand-name">T1 FITNESS</text>
      </view>
      <image class="portal-bg" src="/static/images/portal.jpg" mode="aspectFill" />
      <view class="hero-actions">
        <button
          class="hero-action booking-action"
          hover-class="hero-action-pressed"
          :disabled="loginBusy || coachBusy || browserBusy"
          @tap="openMemberAction('booking')"
        >
          <view class="action-calendar" aria-hidden="true" />
          <text>约课</text>
        </button>
        <button
          class="hero-action checkin-action"
          hover-class="hero-action-pressed"
          :disabled="loginBusy || coachBusy || browserBusy"
          @tap="openMemberAction('checkin')"
        >
          <view class="action-check" aria-hidden="true" />
          <text>签到</text>
        </button>
      </view>
    </view>
    <view class="portal-body">
      <view class="space-head"><button class="space-link" @tap="previewSpace(0)">查看场馆</button></view
      >
      <view v-if="venuePhotos.length" class="space-photos">
        <button v-for="(photo, index) in venuePhotos.slice(0, 3)" :key="photo.id" class="space-photo"
          :aria-label="'查看第' + (index + 1) + '张场馆照片'" @tap="previewSpace(index)">
          <image :src="photo.src" mode="aspectFill" />
        </button>
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
              ? "会员首页"
              : "会员登录"
        }}</text
        >
      </button>
      <button class="explore-button" @tap="openGuide">
        训练计划
      </button>
      <view class="portal-footer"><button
          v-if="targetRole !== 'coach'"
          class="coach-entry"
          :disabled="coachBusy || loginBusy"
          @tap="openCoach"
        >
          教练工作台</button
        ><button
          v-else
          class="coach-entry"
          :disabled="coachBusy || loginBusy"
          @tap="
            targetRole = 'member';
            targetRoute = '';
          "
        >
          会员入口
        </button></view
      >
    </view>

    <LoginConsent v-if="consentRole" @agree="agreeToLogin" @decline="declineLogin" />
    <view v-if="browserLogin" class="modal-mask" @tap="closeBrowserLogin">
      <view class="login-sheet" @tap.stop>
        <view class="sheet-heading"
          ><text>{{ browserRole === "coach" ? "教练登录" : "会员登录" }}</text
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
            ? "使用绑定的手机号和网页登录密码。密码可在小程序教练端「我的」中设置。"
            : "在小程序「我的」获取一次性网页登录码。"
        }}</view>
        <view v-if="browserRole === 'coach'">
          <text class="field-label">教练账号</text
          ><input
            v-model.trim="browserForm.username"
            class="form-input"
            placeholder="绑定的手机号或原登录账号"
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
          ><text>绑定教练账号</text
          ><button class="close-button" :disabled="activationSubmitting" @tap="closeActivationPopup">
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
        <text class="field-label">网页登录密码</text>
        <input v-model="activationForm.webPassword" class="form-input" password maxlength="20"
          placeholder="设置 6–20 位密码" placeholder-class="input-placeholder" />
        <button
          class="sheet-submit"
          :disabled="activationSubmitting"
          :loading="activationSubmitting"
          @tap="submitCoachActivation"
        >
          绑定并设置密码
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import LoginConsent from "../../components/LoginConsent.vue";
import { readVenuePhotos, refreshVenuePhotos } from "../../utils/venue-photos";
import {
  coachWechatActivate,
  coachWechatLogin,
  memberWechatLogin,
  coachPasswordLogin,
  memberWebLogin,
} from "../../api/auth";
import {
  getMemberSession,
  POLICY_VERSION,
  memberProfileTarget,
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
    webPassword: "",
  };
}

export default {
  components: { LoginConsent },
  data() {
    return {
      venuePhotos: readVenuePhotos(),

      statusBarHeight: 24,
      navBottom: 74,
      loginBusy: false,
      coachBusy: false,
      targetRole: "member",
      browserLogin: false,
      consentRole: "",
      agreedLoginRole: "",
      browserRole: "coach",
      browserForm: { username: "", password: "", code: "" },
      browserBusy: false,
      targetRoute: "",
      memberShortcutRoute: "",
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
    if (this.targetRoute && !(this.targetRole === "coach" ? this.coachLoggedIn : this.memberLoggedIn)) {
      this.consentRole = this.targetRole;
    }
  },
  onShow() {
    this.refreshLoginState();
    refreshVenuePhotos().then(photos => { this.venuePhotos = photos; });

  },
  onReady() {
    const loggedIn = this.targetRole === "coach" ? this.coachLoggedIn : this.memberLoggedIn;
    if (this.targetRoute && loggedIn) this.navigateAfterLogin(this.targetRole);
  },
  methods: {
    openGuide() {
      uni.navigateTo({ url: "/pages/guide/index" });
    },
    previewSpace(index) {
      const id = this.venuePhotos[index]?.id || "";
      uni.navigateTo({ url: "/pages/portal/gallery?index=" + index + "&id=" + encodeURIComponent(id) });
    },
    openMemberAction(action) {
      const routes = {
        booking: "/pages/course/index?tab=schedule",
        checkin: "/pages/course/index?tab=reservation",
      };
      if (routes[action]) return this.startTraining(routes[action]);
    },
    async startTraining(shortcutRoute = "") {
      if (this.loginBusy || this.coachBusy || this.browserBusy || this.browserLogin) return;
      this.memberShortcutRoute = shortcutRoute;
      if (this.memberLoggedIn) {
        this.enterHome();
        return;
      }
      this.consentRole = "member";
    },
    async openCoach() {
      if (this.coachBusy || this.loginBusy || this.browserBusy) return;
      if (this.coachLoggedIn) {
        this.goCoachApp();
        return;
      }
      this.consentRole = "coach";
    },
    declineLogin() {
      this.consentRole = "";
      this.agreedLoginRole = "";
      this.memberShortcutRoute = "";
    },
    async agreeToLogin() {
      const role = this.consentRole;
      if (!role) return;
      this.consentRole = "";
      this.agreedLoginRole = role;
      if (role === "member") {
        // #ifdef MP-WEIXIN
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
      if (!this.browserBusy) {
        this.browserLogin = false;
        this.agreedLoginRole = "";
        this.browserForm = { username: "", password: "", code: "" };
        this.memberShortcutRoute = "";
      }
    },
    async submitBrowserLogin() {
      if (this.browserBusy || !this.browserLogin || this.agreedLoginRole !== this.browserRole) return;
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
          ? coachPasswordLogin({ ...payload, consentVersion: POLICY_VERSION })
          : memberWebLogin({ ...payload, consentVersion: POLICY_VERSION }));
        (coach ? saveCoachSession : saveMemberSession)({ ...result, policyVersion: POLICY_VERSION });
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
        const decoded = target.startsWith("/") ? target : decodeURIComponent(target);
        if (/[#\r\n]/.test(decoded)) return "";
        const [path, query = ""] = decoded.split("?");
        const coachTraining = path === "/pages/training/index" && /(?:^|&)role=coach(?:&|$)/.test(decodeURIComponent(query));
        if (this.targetRole === "coach") {
          return coachTraining || path === "/pages/coach/schedule/create" || /^\/pages\/coach\/(?:home|schedule|checkin|student|mine|interest)\/index$/.test(path) ? decoded : "";
        }
        if (coachTraining || path === "/pages/portal/index") return "";
        return path === "/pages/mine/profile" || memberProfileTarget(decoded) === decoded ? decoded : "";
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
      this.agreedLoginRole = "";
      const shortcut = role === "member" ? this.memberShortcutRoute : "";
      const target = shortcut || this.resolveRoute(role);
      const needsProfile = role === "member" &&
        getMemberSession().memberInfo?.profileSetupStatus === "pending";
      const url = needsProfile
        ? `/pages/mine/profile?onboarding=1&target=${encodeURIComponent(target)}`
        : target;
      this.memberShortcutRoute = "";
      if (shortcut && !needsProfile) uni.navigateTo({ url });
      else uni.reLaunch({ url });
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
    openActivationPopup(loginVo) {
      this.activationPopupVisible = true;
      this.activationHint = (loginVo && loginVo.message) || "";
      this.activationForm = createActivationForm();
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
      if (this.agreedLoginRole !== "member") return;
      try {
        uni.showLoading({ title: "登录中..." });
        const loginRes = await this.wxLogin();
        const result = await memberWechatLogin({ code: loginRes.code, consentVersion: POLICY_VERSION });
        saveMemberSession({ ...result, policyVersion: POLICY_VERSION });
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
      if (this.agreedLoginRole !== "coach") return;
      if (this.coachLoggedIn) {
        this.goCoachApp();
        return;
      }
      try {
        uni.showLoading({ title: "校验中..." });
        const loginRes = await this.wxLogin();
        const result = await coachWechatLogin({ code: loginRes.code, consentVersion: POLICY_VERSION });
        if (result && result.needActivation) {
          this.openActivationPopup(result);
          return;
        }
        saveCoachSession({ ...result, policyVersion: POLICY_VERSION });
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
      if (this.activationSubmitting || !this.activationPopupVisible || this.agreedLoginRole !== "coach") {
        return;
      }
      if (!/^1\d{10}$/.test(this.activationForm.mobile)) {
        uni.showToast({ title: "请输入门店登记的 11 位手机号", icon: "none" });
        return;
      }
      if (!this.activationForm.activationCode) {
        uni.showToast({ title: "请输入激活码", icon: "none" });
        return;
      }
      if (this.activationForm.webPassword.length < 6 || this.activationForm.webPassword.length > 20 || !this.activationForm.webPassword.trim()) {
        uni.showToast({ title: "请设置 6–20 位网页登录密码", icon: "none" });
        return;
      }
      try {
        this.activationSubmitting = true;
        uni.showLoading({ title: "激活中..." });
        const loginRes = await this.wxLogin();
        const result = await coachWechatActivate({
          consentVersion: POLICY_VERSION,
          code: loginRes.code,
          mobile: this.activationForm.mobile,
          activationCode: this.activationForm.activationCode,
          webPassword: this.activationForm.webPassword,
        });
        saveCoachSession({ ...result, policyVersion: POLICY_VERSION });
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
  max-width: 680px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 0 36rpx calc(40rpx + env(safe-area-inset-bottom));
  background: var(--t1-bg);
  color: var(--t1-text);
}
.brand-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-right: 195rpx;
}
.brand-logo { width: 48rpx; height: 48rpx; border-radius: 10rpx; flex-shrink: 0; }
.brand-name { font-size: 29rpx; font-weight: 700; letter-spacing: .04em; }
.portal-bg {
  display: block;
  width: 100%;
  height: 360rpx;
  max-height: 300px;
  margin: 24rpx 0;
  border-radius: 20rpx;
}
.hero-actions { display: flex; gap: 16rpx; }
.hero-action {
  flex: 1;
  min-height: 100rpx;
  margin: 0;
  padding: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  background: var(--t1-text);
  border: 1px solid var(--t1-text);
  color: white;
  font-size: 31rpx;
  font-weight: 600;
  border-radius: 14rpx;
}
.checkin-action { background: var(--t1-surface); color: var(--t1-text); border-color: var(--t1-border); }
.hero-action-pressed { opacity: .78; }
.action-calendar,
.action-check {
  position: relative;
  width: 42rpx;
  height: 42rpx;
  border: 2px solid currentColor;
  border-radius: 8rpx;
}
.action-calendar::before {
  content: "";
  position: absolute;
  top: -6rpx;
  left: 6rpx;
  right: 6rpx;
  height: 10rpx;
  border-left: 2px solid currentColor;
  border-right: 2px solid currentColor;
}
.action-calendar::after {
  content: "";
  position: absolute;
  top: 11rpx;
  left: 0;
  right: 0;
  border-top: 2px solid currentColor;
}
.action-check {
  border-radius: 50%;
}
.action-check::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 43%;
  width: 9rpx;
  height: 18rpx;
  border-right: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: translate(-50%, -50%) rotate(45deg);
}
.portal-body { display: flex; flex-direction: column; }
.space-head { display: flex; align-items: center; margin-top: 44rpx; }
.space-link {
  display: block; margin: 0; padding: 0; background: transparent;
  color: var(--t1-text); font-size: 30rpx; font-weight: 600; line-height: 88rpx;
}
.space-photos { display: flex; gap: 12rpx; margin-bottom: 36rpx; }
.space-photo {
  flex: 1; min-width: 0; margin: 0; padding: 0; border-radius: 12rpx; overflow: hidden;
}
.space-photo image { display: block; width: 100%; height: 138rpx; }
.start-button, .explore-button, .coach-entry {
  position: relative; display: flex; align-items: center; width: 100%; margin: 0;
  min-height: 96rpx; padding: 12rpx 0; border-radius: 0; text-align: left;
  border-top: 1px solid var(--t1-border); background: transparent;
  color: var(--t1-text); font-size: 29rpx; font-weight: 500;
}
.start-button::before, .explore-button::before, .coach-entry::before {
  content: ""; position: absolute; right: 6rpx; width: 12rpx; height: 12rpx;
  border-right: 2rpx solid var(--t1-muted); border-top: 2rpx solid var(--t1-muted); transform: rotate(45deg);
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
  max-height: 92vh;
  overflow-y: auto;
  width: 100%;
  max-width: 760px;
  padding: 36rpx 36rpx calc(42rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: var(--t1-surface);
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
  background: var(--t1-inset);
  color: var(--t1-muted);
  border-radius: 50%;
  font-size: 42rpx;
  font-weight: 400;
}
.sheet-description {
  margin: 16rpx 0 28rpx;
  color: var(--t1-muted);
  font-size: 25rpx;
  line-height: 1.7;
}
.field-label {
  display: block;
  color: var(--t1-muted);
  font-size: 24rpx;
  margin: 24rpx 0 12rpx;
}
.form-input {
  height: 94rpx;
  padding: 0 24rpx;
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  color: var(--t1-text);
  background: var(--t1-inset);
  font-size: 29rpx;
}
.input-placeholder {
  color: var(--t1-muted);
}
.sheet-submit {
  margin-top: 32rpx;
  min-height: 94rpx;
  line-height: 94rpx;
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-size: 29rpx;
  font-weight: 700;
  border-radius: 14rpx;
}
</style>
