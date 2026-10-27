<template>
  <view class="page-wrap">
    <view class="back-link" @tap="goBack">‹ 返回我的</view>
    <view class="eyebrow">T1 / PROFILE</view>
    <view class="page-title">关于你</view>
    <view class="page-subtitle">让教练更了解你的训练偏好。</view>
    <view class="profile-card">
      <image
        v-if="form.avatarUrl"
        class="avatar"
        :src="form.avatarUrl"
        mode="aspectFill"
      />
      <view v-else class="avatar avatar-letter">T1</view>
      <view class="profile-main"
        ><view class="profile-name">{{
          form.nickname || form.realName || "你的会员资料"
        }}</view
        ><view class="muted">每一次训练，从了解自己开始。</view></view
      >
    </view>
    <!-- #ifdef MP-WEIXIN -->
    <button class="btn ghost wechat-btn" @tap="syncWechatProfile">
      使用微信头像与昵称
    </button>
    <!-- #endif -->
    <view class="section-heading">基本资料</view>
    <view class="form-card">
      <view class="form-item"
        ><text class="label">昵称</text
        ><input
          v-model="form.nickname"
          class="input"
          placeholder="你希望被怎样称呼"
          placeholder-style="color:#7d8077"
          maxlength="20"
      /></view>
      <view class="form-item"
        ><text class="label">姓名</text
        ><input
          v-model="form.realName"
          class="input"
          placeholder="填写姓名"
          placeholder-style="color:#7d8077"
          maxlength="20"
      /></view>
      <view class="form-item readonly"
        ><text class="label">手机号</text
        ><text class="value">{{ form.mobile || "尚未绑定" }}</text></view
      >
      <view class="form-help" v-if="!form.mobile"
        >可返回「我的」，在「账号与登录」中绑定。</view
      >
      <view class="form-item picker-item" @tap="openGenderPicker"
        ><text class="label">性别</text
        ><view class="picker-value"
          ><text>{{ currentGenderLabel }}</text
          ><text class="arrow">›</text></view
        ></view
      >
      <picker mode="date" :value="form.birthday" @change="onBirthdayChange"
        ><view class="form-item no-border"
          ><text class="label">生日</text
          ><view class="picker-value"
            ><text>{{ form.birthday || "选择生日" }}</text
            ><text class="arrow">›</text></view
          ></view
        ></picker
      >
    </view>
    <view class="section-heading">希望教练了解的事</view>
    <view class="form-card remark-card">
      <textarea
        v-model="form.remark"
        class="remark-input"
        maxlength="200"
        placeholder="例如喜欢的训练方式、训练目标或需要特别注意的事项。"
        placeholder-style="color:#7d8077"
      />
      <view class="character-count">{{ form.remark.length }} / 200</view>
    </view>
    <button
      class="btn submit-btn"
      :disabled="loading || saving"
      @tap="submitProfile"
    >
      {{ saving ? "正在保存…" : loading ? "正在读取资料…" : "保存资料" }}
    </button>
  </view>
</template>
<script>
import { memberMineProfile, memberUpdateProfile } from "../../api/auth";

const genderOptions = [
  { label: "暂不填写", value: "0" },
  { label: "男", value: "1" },
  { label: "女", value: "2" },
];

