<template>
  <view class="page-wrap with-nav">

    <view class="profile" @tap="handleMenu({ path: '/pages/mine/profile' })">
      <view class="profile-main"
        ><view class="profile-name">{{ displayName }}</view
        ></view
      >
      <MemberAvatar :source="summary.avatarUrl || ''" />
    </view>
    <view class="stats-grid">
      <view
        class="stats-item"
        v-for="item in stats"
        :key="item.label"
        @tap="handleMenu(item)"
        ><text class="stats-value">{{ item.value }}</text
        ><text class="stats-label">{{ item.label }}</text></view
      >
    </view>
    <view v-if="summaryError" class="empty error" @tap="loadSummary"
      ><text class="empty-title">暂时没能读取你的信息</text>{{ summaryError
      }}<view>轻点重试</view></view
    >
    <view class="section-heading">会员服务</view>
    <view class="menu-list">
      <view
        class="menu-row"
        v-for="item in menuList"
        :key="item.title"
        @tap="handleMenu(item)"
      ><view class="menu-main"
          ><view class="menu-title">{{ item.title }}</view
          ></view
        ><text class="menu-arrow">›</text>
      </view>
    </view>
    <view
      class="account-toggle row"
      @tap="accountToolsVisible = !accountToolsVisible"
      ><view
        ><view class="menu-title">账号设置</view
        ></view
      ><text class="menu-arrow">{{
        accountToolsVisible ? "−" : "+"
      }}</text></view
    >
    <view v-if="accountToolsVisible" class="panel account-panel">
      <label class="row">
        <text class="menu-title">课程兴趣记录</text>
        <switch :checked="interestEnabled" :disabled="!interestKey" color="#41443f" @change="changeInterest" />
      </label>
      <view class="menu-desc">开启后，教练可根据你浏览课程的时长推荐课程。</view>
      <view class="account-divider"></view>
      <view class="menu-title">绑定手机号</view>
      <view class="menu-desc">{{
        summary.mobile
          ? "当前号码 " + summary.mobile
          : "绑定后，方便门店确认你的会员资料。"
      }}</view>
      <!-- #ifdef MP-WEIXIN -->
      <button
        class="btn ghost"
        :disabled="phoneBinding || loading"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
      >
        {{
          phoneBinding
            ? "绑定中…"
            : summary.mobile
              ? "更换手机号"
              : "授权绑定手机号"
        }}
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <button
        class="btn ghost"
        :disabled="phoneBinding || loading"
        @tap="showWechatOnlyTip"
      >
        如何绑定手机号
      </button>
      <!-- #endif -->
      <view class="account-divider"></view>
      <view class="menu-title">在网页上继续</view>
      <view class="menu-desc"
        >生成登录码后，在自己的浏览器输入。两分钟内有效，只能使用一次，请勿转发。</view
      >
      <button class="btn ghost" @tap="createWebCode">生成网页登录码</button>
    </view>

    <view class="policy-links">
      <navigator url="/pages/legal/index?doc=terms">用户服务协议</navigator>
      <navigator url="/pages/legal/index?doc=privacy">隐私政策</navigator>
    </view>
    <T1Nav role="member" current="mine" />
  </view>
</template>
<script>
import T1Nav from "../../components/T1Nav.vue";
import { courseInterestEnabled, interestPreferenceKey } from "../../utils/course-interest";
import MemberAvatar from "../../components/MemberAvatar.vue";
import {
  memberBindPhone,
  memberMineSummary,
  memberWebCode,
} from "../../api/auth";
import { getMemberSession, saveMemberSession } from "../../utils/auth";

