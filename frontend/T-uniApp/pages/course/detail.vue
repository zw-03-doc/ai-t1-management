<template>
  <view class="detail">
    <view v-if="error" class="card" @tap="load">{{ error }} · 点击重试</view>
    <view v-if="course" class="card">
      <image
        v-if="course.coverUrl"
        :src="course.coverUrl"
        mode="widthFix"
        class="cover"
      />
      <view class="title">{{ course.courseName }}</view
      ><view class="price"
        >¥{{ course.displayPrice || 0 }} <text>展示价格 · 线下咨询</text></view
      >
      <view class="meta"
        >{{ course.storeName }} · {{ course.coachName || "待安排教练" }}</view
      >
      <view class="meta"
        >{{ course.durationMinutes }} 分钟 · 最多 {{ course.capacity }} 人</view
      >
      <view class="description">{{
        course.description || "详情请咨询负责教练。"
      }}</view>
      <view class="meta"
        >可提前 {{ course.bookingAdvanceDays }} 天预约 · 开课前
        {{ course.bookingCloseMinutes }} 分钟停止预约</view
      >
      <view v-if="course.listingUntil" class="meta"
        >本轮上架截止：{{ course.listingUntil }}（北京时间）</view
      >
      <button class="primary" @tap="contact">联系负责私教</button
      ><view v-if="course.coachContactWechat" class="meta"
        >微信：{{ course.coachContactWechat }}</view
      >
      <button @tap="reserve">查看可约时段</button>
    </view>
    <view class="card disclosure"
      ><view>课程推荐偏好</view
      ><view class="meta"
        >开启后，记录本页前台浏览时长，供负责教练了解你关注的课程。仅作推荐参考。</view
      ><switch :checked="tracking" @change="setTracking" /><text>{{
        tracking ? "已开启" : "已关闭"
      }}</text></view
    >
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
      uni.navigateTo({ url: "/pages/course/index?tab=schedule" });
    },
  },
};
</script>
<style scoped>
.detail {
  max-width: 850px;
  margin: auto;
  padding: 24rpx;
}
.card {
  background: #fff;
  padding: 32rpx;
  border-radius: 22rpx;
  margin-bottom: 24rpx;
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  margin: 20rpx 0;
}
.price {
  font-size: 40rpx;
  color: #246348;
}
.price text,
.meta {
  font-size: 25rpx;
  color: #6b7b73;
  line-height: 1.8;
}
.description {
  white-space: pre-wrap;
  margin: 30rpx 0;
  font-size: 29rpx;
  line-height: 1.8;
}
.cover {
  width: 100%;
  border-radius: 16rpx;
}
.primary {
  background: #246348;
  color: #fff;
}
button {
  margin-top: 20rpx;
}
.disclosure {
  font-size: 27rpx;
}
.disclosure switch {
  margin-top: 20rpx;
}
</style>