export default {
  data() {
    return {
      loading: false,
      saving: false,
      form: {
        nickname: "",
        realName: "",
        mobile: "",
        gender: "0",
        birthday: "",
        avatarUrl: "",
        remark: "",
      },
    };
  },
  computed: {
    currentGenderLabel() {
      const matched = genderOptions.find(
        (item) => item.value === this.form.gender,
      );
      return matched ? matched.label : "暂不填写";
    },
  },
  onShow() {
    this.loadProfile();
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => uni.redirectTo({ url: "/pages/mine/index" }),
      });
    },

    formatDate(value) {
      if (!value) {
        return "";
      }
      if (typeof value === "string") {
        return value.slice(0, 10);
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return "";
      }
      const year = date.getFullYear();
      const month = `${date.getMonth() + 1}`.padStart(2, "0");
      const day = `${date.getDate()}`.padStart(2, "0");
      return `${year}-${month}-${day}`;
    },
    async loadProfile() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      try {
        const profile = (await memberMineProfile()) || {};
        this.form = {
          nickname: profile.nickname || "",
          realName: profile.realName || "",
          mobile: profile.mobile || "",
          gender: profile.gender || "0",
          birthday: this.formatDate(profile.birthday),
          avatarUrl: profile.avatarUrl || "",
          remark: profile.remark || "",
        };
      } catch (error) {
        uni.showToast({ title: error.message || "加载资料失败", icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    openGenderPicker() {
      uni.showActionSheet({
        itemList: genderOptions.map((item) => item.label),
        success: ({ tapIndex }) => {
          const option = genderOptions[tapIndex];
          if (option) {
            this.form.gender = option.value;
          }
        },
      });
    },
    onBirthdayChange(event) {
      this.form.birthday = event?.detail?.value || "";
    },
    async syncWechatProfile() {
      if (typeof uni.getUserProfile !== "function") {
        uni.showToast({ title: "请在微信小程序中使用此功能", icon: "none" });
        return;
      }
      try {
        const result = await new Promise((resolve, reject) => {
          uni.getUserProfile({
            desc: "用于填写会员头像与昵称",
            success: resolve,
            fail: reject,
          });
        });
        const userInfo = result?.userInfo || {};
        this.form.nickname = userInfo.nickName || this.form.nickname;
        this.form.avatarUrl = userInfo.avatarUrl || this.form.avatarUrl;
      } catch (error) {
        uni.showToast({ title: "已取消头像与昵称授权", icon: "none" });
      }
    },
    async submitProfile() {
      if (this.loading || this.saving) return;
      this.saving = true;
      try {
        uni.showLoading({ title: "保存中..." });
        await memberUpdateProfile({
          nickname: this.form.nickname,
          realName: this.form.realName,
          gender: this.form.gender,
          birthday: this.form.birthday || undefined,
          avatarUrl: this.form.avatarUrl,
          remark: this.form.remark,
        });
        uni.showToast({ title: "资料保存成功", icon: "success" });
      } catch (error) {
        uni.showToast({ title: error.message || "资料保存失败", icon: "none" });
      } finally {
        this.saving = false;
        uni.hideLoading();
      }
    },
  },
};
</script>
<style scoped>
.page-wrap {
  min-height: 100vh;
  box-sizing: border-box;
  background: #111211;
  color: #f6f4ef;
  padding: 32rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  max-width: 960px;
  margin: auto;
}
.with-nav {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #ff7a35;
  font-weight: 700;
}
.page-title {
  font-size: 52rpx;
  line-height: 1.25;
  font-weight: 700;
  margin-top: 12rpx;
  letter-spacing: -1rpx;
}
.page-subtitle,
.muted {
  font-size: 25rpx;
  line-height: 1.7;
  color: #a2a49c;
}
.page-subtitle {
  margin-top: 14rpx;
}
.panel,
.card {
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-top: 24rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
}
.tag {
  font-size: 21rpx;
  line-height: 1.5;
  padding: 8rpx 14rpx;
  background: #30271f;
  color: #ff7a35;
  border-radius: 8rpx;
  white-space: nowrap;
  flex-shrink: 0;
}
.btn {
  margin: 0;
  min-height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  border-radius: 14rpx;
  background: #ff7a35;
  color: #111211;
  font-size: 28rpx;
  font-weight: 700;
}
.btn::after {
  border: 0;
}
.btn.ghost {
  background: transparent;
  border: 1rpx solid #42453f;
  color: #f6f4ef;
}
.btn[disabled] {
  background: #30322f;
  color: #85887f;
  opacity: 0.7;
}
.empty {
  padding: 64rpx 28rpx;
  text-align: center;
  border: 1rpx dashed #30322f;
  border-radius: 20rpx;
  margin-top: 24rpx;
  color: #a2a49c;
  font-size: 26rpx;
  line-height: 1.8;
}
.empty-title {
  display: block;
  color: #f6f4ef;
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.error {
  color: #ffb18c;
}
.scroll {
  white-space: nowrap;
  width: 100%;
  margin-top: 28rpx;
}
.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
  padding: 0 26rpx;
  margin-right: 12rpx;
  border-radius: 14rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  color: #a2a49c;
  font-size: 26rpx;
  box-sizing: border-box;
}
.chip.active {
  color: #111211;
  background: #ff7a35;
  border-color: #ff7a35;
  font-weight: 700;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 80rpx;
  color: #a2a49c;
  font-size: 26rpx;
  margin: -12rpx 0 12rpx;
}

.profile-card {
  display: flex;
  gap: 24rpx;
  align-items: center;
  margin-top: 36rpx;
  padding: 28rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
}
.avatar {
  width: 108rpx;
  height: 108rpx;
  flex-shrink: 0;
  border-radius: 18rpx;
}
.avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #30271f;
  color: #ff7a35;
  font-size: 38rpx;
  font-weight: 700;
}
.profile-main {
  min-width: 0;
  flex: 1;
}
.profile-name {
  font-size: 32rpx;
  font-weight: 700;
  word-break: break-all;
}
.profile-main .muted {
  font-size: 22rpx;
  margin-top: 12rpx;
}
.wechat-btn {
  margin-top: 20rpx;
  font-size: 26rpx;
  font-weight: 500;
}
.section-heading {
  font-size: 25rpx;
  font-weight: 600;
  color: #a2a49c;
  margin: 36rpx 0 18rpx;
}
.form-card {
  padding: 0 26rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
}
.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  min-height: 110rpx;
  border-bottom: 1rpx solid #30322f;
}
.form-item.no-border {
  border: 0;
}
.label {
  flex-shrink: 0;
  width: 100rpx;
  font-size: 27rpx;
  color: #a2a49c;
}
.input,
.value {
  min-width: 0;
  flex: 1;
  font-size: 27rpx;
  text-align: right;
  color: #f6f4ef;
}
.input {
  height: 92rpx;
  line-height: 92rpx;
}
.readonly .value {
  color: #a2a49c;
}
.form-help {
  font-size: 21rpx;
  line-height: 1.7;
  color: #a2a49c;
  padding: 0 0 20rpx;
  text-align: right;
}
.picker-value {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-size: 27rpx;
  color: #f6f4ef;
}
.arrow {
  font-size: 35rpx;
  color: #85887f;
}
.remark-card {
  padding: 24rpx 26rpx;
}
.remark-input {
  width: 100%;
  min-height: 220rpx;
  font-size: 27rpx;
  line-height: 1.8;
  color: #f6f4ef;
}
.character-count {
  text-align: right;
  font-size: 21rpx;
  color: #85887f;
  margin-top: 12rpx;
}
.submit-btn {
  margin-top: 32rpx;
}
</style>
