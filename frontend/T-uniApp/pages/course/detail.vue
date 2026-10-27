<template>
  <view class="page-wrap">
    <view class="back-link" @tap="goBack">‹ 返回课程</view>
    <view v-if="error" class="empty error" @tap="load"
      ><text class="empty-title">课程暂时没能加载</text>{{ error
      }}<view>轻点重试</view></view
    >
    <view v-else-if="!course" class="empty">正在加载课程…</view>
    <block v-if="course">
      <image
        v-if="course.coverUrl"
        :src="course.coverUrl"
        mode="aspectFill"
        class="cover"
      />
      <view class="eyebrow">T1 / {{ typeText(course.courseType) }}</view>
      <view class="page-title">{{ course.courseName }}</view>
      <view class="page-subtitle">{{ course.storeName || "门店待定" }}</view>
      <view class="facts">
        <view class="fact"
          ><text class="fact-value"
            >{{ course.durationMinutes == null ? "—" : course.durationMinutes
            }}<text class="unit">分钟</text></text
          ><text class="muted">单节时长</text></view
        >
        <view class="fact"
          ><text class="fact-value"
            >{{ course.capacity == null ? "—" : course.capacity
            }}<text class="unit">人</text></text
          ><text class="muted">每节人数上限</text></view
        >
        <view class="fact"
          ><text class="fact-price">{{ priceText(course.displayPrice) }}</text
          ><text class="muted">课程参考价</text></view
        >
      </view>
      <view class="panel">
        <view class="section-title">这节课，适合怎样的你</view>
        <view class="description">{{
          course.description || "课程内容和训练安排，请与负责教练沟通。"
        }}</view>
      </view>
      <view class="panel coach-panel">
        <view class="row"
          ><view
            ><view class="label">负责教练</view
            ><view class="coach-name">{{
              course.coachName || "教练待安排"
            }}</view></view
          ><text class="coach-mark">T1</text></view
        >
        <view class="muted coach-note"
          >对强度、上课方式或价格有疑问，先与教练聊聊。</view
        >
        <view v-if="course.coachContactWechat" class="contact-account"
          >微信 · {{ course.coachContactWechat }}</view
        >
        <button class="btn ghost" @tap="contact">
          {{
            course.coachContactWechat ? "复制微信号 · 咨询教练" : "咨询课程安排"
          }}
        </button>
      </view>
      <view class="panel">
        <view class="section-title">预约前了解</view>
        <view class="rule-row" v-if="course.bookingAdvanceDays != null"
          ><text>可提前预约</text
          ><text>{{ course.bookingAdvanceDays }} 天</text></view
        >
        <view class="rule-row" v-if="course.bookingCloseMinutes != null"
          ><text>预约截止</text
          ><text>开课前 {{ course.bookingCloseMinutes }} 分钟</text></view
        >
        <view class="rule-row" v-if="course.cancelCloseMinutes != null"
          ><text>取消截止</text
          ><text>开课前 {{ course.cancelCloseMinutes }} 分钟</text></view
        >
        <view class="rule-row" v-if="course.listingUntil"
          ><text>本轮上架至</text
          ><text>{{ dateText(course.listingUntil) }}</text></view
        >
        <view class="muted rule-note"
          >预约的是具体日期与时段。教练、名额和参考价以所选场次为准，费用请线下咨询。</view
        >
      </view>
      <button class="btn reserve-button" @tap="reserve">
        选择日期与时段 →
      </button>
      <view class="consent-card">
        <view class="row"
          ><view class="section-title">让教练了解你的兴趣</view
          ><switch :checked="tracking" color="#ff7a35" @change="setTracking"
        /></view>
        <view class="muted"
          >开启后，将本页停留时长用于课程推荐。只统计页面打开且在前台的时间，可随时关闭。</view
        >
        <view class="consent-state">{{
          tracking ? "已允许记录浏览时长" : "当前不记录浏览时长"
        }}</view>
      </view>
    </block>
  </view>
