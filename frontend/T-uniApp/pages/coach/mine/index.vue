<template>
  <view class="mine-page">
    <view class="page-head"
      ><text class="eyebrow">个人中心</text
      ><view class="page-title">教练账户</view></view
    >
    <view class="profile-card"
      ><image
        class="brand-avatar"
        src="/static/images/brand-icon.jpg"
        mode="aspectFill"
      /><view class="profile-main"
        ><text class="profile-name">{{ loaded ? displayName : "T1 教练" }}</text
        ><text class="profile-desc">{{
          loaded ? profileDesc : "正在加载教练资料"
        }}</text></view
      ></view
    >
    <view class="account-row"
      ><text>当前登录方式</text
      ><text class="login-chip">{{ loginMethod }}</text></view
    >
    <view v-if="loading" class="state-card"
      ><text class="state-title">正在加载账户资料</text
      ><text class="muted">请稍候</text></view
    >
    <view v-else-if="errorText" class="state-card"
      ><text class="state-title">账户资料加载失败</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="loadData">重新加载</button></view
    >
    <block v-else-if="loaded">
      <view class="form-card"
        ><button
          class="section-toggle"
          @tap="openSection = openSection === 'profile' ? '' : 'profile'"
        >
          <view
            ><text class="section-title">个人资料</text
            ><text class="section-desc">姓名、联系方式与擅长方向</text></view
          ><text class="toggle-arrow">{{
            openSection === "profile" ? "−" : "+"
          }}</text>
        </button>
        <view v-if="openSection === 'profile'" class="form-content">
          <view class="form-item"
            ><text class="label">昵称</text
            ><input
              v-model.trim="form.nickName"
              class="input"
              maxlength="30"
              placeholder="请输入昵称"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item"
            ><text class="label">姓名</text
            ><input
              v-model.trim="form.coachName"
              class="input"
              maxlength="50"
              placeholder="请输入姓名"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item"
            ><text class="label">手机号</text
            ><input
              v-model.trim="form.mobile"
              class="input"
              type="number"
              maxlength="11"
              placeholder="请输入手机号"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item"
            ><text class="label">擅长方向</text
            ><input
              v-model.trim="form.skillTags"
              class="input"
              maxlength="200"
              placeholder="例如：增肌、体能训练"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item textarea-row"
            ><text class="label">个人简介</text
            ><textarea
              v-model.trim="form.intro"
              class="textarea"
              maxlength="200"
              placeholder="介绍你的教学经验与训练理念"
              placeholder-style="color:#74786f"
            /><text class="word-count">{{ form.intro.length }}/200</text></view
          >
          <button
            class="primary-btn"
            :disabled="savingProfile || loading"
            @tap="saveProfile"
          >
            {{ savingProfile ? "保存中…" : "保存资料" }}
          </button>
        </view>
      </view>
      <view class="form-card"
        ><button
          class="section-toggle"
          @tap="openSection = openSection === 'password' ? '' : 'password'"
        >
          <view
            ><text class="section-title">登录密码</text
            ><text class="section-desc">管理浏览器登录密码</text></view
          ><text class="toggle-arrow">{{
            openSection === "password" ? "−" : "+"
          }}</text>
        </button>
        <view v-if="openSection === 'password'" class="form-content">
          <view class="form-item"
            ><text class="label">当前密码</text
            ><input
              v-model="passwordForm.oldPassword"
              class="input"
              password
              placeholder="请输入当前密码"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item"
            ><text class="label">新密码</text
            ><input
              v-model="passwordForm.newPassword"
              class="input"
              password
              placeholder="请输入至少 6 位新密码"
              placeholder-style="color:#74786f"
          /></view>
          <view class="form-item"
            ><text class="label">确认密码</text
            ><input
              v-model="passwordForm.confirmPassword"
              class="input"
              password
              placeholder="再次输入新密码"
              placeholder-style="color:#74786f"
          /></view>
          <button
            class="primary-btn"
            :disabled="savingPassword || loading"
            @tap="savePassword"
          >
            {{ savingPassword ? "提交中…" : "修改密码" }}
          </button>
        </view>
      </view>
      <view class="message-card"
        ><view class="message-heading"
          ><text class="section-title">工作提醒</text
          ><text class="small-label">最近 10 条</text></view
        ><view v-if="!messageList.length" class="message-empty"
          >暂无新的工作提醒</view
        ><view
          v-for="(item, index) in messageList"
          :key="index"
          class="message-item"
          ><text class="message-title">{{ item.title }}</text
          ><text class="message-desc">{{ item.content }}</text
          ><text class="message-time">{{
            formatDateTime(item.eventTime)
          }}</text></view
        ></view
      >
    </block>
    <button class="logout-btn" :disabled="loggingOut" @tap="logout">
      {{ loggingOut ? "退出中…" : "退出登录" }}
    </button>
    <T1Nav role="coach" current="mine" />
  </view>
