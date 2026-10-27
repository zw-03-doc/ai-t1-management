<template>
  <view class="coach-page">
    <view class="hero">
      <image
        class="hero-image"
        src="/static/images/live-2.jpg"
        mode="aspectFill"
      />
      <view class="hero-shade"></view>
      <view class="hero-content">
        <text class="eyebrow">T1 FITNESS / 教练工作台</text>
        <view class="hero-title">今日教学安排</view>
        <view class="hero-sub"
          >{{ coachName }} · {{ coachProfile.storeName || "门店待分配" }}</view
        >
      </view>
    </view>

    <view class="stats-grid">
      <button
        class="stat"
        v-for="item in stats"
        :key="item.label"
        @tap="goPage(item.url)"
      >
        <text class="stat-number">{{ loaded ? item.value : "—" }}</text>
        <text class="stat-label">{{ item.label }}</text>
      </button>
    </view>

    <view v-if="loading" class="state-card"
      ><text class="state-title">正在整理今日工作</text
      ><text class="muted">请稍候</text></view
    >
    <view v-else-if="errorText" class="state-card"
      ><text class="state-title">暂时无法加载</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="loadOverview">重新加载</button></view
    >
    <block v-else>
      <view class="section-head"
        ><text class="section-title">今日授课</text
        ><button class="text-btn" @tap="goPage('/pages/coach/schedule/index')">
          全部排课 ›
        </button></view
      >
      <view class="panel">
        <view v-if="!scheduleList.length" class="empty"
          ><text class="state-title">今天暂无课程</text
          ><text class="muted">下一次授课安排可在排课中查看。</text></view
        >
        <button
          v-for="item in scheduleList"
          :key="item.scheduleId"
          class="lesson-row"
          @tap="goPage('/pages/coach/schedule/index')"
        >
          <view class="time-column"
            ><text class="time-main">{{ formatTime(item.startTime) }}</text
            ><text class="muted">{{ formatTime(item.endTime) }}</text></view
          >
          <view class="row-main"
            ><text class="row-title">{{ item.courseName || "课程待定" }}</text
            ><text class="muted">{{ item.storeName || "门店待定" }}</text
            ><text class="lesson-count"
              >已确认 {{ item.signedCount || 0 }} / 已预约
              {{ item.reservedCount || 0 }}</text
            ></view
          >
          <text class="arrow">›</text>
        </button>
      </view>

      <view class="section-head"
        ><text class="section-title">签到待办</text
        ><button class="text-btn" @tap="goPage('/pages/coach/checkin/index')">
          处理签到 ›
        </button></view
      >
      <view class="panel">
        <view v-if="!pendingList.length" class="empty"
          ><text class="state-title">当前没有待处理签到</text
          ><text class="muted">新的到课记录会显示在这里。</text></view
        >
        <button
          v-for="item in pendingList"
          :key="item.checkinId || item.reservationId"
          class="pending-row"
          @tap="goPage('/pages/coach/checkin/index')"
        >
          <view class="row-main"
            ><text class="row-title">{{ item.memberName || "会员" }}</text
            ><text class="muted"
              >{{ item.courseName || "课程待定" }} ·
              {{ formatTime(item.checkinTime || item.startTime) }}</text
            ></view
          >
          <text class="status-chip">{{
            item.checkinId ? "待核销" : "待签到"
          }}</text>
        </button>
      </view>
    </block>

    <view class="section-head"
      ><text class="section-title">学员与服务</text></view
    >
    <view class="entry-grid">
      <button class="entry-card" @tap="goPage('/pages/coach/student/index')">
        <text class="entry-kicker">学员管理</text
        ><text class="entry-title">我的学员 <text class="arrow">›</text></text
        ><text class="muted">到课记录与训练计划</text>
      </button>
      <button class="entry-card" @tap="goPage('/pages/coach/interest/index')">
        <text class="entry-kicker">课程沟通</text
        ><text class="entry-title">课程兴趣 <text class="arrow">›</text></text
        ><text class="muted">了解会员关注的课程</text>
      </button>
    </view>
    <view
      v-if="!loading && !errorText && messageList.length"
      class="notice-section"
    >
      <view class="section-head"
        ><text class="section-title">工作提醒</text
        ><button class="text-btn" @tap="goPage('/pages/coach/mine/index')">
          查看全部 ›
        </button></view
      >
      <view class="panel"
        ><view
          v-for="(item, index) in messageList.slice(0, 3)"
          :key="index"
          class="notice"
          ><text class="row-title">{{ item.title }}</text
          ><text class="muted">{{ item.content }}</text
          ><text class="notice-time">{{
            formatDateTime(item.eventTime)
          }}</text></view
        ></view
      >
    </view>
    <T1Nav role="coach" current="home" />
  </view>
</template>