</template>
<script>
import { request } from "../../utils/request";
export default {
  data() {
    return {
      courseId: "",
      course: null,
      error: "",
      tracking: false,
      sessionId: "",
      timer: null,
      lastTick: 0,
      visible: false,
    };
  },
  onLoad(options) {
    this.courseId = options.id;
    this.tracking = uni.getStorageSync("T1_INTEREST_CONSENT") === true;
    this.sessionId = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    this.load();
  },
  onShow() {
    this.visible = true;
    this.startTimer();
  },
  onHide() {
    this.stopTimer();
    this.visible = false;
  },
  onUnload() {
    this.stopTimer();
    this.visible = false;
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => uni.redirectTo({ url: "/pages/course/index" }),
      });
    },
    priceText(value) {
      return value === null || value === undefined || value === ""
        ? "价格待咨询"
        : "¥" + Number(value).toFixed(2);
    },
    dateText(value) {
      if (!value) return "";
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return "";
      return (
        date.getFullYear() +
        " / " +
        String(date.getMonth() + 1).padStart(2, "0") +
        " / " +
        String(date.getDate()).padStart(2, "0")
      );
    },
    typeText(value) {
      return (
        {
          group: "团体课",
          private: "私教课",
          small: "小班课",
          experience: "体验课",
        }[value] || "训练课程"
      );
    },

    async load() {
      try {
        this.course = await request({
          url: `/fitness/mini/course/${this.courseId}`,
          auth: "member",
        });
        this.error = "";
      } catch (e) {
        this.error = e.message;
      }
    },
    setTracking(e) {
      this.stopTimer();
      this.tracking = e.detail.value;
      uni.setStorageSync("T1_INTEREST_CONSENT", this.tracking);
      this.startTimer();
    },
    startTimer() {
      if (!this.tracking || this.timer || !this.visible) return;
      this.lastTick = Date.now();
      this.timer = setInterval(() => this.heartbeat(), 10000);
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
        this.heartbeat();
      }
    },
    heartbeat() {
      const now = Date.now();
      const seconds = Math.floor((now - this.lastTick) / 1000);
      this.lastTick = now;
      if (!this.tracking || !this.visible || !this.course || seconds < 1)
        return;
      let source = "mini";
      // #ifdef H5
      source = "web";
      if (document.visibilityState !== "visible") return;
      // #endif
      request({
        url: `/fitness/mini/course/${this.courseId}/interest`,
        method: "POST",
        auth: "member",
        noRepeat: true,
        data: {
          sessionId: this.sessionId,
          eventId: `${this.sessionId}-${now}`,
          activeSeconds: Math.min(10, seconds),
          source,
        },
      }).catch(() => {});
    },
    contact() {
      if (!this.course.coachContactWechat) {
        uni.showToast({
          title: "教练尚未配置咨询微信号，请联系门店",
          icon: "none",
        });
        return;
      }
      uni.setClipboardData({ data: this.course.coachContactWechat });
    },
    reserve() {
      uni.navigateTo({
        url:
          "/pages/course/index?tab=schedule&courseId=" +
          encodeURIComponent(this.courseId) +
          "&courseName=" +
          encodeURIComponent(this.course.courseName || ""),
      });
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
  height: 380rpx;
  border-radius: 20rpx;
  margin-bottom: 32rpx;
}
.facts {
  display: flex;
  align-items: stretch;
  margin-top: 32rpx;
  padding: 28rpx 0;
  border-top: 1rpx solid #30322f;
  border-bottom: 1rpx solid #30322f;
}
.fact {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  min-width: 0;
  padding: 0 16rpx;
  border-right: 1rpx solid #30322f;
}
.fact:first-child {
  padding-left: 0;
}
.fact:last-child {
  border: 0;
  padding-right: 0;
}
.fact-value {
  font-size: 40rpx;
  font-weight: 700;
}
.unit {
  font-size: 21rpx;
  margin-left: 8rpx;
  font-weight: 400;
  color: #a2a49c;
}
.fact .muted {
  font-size: 21rpx;
}
.fact-price {
  font-size: 31rpx;
  color: #ff7a35;
  font-weight: 700;
  line-height: 1.6;
  word-break: break-all;
}
.description {
  white-space: pre-wrap;
  margin-top: 24rpx;
  font-size: 28rpx;
  line-height: 1.9;
  color: #d6d7d0;
}
.label {
  font-size: 23rpx;
  color: #a2a49c;
}
.coach-name {
  font-size: 36rpx;
  font-weight: 700;
  margin-top: 12rpx;
}
.coach-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88rpx;
  height: 88rpx;
  border: 1rpx solid #494438;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #ff7a35;
}
.coach-note {
  margin: 24rpx 0;
}
.contact-account {
  font-size: 26rpx;
  color: #f6f4ef;
  margin-bottom: 20rpx;
  word-break: break-all;
}
.coach-panel .btn {
  width: 100%;
}
.rule-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #30322f;
  color: #a2a49c;
  font-size: 24rpx;
}
.rule-row text:last-child {
  color: #f6f4ef;
  text-align: right;
}
.rule-note {
  font-size: 23rpx;
  margin-top: 24rpx;
}
.reserve-button {
  margin-top: 32rpx;
}
.consent-card {
  padding: 32rpx 0 8rpx;
  margin-top: 16rpx;
}
.consent-card .section-title {
  font-size: 26rpx;
}
.consent-card .muted {
  font-size: 23rpx;
  margin-top: 14rpx;
}
.consent-card switch {
  flex-shrink: 0;
  transform: scale(0.85);
  transform-origin: right center;
}
.consent-state {
  color: #a2a49c;
  font-size: 21rpx;
  margin-top: 14rpx;
}
</style>