</template>
<script>
import {
  coachMessageList,
  coachProfile,
  coachUpdatePassword,
  coachUpdateProfile,
  coachLogout,
} from "../../../api/auth";
import {
  clearCoachSession,
  getCoachSession,
  saveCoachSession,
} from "../../../utils/auth";

import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return {
      profile: {},
      session: {},
      loaded: false,
      openSection: "profile",
      form: {
        nickName: "",
        coachName: "",
        mobile: "",
        avatarUrl: "",
        skillTags: "",
        intro: "",
      },
      passwordForm: {
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
      },
      messageList: [],
      loading: false,
      errorText: "",
      savingProfile: false,
      savingPassword: false,
      loggingOut: false,
    };
  },
  computed: {
    displayName() {
      return this.profile.coachName || this.profile.nickName || "T1 教练";
    },
    profileDesc() {
      return [this.profile.storeName || "门店待分配", this.profile.skillTags]
        .filter(Boolean)
        .join(" · ");
    },
    loginMethod() {
      return this.session.openid ? "微信登录" : "账号登录";
    },
  },
  onShow() {
    this.session = getCoachSession() || {};
    this.loadData();
  },
  methods: {
    async loadData() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        const [profile, messageTable] = await Promise.all([
          coachProfile(),
          coachMessageList({ pageNum: 1, pageSize: 10 }),
        ]);
        this.profile = profile || {};
        this.loaded = true;
        this.form = {
          nickName: (profile && profile.nickName) || "",
          coachName: (profile && profile.coachName) || "",
          mobile: (profile && profile.mobile) || "",
          avatarUrl: (profile && profile.avatarUrl) || "",
          skillTags: (profile && profile.skillTags) || "",
          intro: (profile && profile.intro) || "",
        };
        this.messageList = (messageTable && messageTable.rows) || [];
        const session = getCoachSession();
        saveCoachSession({
          ...session,
          profile: {
            ...(session.profile || {}),
            ...(profile || {}),
          },
        });
      } catch (error) {
        this.errorText = error.message || "教练资料加载失败";
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    validateProfileForm() {
      if (!this.form.coachName) {
        uni.showToast({ title: "请输入姓名", icon: "none" });
        return false;
      }
      if (this.form.mobile && !/^1\d{10}$/.test(this.form.mobile)) {
        uni.showToast({ title: "手机号格式不正确", icon: "none" });
        return false;
      }
      return true;
    },
    async saveProfile() {
      if (this.savingProfile || this.loading) {
        return;
      }
      if (!this.validateProfileForm()) {
        return;
      }
      this.savingProfile = true;
      try {
        uni.showLoading({ title: "保存中..." });
        await coachUpdateProfile({ ...this.form });
        uni.showToast({ title: "资料已保存", icon: "success" });
        await this.loadData();
      } catch (error) {
        uni.showToast({ title: error.message || "保存失败", icon: "none" });
      } finally {
        this.savingProfile = false;
        uni.hideLoading();
      }
    },
    async savePassword() {
      if (this.savingPassword || this.loading) {
        return;
      }
      if (
        !this.passwordForm.oldPassword ||
        !this.passwordForm.newPassword ||
        !this.passwordForm.confirmPassword
      ) {
        uni.showToast({ title: "请填写完整密码信息", icon: "none" });
        return;
      }
      if (this.passwordForm.newPassword.length < 6) {
        uni.showToast({ title: "新密码至少6位", icon: "none" });
        return;
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        uni.showToast({ title: "两次新密码不一致", icon: "none" });
        return;
      }
      this.savingPassword = true;
      try {
        uni.showLoading({ title: "提交中..." });
        await coachUpdatePassword({
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword,
        });
        this.passwordForm = {
          oldPassword: "",
          newPassword: "",
          confirmPassword: "",
        };
        uni.showToast({ title: "密码修改成功", icon: "success" });
      } catch (error) {
        uni.showToast({ title: error.message || "密码修改失败", icon: "none" });
      } finally {
        this.savingPassword = false;
        uni.hideLoading();
      }
    },
    confirmDialog(content) {
      return new Promise((resolve) => {
        uni.showModal({
          title: "请确认",
          content,
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false),
        });
      });
    },
    async logout() {
      if (this.loggingOut) {
        return;
      }
      const ok = await this.confirmDialog("确认退出当前教练账号吗？");
      if (!ok) {
        return;
      }
      this.loggingOut = true;
      try {
        await coachLogout();
      } catch (error) {
        // 忽略后端失败，保证前端状态可退出
      } finally {
        clearCoachSession();
        this.loggingOut = false;
        uni.reLaunch({ url: "/pages/portal/index?role=coach" });
      }
    },
    goPage(url) {
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      if (current && `/${current.route}` === url) {
        return;
      }
      uni.redirectTo({ url });
    },
    formatDateTime(value) {
      if (!value) return "时间未提供";
      const match = String(value).match(
        /^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})/,
      );
      if (match)
        return `${Number(match[2])}/${Number(match[3])} ${match[4]}:${match[5]}`;
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return "时间未提供";
      const hh = `${date.getHours()}`.padStart(2, "0");
      const mm = `${date.getMinutes()}`.padStart(2, "0");
      return `${date.getMonth() + 1}/${date.getDate()} ${hh}:${mm}`;
    },
  },
};
</script>
<style scoped lang="scss">
.mine-page {
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 32rpx 32rpx calc(160rpx + env(safe-area-inset-bottom));
  background: #111211;
  color: #f6f4ef;
}
button {
  padding: 0;
  margin: 0;
  font: inherit;
  color: inherit;
  background: transparent;
  line-height: 1.5;
  border-radius: 0;
}
button::after {
  border: 0;
}
button[disabled] {
  opacity: 0.5;
}
.page-head {
  padding: 8rpx 0 26rpx;
}
.eyebrow {
  color: #ff7a35;
  font-size: 22rpx;
}
.page-title {
  font-size: 46rpx;
  font-weight: 700;
  margin-top: 12rpx;
}
.profile-card {
  display: flex;
  gap: 24rpx;
  align-items: center;
  padding: 28rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
}
.brand-avatar {
  height: 110rpx;
  width: 110rpx;
  border-radius: 18rpx;
  flex-shrink: 0;
}
.profile-main {
  flex: 1;
  min-width: 0;
}
.profile-name {
  display: block;
  font-size: 36rpx;
  font-weight: 650;
}
.profile-desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: #a2a49c;
  margin-top: 10rpx;
}
.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 100rpx;
  font-size: 25rpx;
  color: #a2a49c;
}
.login-chip {
  font-size: 22rpx;
  padding: 8rpx 14rpx;
  border: 1rpx solid #30322f;
  border-radius: 8rpx;
  color: #c4c6bd;
}
.form-card,
.message-card {
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}
.section-toggle {
  width: 100%;
  padding: 26rpx 28rpx;
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-align: left;
  min-height: 116rpx;
}
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 650;
}
.section-desc {
  display: block;
  font-size: 23rpx;
  color: #a2a49c;
  margin-top: 8rpx;
}
.toggle-arrow {
  color: #ff7a35;
  font-size: 36rpx;
  flex-shrink: 0;
  margin-left: 18rpx;
}
.form-content {
  padding: 0 28rpx 28rpx;
}
.form-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-height: 104rpx;
  border-top: 1rpx solid #30322f;
}
.label {
  width: 130rpx;
  flex-shrink: 0;
  font-size: 25rpx;
  color: #c4c6bd;
}
.input {
  flex: 1;
  min-width: 0;
  height: 96rpx;
  color: #f6f4ef;
  font-size: 26rpx;
}
.textarea-row {
  display: block;
  padding-top: 24rpx;
}
.textarea-row .label {
  display: block;
  width: auto;
}
.textarea {
  width: 100%;
  min-height: 160rpx;
  max-height: 220rpx;
  font-size: 26rpx;
  line-height: 1.7;
  padding-top: 18rpx;
  color: #f6f4ef;
  box-sizing: border-box;
}
.word-count {
  display: block;
  text-align: right;
  color: #a2a49c;
  font-size: 21rpx;
  padding: 8rpx 0;
}
.primary-btn,
.secondary-btn,
.logout-btn {
  min-height: 90rpx;
  padding: 16rpx 24rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  border-radius: 12rpx;
  font-size: 27rpx;
  font-weight: 600;
  margin-top: 24rpx;
}
.primary-btn {
  background: #ff7a35;
  color: #111211;
}
.secondary-btn {
  background: #30322f;
}
.logout-btn {
  width: 100%;
  border: 1rpx solid #30322f;
  color: #a2a49c;
  background: #1c1d1b;
}
.message-card {
  padding: 28rpx;
}
.message-heading {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
}
.small-label {
  color: #a2a49c;
  font-size: 22rpx;
}
.message-empty {
  padding: 32rpx 0 12rpx;
  color: #a2a49c;
  font-size: 24rpx;
  text-align: center;
}
.message-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #30322f;
}
.message-item:last-child {
  border: 0;
  padding-bottom: 0;
}
.message-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
}
.message-desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  margin-top: 10rpx;
  color: #a2a49c;
}
.message-time {
  display: block;
  margin-top: 12rpx;
  color: #a2a49c;
  font-size: 21rpx;
}
.state-card {
  padding: 44rpx 28rpx;
  text-align: center;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.muted {
  display: block;
  font-size: 24rpx;
  color: #a2a49c;
  line-height: 1.7;
  margin-top: 12rpx;
}
</style>
