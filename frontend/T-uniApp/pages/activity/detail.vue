<template>
  <view class="page-wrap">
    <view class="back-link" @tap="goBack">‹ 返回活动</view>
    <view v-if="loading" class="empty">正在加载活动…</view>
    <view v-else-if="errorText" class="empty error" @tap="loadDetail"
      ><text class="empty-title">活动暂时没能加载</text>{{ errorText
      }}<view>轻点重试</view></view
    >
    <block v-else-if="detail.activityId">
      <image
        v-if="detail.coverUrl"
        class="cover"
        :src="detail.coverUrl"
        mode="aspectFill"
      />
      <view class="row"
        ><text class="eyebrow">T1 / CLUB EVENT</text
        ><text class="tag" v-if="detail.weeklyFeatured === '1'"
          >本周推荐</text
        ></view
      >
      <view class="page-title">{{ detail.title }}</view>
      <view class="page-subtitle" v-if="detail.subTitle">{{
        detail.subTitle
      }}</view>
      <view class="highlight" v-if="detail.priceHighlight">{{
        detail.priceHighlight
      }}</view>
      <view class="event-meta">
        <view class="event-meta-row"
          ><text class="meta-label">门店</text
          ><text>{{ detail.storeName || "全门店" }}</text></view
        >
        <view class="event-meta-row"
          ><text class="meta-label">时间</text
          ><view class="date-lines"
            ><text>{{ formatDateTime(detail.startTime) }}</text
            ><text class="muted"
              >至 {{ formatDateTime(detail.endTime) }}</text
            ></view
          ></view
        >
        <view class="event-meta-row" v-if="detail.targetAudience"
          ><text class="meta-label">适合</text
          ><text>{{ detail.targetAudience }}</text></view
        >
        <view class="countdown">{{
          formatCountdown(detail.countdownEndTime || detail.endTime)
        }}</view>
      </view>
      <view class="panel" v-if="detail.ruleNote || detail.discountLabel">
        <view class="section-title">参与前了解</view>
        <view class="content" v-if="detail.discountLabel">{{
          detail.discountLabel
        }}</view>
        <view class="content" v-if="detail.ruleNote">{{
          detail.ruleNote
        }}</view>
      </view>
      <view class="panel" v-if="detail.detailContent"
        ><view class="section-title">活动详情</view
        ><view class="content">{{ detail.detailContent }}</view></view
      >
      <image
        v-if="detail.detailImageUrl"
        class="detail-image"
        :src="detail.detailImageUrl"
        mode="widthFix"
      />
      <view class="consult">
        <view class="section-title">感兴趣？到店聊聊。</view>
        <view class="muted"
          >参与方式和会员卡权益，请与门店或负责教练确认。</view
        >
        <button class="btn" @tap="handlePrimaryAction">
          {{ primaryActionText }}
        </button>
        <button
          v-if="detail.actionType !== 'contact' && detail.storeContactPhone"
          class="btn ghost"
          @tap="contactStore"
        >
          致电门店咨询
        </button>
      </view>
    </block>
    <view v-else class="empty"
      ><text class="empty-title">没有找到这个活动</text
      >返回活动页，看看其他选择。</view
    >
  </view>
</template>
<script>
import { memberActivityDetail } from "../../api/auth";

export default {
  data() {
    return {
      activityId: undefined,
      detail: {},
      loading: false,
      errorText: "",
    };
  },
  computed: {
    primaryActionText() {
      return this.detail.actionType === "contact"
        ? "致电门店咨询"
        : "查看会员卡方案";
    },
  },
  onLoad(options) {
    this.activityId = options && options.id;
    this.loadDetail();
  },
  methods: {
    async loadDetail() {
      if (!this.activityId) {
        this.errorText = "活动参数缺失";
        return;
      }
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        this.detail = (await memberActivityDetail(this.activityId)) || {};
      } catch (error) {
        this.errorText = error.message || "活动详情加载失败";
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    handlePrimaryAction() {
      if (this.detail.actionType === "contact") {
        this.contactStore();
        return;
      }
      this.goPlanList();
    },
    goPlanList() {
      uni.navigateTo({ url: `/pages/activity/plan?id=${this.activityId}` });
    },
    contactStore() {
      const phone = this.detail.storeContactPhone;
      if (!phone) {
        uni.showToast({ title: "当前门店暂无联系电话", icon: "none" });
        return;
      }
      uni.makePhoneCall({ phoneNumber: phone });
    },
    goBack() {
      uni.navigateBack({
        delta: 1,
        fail: () => uni.redirectTo({ url: "/pages/activity/index" }),
      });
    },
    formatDateTime(value) {
      if (!value) {
        return "--";
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return "--";
      }
      const yyyy = date.getFullYear();
      const mm = `${date.getMonth() + 1}`.padStart(2, "0");
      const dd = `${date.getDate()}`.padStart(2, "0");
      const hh = `${date.getHours()}`.padStart(2, "0");
      const mi = `${date.getMinutes()}`.padStart(2, "0");
      return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
    },
    formatCountdown(value) {
      if (!value) {
        return "具体时间请咨询门店";
      }
      const end = new Date(value);
      if (Number.isNaN(end.getTime())) {
        return "具体时间请咨询门店";
      }
      const diff = end.getTime() - Date.now();
      if (diff <= 0) {
        return "活动已结束";
      }
      const day = Math.floor(diff / (24 * 60 * 60 * 1000));
      const hour = Math.floor(
        (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000),
      );
      if (day > 0) {
        return `倒计时 ${day}天${hour}小时`;
      }
      return `倒计时 ${hour}小时`;
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

.cover {
  display: block;
  width: 100%;
  height: 400rpx;
  border-radius: 20rpx;
  margin-bottom: 32rpx;
}
.highlight {
  font-size: 35rpx;
  font-weight: 600;
  color: #ff7a35;
  line-height: 1.6;
  margin-top: 28rpx;
}
.event-meta {
  padding: 16rpx 0 24rpx;
  margin-top: 20rpx;
  border-top: 1rpx solid #30322f;
  border-bottom: 1rpx solid #30322f;
}
.event-meta-row {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  padding: 16rpx 0;
  font-size: 27rpx;
  line-height: 1.6;
}
.meta-label {
  flex-shrink: 0;
  width: 64rpx;
  color: #a2a49c;
}
.date-lines {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.countdown {
  display: inline-block;
  background: #30271f;
  color: #ff7a35;
  font-size: 23rpx;
  padding: 12rpx 20rpx;
  border-radius: 8rpx;
  margin-top: 16rpx;
}
.content {
  margin-top: 24rpx;
  white-space: pre-wrap;
  font-size: 27rpx;
  line-height: 1.9;
  color: #d6d7d0;
}
.detail-image {
  display: block;
  width: 100%;
  border-radius: 20rpx;
  margin-top: 24rpx;
}
.consult {
  padding: 36rpx 0 0;
}
.consult .muted {
  margin: 16rpx 0 28rpx;
}
.consult .btn {
  margin-top: 16rpx;
}
</style>