<script>
import { coachHomeOverview } from "../../../api/auth";
import { getCoachSession } from "../../../utils/auth";
import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return {
      loading: false,
      loaded: false,
      errorText: "",
      overview: {},
      coachSession: {},
    };
  },
  computed: {
    coachProfile() {
      return this.coachSession.profile || {};
    },
    coachName() {
      return (
        this.coachProfile.coachName || this.coachProfile.nickName || "教练"
      );
    },
    stats() {
      const s = this.overview.stats || {};
      return [
        {
          label: "今日课程",
          value: s.todayScheduleCount || 0,
          url: "/pages/coach/schedule/index",
        },
        {
          label: "待核销",
          value: s.pendingCheckinCount || 0,
          url: "/pages/coach/checkin/index",
        },
        {
          label: "我的学员",
          value: s.totalStudentCount || 0,
          url: "/pages/coach/student/index",
        },
      ];
    },
    pendingList() {
      return this.overview.pendingList || [];
    },
    scheduleList() {
      return this.overview.todayScheduleList || [];
    },
    messageList() {
      return this.overview.messageList || [];
    },
  },
  onShow() {
    this.coachSession = getCoachSession() || {};
    this.loadOverview();
  },
  methods: {
    async loadOverview() {
      if (this.loading) return;
      this.loading = true;
      this.errorText = "";
      try {
        this.overview = (await coachHomeOverview()) || {};
        this.loaded = true;
      } catch (e) {
        this.errorText = e.message || "请检查网络后重试";
        this.loaded = false;
      } finally {
        this.loading = false;
      }
    },
    goPage(url) {
      uni.navigateTo({ url });
    },
    formatTime(value) {
      if (!value) return "--:--";
      const match = String(value).match(/(?:T|\s)(\d{2}:\d{2})/);
      if (match) return match[1];
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return "--:--";
      return (
        `${date.getHours()}`.padStart(2, "0") +
        ":" +
        `${date.getMinutes()}`.padStart(2, "0")
      );
    },
    formatDateTime(value) {
      if (!value) return "时间未提供";
      return String(value).replace("T", " ").slice(0, 16);
    },
  },
};
</script>

<style scoped lang="scss">
.coach-page {
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  min-height: 100vh;
  background: #111211;
  color: #f6f4ef;
  padding: 32rpx 32rpx calc(160rpx + env(safe-area-inset-bottom));
}
button {
  margin: 0;
  padding: 0;
  color: inherit;
  font: inherit;
  line-height: 1.5;
  text-align: left;
  background: transparent;
  border-radius: 0;
}
button::after {
  border: 0;
}
.hero {
  position: relative;
  height: 300rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: #1c1d1b;
}
.hero-image,
.hero-shade {
  position: absolute;
  width: 100%;
  height: 100%;
  left: 0;
  top: 0;
}
.hero-shade {
  background:
    linear-gradient(90deg, rgba(17, 18, 17, 0.89), rgba(17, 18, 17, 0.25)),
    linear-gradient(0deg, rgba(17, 18, 17, 0.6), transparent);
}
.hero-content {
  position: relative;
  padding: 32rpx;
  display: flex;
  height: 100%;
  box-sizing: border-box;
  flex-direction: column;
  justify-content: flex-end;
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 2rpx;
  color: #ff7a35;
}
.hero-title {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.45;
  margin: 18rpx 0 12rpx;
}
.hero-sub {
  font-size: 25rpx;
  color: #d8d9d2;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 20rpx 0 32rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
}
.stat {
  padding: 24rpx 8rpx;
  text-align: center;
}
.stat + .stat {
  border-left: 1rpx solid #30322f;
}
.stat-number {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #ff7a35;
}
.stat-label {
  display: block;
  color: #a2a49c;
  font-size: 23rpx;
  margin-top: 4rpx;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  gap: 16rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 650;
}
.text-btn {
  color: #a2a49c;
  font-size: 24rpx;
  min-height: 88rpx;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.panel {
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}
.lesson-row,
.pending-row {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 28rpx 24rpx;
  gap: 24rpx;
  box-sizing: border-box;
}
.lesson-row + .lesson-row,
.pending-row + .pending-row,
.notice + .notice {
  border-top: 1rpx solid #30322f;
}
.time-column {
  flex-shrink: 0;
  text-align: center;
  min-width: 90rpx;
}
.time-main {
  font-size: 31rpx;
  font-weight: 650;
  display: block;
}
.row-main {
  flex: 1;
  min-width: 0;
}
.row-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 1.5;
}
.muted {
  display: block;
  font-size: 24rpx;
  line-height: 1.65;
  color: #a2a49c;
  margin-top: 8rpx;
}
.lesson-count {
  display: block;
  font-size: 22rpx;
  margin-top: 12rpx;
  color: #c4c6bd;
}
.arrow {
  color: #ff7a35;
  font-size: 36rpx;
  font-weight: 400;
}
.status-chip {
  background: #30251d;
  color: #ff7a35;
  border-radius: 8rpx;
  padding: 8rpx 14rpx;
  font-size: 22rpx;
  flex-shrink: 0;
}
.state-card,
.empty {
  padding: 40rpx 26rpx;
  text-align: center;
}
.state-card {
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  background: #1c1d1b;
  margin: 24rpx 0;
}
.state-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
}
.secondary-btn {
  margin: 24rpx auto 0;
  padding: 18rpx 32rpx;
  min-height: 88rpx;
  width: 260rpx;
  background: #30322f;
  border-radius: 12rpx;
  font-size: 26rpx;
  text-align: center;
  box-sizing: border-box;
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}
.entry-card {
  padding: 26rpx 24rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
}
.entry-kicker {
  display: block;
  color: #ff7a35;
  font-size: 21rpx;
  margin-bottom: 18rpx;
}
.entry-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 30rpx;
  font-weight: 600;
}
.notice-section {
  margin-top: 24rpx;
}
.notice {
  padding: 24rpx;
}
.notice-time {
  display: block;
  font-size: 21rpx;
  color: #a2a49c;
  margin-top: 12rpx;
}
</style>