export default {
  components: { T1Nav, MemberAvatar },
  data() {
    return {
      loading: false,
      phoneBinding: false,
      accountToolsVisible: false,
      interestKey: "",
      interestEnabled: false,
      summaryError: "",
      summary: {},
      menuList: [
        {
          title: "我的预约",
          path: "/pages/course/index?tab=reservation",
        },
        {
          title: "我的会员卡",
          path: "/pages/course/index?tab=card",
        },
        {
          title: "训练计划",
          path: "/pages/training/index",
        },
        {
          title: "个人资料",
          path: "/pages/mine/profile",
        },
      ],
    };
  },
  computed: {
    displayName() {
      return this.summary.realName || this.summary.nickname || "会员";
    },
    stats() {
      return [
        {
          label: "预约记录",
          value:
            this.loading || this.summaryError
              ? "—"
              : this.summary.totalReservationCount || 0,
          path: "/pages/course/index?tab=reservation",
        },
        {
          label: "可用会员卡",
          value:
            this.loading || this.summaryError
              ? "—"
              : this.summary.activeCardCount || 0,
          path: "/pages/course/index?tab=card",
        },
        {
          label: "已完成签到",
          value:
            this.loading || this.summaryError
              ? "—"
              : this.summary.confirmedCheckinCount || 0,
          path: "/pages/course/index?tab=reservation",
        },
      ];
    },
  },
  onShow() {
    this.interestKey = interestPreferenceKey();
    this.interestEnabled = courseInterestEnabled(this.interestKey);
    this.loadSummary();
  },
  methods: {
    changeInterest(event) {
      if (!this.interestKey || this.interestKey !== interestPreferenceKey()) return;
      const enabled = event.detail.value === true;
      uni.setStorageSync(this.interestKey, enabled);
      this.interestEnabled = enabled;
    },
    async createWebCode() {
      try {
        const data = await memberWebCode();
        uni.setClipboardData({ data: data.code });
        uni.showModal({
          title: "网页登录码已复制",
          content:
            "请在 t1.wedodiary.top/app/ 的会员入口粘贴。2分钟后过期，使用一次后失效。",
          showCancel: false,
        });
      } catch (e) {
        uni.showToast({ title: e.message, icon: "none" });
      }
    },
    async loadSummary() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.summaryError = "";
      try {
        this.summary = (await memberMineSummary()) || {};
        const session = getMemberSession();
        saveMemberSession({
          ...session,
          memberInfo: {
            ...(session.memberInfo || {}),
            mobile: this.summary.mobile,
            realName: this.summary.realName,
            nickname: this.summary.nickname,
            avatarUrl: this.summary.avatarUrl,
          },
        });
      } catch (error) {
        this.summaryError = error.message || "加载我的信息失败";
        uni.showToast({ title: this.summaryError, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    handleMenu(item) {
      if (item.path) {
        uni.navigateTo({ url: item.path });
        return;
      }
      uni.showToast({ title: "暂时无法打开，请稍后再试", icon: "none" });
    },
    showWechatOnlyTip() {
      uni.showToast({ title: "请在微信小程序内完成手机号绑定", icon: "none" });
    },
    async onGetPhoneNumber(event) {
      if (this.phoneBinding) {
        return;
      }
      const detail = (event && event.detail) || {};
      if (detail.errMsg !== "getPhoneNumber:ok" || !detail.code) {
        const message = String(detail.errMsg || "");
        if (/privacy agreement|errno:112/i.test(message)) {
          uni.showModal({
            title: "手机号授权失败",
            content: "暂时无法使用手机号授权，请联系门店协助绑定。",
            showCancel: false,
          });
        } else if (/no permission/i.test(message)) {
          uni.showModal({
            title: "暂时无法获取手机号",
            content: "暂时无法获取手机号，请在微信中重试，或联系门店协助。",
            showCancel: false,
          });
        } else {
          uni.showModal({
            title: "手机号授权失败",
            content: "未完成手机号授权，你可以稍后再试。",
            showCancel: false,
          });
        }
        return;
      }
      this.phoneBinding = true;
      try {
        uni.showLoading({ title: "绑定中..." });
        await memberBindPhone({ phoneCode: detail.code });
        await this.loadSummary();
        uni.showToast({ title: "手机号绑定成功", icon: "success" });
      } catch (error) {
        uni.showToast({
          title: error.message || "手机号绑定失败",
          icon: "none",
        });
      } finally {
        this.phoneBinding = false;
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
  background: var(--t1-bg);
  color: var(--t1-text);
  padding: 32rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  max-width: 960px;
  margin: auto;
}
.with-nav {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}
.page-title {
  font-size: 48rpx;
  line-height: 1.25;
  font-weight: 700;
  margin-top: 12rpx;
  letter-spacing: -1rpx;
}
.page-subtitle,
.muted {
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--t1-muted);
}
.panel,
.card {
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
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
  background: var(--t1-tint);
  color: var(--t1-accent);
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
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-size: 28rpx;
  font-weight: 700;
}
.btn::after {
  border: 0;
}
.btn.ghost {
  background: transparent;
  border: 1rpx solid var(--t1-border);
  color: var(--t1-text);
}
.btn[disabled] {
  background: var(--t1-inset);
  color: var(--t1-muted);
  opacity: 0.7;
}
.empty {
  padding: 64rpx 28rpx;
  text-align: center;
  border: 1rpx dashed var(--t1-border);
  border-radius: 20rpx;
  margin-top: 24rpx;
  color: var(--t1-muted);
  font-size: 26rpx;
  line-height: 1.8;
}
.empty-title {
  display: block;
  color: var(--t1-text);
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.error {
  color: var(--t1-danger);
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
  border: 1rpx solid var(--t1-border);
  background: var(--t1-surface);
  color: var(--t1-muted);
  font-size: 26rpx;
  box-sizing: border-box;
}
.chip.active {
  color: var(--t1-on-dark);
  background: var(--t1-text);
  border-color: var(--t1-accent);
  font-weight: 700;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 80rpx;
  color: var(--t1-muted);
  font-size: 26rpx;
  margin: -12rpx 0 12rpx;
}

.profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 32rpx 0 36rpx;
}
.profile-main {
  min-width: 0;
  flex: 1;
}
.profile-name {
  font-size: 44rpx;
  line-height: 1.35;
  font-weight: 700;
  word-break: break-all;
}
.stats-grid {
  display: flex;
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
  border-radius: 20rpx;
  padding: 30rpx 0;
}
.stats-item {
  flex: 1;
  text-align: center;
  border-right: 1rpx solid var(--t1-border);
  min-width: 0;
}
.stats-item:last-child {
  border: 0;
}
.stats-value {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: var(--t1-text);
}
.stats-label {
  display: block;
  font-size: 22rpx;
  margin-top: 12rpx;
  color: var(--t1-muted);
}
.section-heading {
  font-size: 25rpx;
  font-weight: 600;
  color: var(--t1-muted);
  margin: 40rpx 0 20rpx;
}
.menu-list {
  border: 1rpx solid var(--t1-border);
  border-radius: 20rpx;
  background: var(--t1-surface);
  padding: 0 26rpx;
}
.menu-row {
  display: flex;
  gap: 22rpx;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--t1-border);
}
.menu-row:last-child {
  border: 0;
}
.menu-main {
  flex: 1;
  min-width: 0;
}
.menu-title {
  font-size: 29rpx;
  font-weight: 600;
}
.menu-desc {
  font-size: 23rpx;
  line-height: 1.7;
  margin-top: 10rpx;
  color: var(--t1-muted);
}
.menu-arrow {
  font-size: 38rpx;
  color: var(--t1-muted);
  flex-shrink: 0;
}
.account-toggle {
  padding: 28rpx 4rpx;
  margin-top: 20rpx;
}
.account-toggle .menu-title {
  font-size: 27rpx;
}
.account-panel {
  margin-top: 0;
}
.account-panel .btn {
  margin-top: 24rpx;
}
.account-divider {
  height: 1rpx;
  background: var(--t1-inset);
  margin: 32rpx 0;
}
</style>
