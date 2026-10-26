<template>
  <view class="home-page">
    <view class="greeting"
      ><view
        ><view class="date-line">{{ dateLabel }}</view
        ><view class="greeting-title"
          >{{ displayName ? `${displayName}，` : "" }}今天练点什么？</view
        ></view
      ><view class="avatar" @tap="go('/pages/mine/index')">{{
        (displayName || "T1").slice(0, 2)
      }}</view></view
    >
    <view v-if="error" class="error-card" @tap="load"
      >{{ error }} · 重新加载 ↗</view
    >
    <view class="training-focus">
      <image
        class="training-photo"
        src="/static/images/live-2.jpg"
        mode="aspectFill"
      /><view class="training-shade" />
      <view class="focus-content">
        <view class="focus-top"
          ><text class="focus-label">{{
            nextWorkout ? workoutLabel : "我的训练"
          }}</text
          ><text class="focus-mark">T1 / TRAIN</text></view
        >
        <view class="focus-title">{{
          loading
            ? "正在读取训练安排"
            : trainingError
              ? "查看我的训练"
              : nextWorkout
                ? nextWorkout.title
                : activePlan
                  ? activePlan.name
                  : "从一份计划开始"
        }}</view>
        <view class="focus-description">{{ trainingDescription }}</view>
        <view
          v-if="nextWorkout && nextWorkout.status !== 'rest'"
          class="focus-meta"
          ><text>{{ nextWorkout.exercises?.length || 0 }} 个动作</text
          ><text>{{ targetSets }} 组目标</text
          ><text>{{ activePlan.cycleDays }} 天循环</text></view
        >
        <button class="focus-button" @tap="go('/pages/training/index')">
          <text>{{ activePlan ? "查看训练安排" : "定制我的计划" }}</text
          ><text>→</text>
        </button>
      </view>
    </view>
    <view class="quick-grid">
      <button @tap="go('/pages/course/index')">
        <text class="quick-number">01</text
        ><text class="quick-title">预约课程</text
        ><text class="quick-caption">找到适合的一节课 ↗</text>
      </button>
      <button @tap="go('/pages/course/index?tab=reservation')">
        <text class="quick-number">02</text
        ><text class="quick-title">到店签到</text
        ><text class="quick-caption">查看我的预约 ↗</text>
      </button>
      <button @tap="go('/pages/guide/index')">
        <text class="quick-number">03</text
        ><text class="quick-title">动作指南</text
        ><text class="quick-caption">把每一组练扎实 ↗</text>
      </button>
    </view>
    <view class="section-heading"
      ><view
        ><text class="section-title">我的预约</text
        ><view class="section-caption">查看上课时间，到店后完成签到</view></view
      ><button @tap="go('/pages/course/index?tab=reservation')">
        全部预约 ↗
      </button></view
    >
    <view v-if="loading" class="loading-card">正在读取预约…</view>
    <view v-else-if="!homeData && error" class="empty-booking"
      ><view class="empty-title">预约暂时没能加载</view
      ><view class="section-caption">请检查网络后重试。</view
      ><button @tap="load">重新加载 →</button></view
    >
    <view v-else-if="upcoming.length" class="upcoming-list">
      <view
        v-for="item in upcoming.slice(0, 2)"
        :key="item.reservationId"
        class="booking-card"
        @tap="go('/pages/course/index?tab=reservation')"
      >
        <view class="booking-clock"
          ><text>{{ clock(item.startTime) }}</text
          ><text class="booking-date">{{
            date(item.scheduleDate || item.startTime)
          }}</text></view
        >
        <view class="booking-main"
          ><view class="booking-name">{{ item.courseName || "预约课程" }}</view
          ><view class="booking-caption"
            >{{ item.coachName || "教练待安排" }} ·
            {{ item.storeName || "门店待安排" }}</view
          ></view
        ><text class="arrow">↗</text>
      </view>
    </view>
    <view v-else class="empty-booking"
      ><view class="empty-title">暂时没有待上课的预约</view
      ><view class="section-caption">看看课表，留一段时间给自己。</view
      ><button @tap="go('/pages/course/index')">去看看课程 →</button></view
    >
    <view class="summary-strip"
      ><view v-for="item in stats" :key="item.label"
        ><text class="summary-value">{{ item.value }}</text
        ><text class="summary-label">{{ item.label }}</text></view
      ></view
    >
    <view class="section-heading"
      ><view
        ><text class="section-title">训练，也有自己的节奏</text
        ><view class="section-caption"
          >从三分化或五分化开始，按你的日程调整。</view
        ></view
      ></view
    >
    <view class="inspiration-card" @tap="go('/pages/guide/index')"
      ><image src="/static/images/live-1.jpg" mode="aspectFill" /><view
        class="inspiration-copy"
        ><text class="small-label">TRAINING NOTES</text
        ><view class="inspiration-title"
          ><view>找到适合你的</view><view>下一份训练计划</view></view
        ><text class="inspiration-link">探索训练灵感 ↗</text></view
      ></view
    >
    <T1Nav role="member" current="home" />
  </view>
</template>
<script>
import { memberHomeIndex } from "../../api/auth";
import { getMemberSession } from "../../utils/auth";
import { trainingApi } from "../../api/training";
import T1Nav from "../../components/T1Nav.vue";
const chinaNow = () => new Date(Date.now() + 8 * 3600000);
const today = () => chinaNow().toISOString().slice(0, 10);
export default {
  components: { T1Nav },
  data() {
    return {
      homeData: null,
      activePlan: null,
      nextWorkout: null,
      loading: false,
      error: "",
      trainingError: false,
    };
  },
  computed: {
    displayName() {
      const m =
        this.homeData?.mineSummary || getMemberSession().memberInfo || {};
      return m.realName || m.nickname || "";
    },
    dateLabel() {
      const d = chinaNow();
      return `${d.getUTCMonth() + 1}月${d.getUTCDate()}日 · 星期${"日一二三四五六"[d.getUTCDay()]}`;
    },
    upcoming() {
      return this.homeData?.upcomingReservationList || [];
    },
    stats() {
      const s = this.homeData?.mineSummary;
      return [
        { label: "累计预约", value: s ? s.totalReservationCount || 0 : "—" },
        { label: "待上课程", value: s ? s.pendingReservationCount || 0 : "—" },
        { label: "有效会员卡", value: s ? s.activeCardCount || 0 : "—" },
      ];
    },
    workoutLabel() {
      return this.nextWorkout?.plannedDate === today()
        ? "今日训练"
        : this.nextWorkout?.plannedDate < today()
          ? "待完成的训练"
          : "下一次训练";
    },
    targetSets() {
      return (this.nextWorkout?.exercises || []).reduce(
        (n, e) => n + (e.targets?.length || 0),
        0,
      );
    },
    trainingDescription() {
      if (this.loading) return "稍等一下，把今天的安排准备好。";
      if (this.trainingError) return "训练安排暂时未能读取，点下方重新查看。";
      if (this.nextWorkout?.status === "rest")
        return "这一训练日安排休息，为下次训练留好状态。";
      if (this.nextWorkout)
        return `${this.activePlan.name} · ${this.date(this.nextWorkout.plannedDate)}`;
      if (this.activePlan) return "这轮日程已完成，打开计划看看你的训练记录。";
      return "选一份免费模板，安排训练日，再记录每一组。";
    },
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      if (this.loading) return;
      this.loading = true;
      this.error = "";
      this.trainingError = false;
      const results = await Promise.allSettled([
        memberHomeIndex(),
        trainingApi("/plans"),
      ]);
      if (results[0].status === "fulfilled")
        this.homeData = results[0].value || {};
      else this.error = "首页暂时未能加载";
      this.activePlan = null;
      this.nextWorkout = null;
      if (results[1].status === "fulfilled") {
        this.activePlan =
          (results[1].value || []).find((p) => p.status === "active") || null;
        if (this.activePlan) {
          try {
            const workouts = await trainingApi(
              `/plans/${this.activePlan.planId}/workouts`,
            );
            const candidates = (workouts || [])
              .filter(
                (w) =>
                  w.status === "planned" ||
                  (w.status === "rest" && w.plannedDate >= today()),
              )
              .sort((a, b) => a.plannedDate.localeCompare(b.plannedDate));
            this.nextWorkout =
              candidates.find((w) => w.plannedDate === today()) ||
              candidates[0] ||
              null;
          } catch (_) {
            this.trainingError = true;
          }
        }
      } else this.trainingError = true;
      this.loading = false;
    },
    go(url) {
      uni.navigateTo({ url });
    },
    clock(value) {
      const m = String(value || "").match(/(?:T|\s)(\d{2}:\d{2})/);
      return m ? m[1] : "待定";
    },
    date(value) {
      const m = String(value || "").match(/(\d{4})-(\d{2})-(\d{2})/);
      return m ? `${Number(m[2])}月${Number(m[3])}日` : "日期待定";
    },
  },
};
</script>
<style scoped>
.home-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 24rpx 32rpx calc(170rpx + env(safe-area-inset-bottom));
}
.greeting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 30rpx;
}
.date-line {
  color: #a2a49c;
  font-size: 23rpx;
  margin-bottom: 12rpx;
}
.greeting-title {
  color: #f6f4ef;
  font-size: 37rpx;
  font-weight: 700;
  line-height: 1.4;
}
.avatar {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #30332c;
  border: 1px solid #414638;
  color: #e0e3d7;
  text-align: center;
  line-height: 72rpx;
  font-size: 24rpx;
}
.training-focus {
  position: relative;
  overflow: hidden;
  border-radius: 24rpx;
  min-height: 420rpx;
  background: #242721;
  border: 1px solid #373d30;
}
.training-photo,
.training-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.training-photo {
  opacity: 0.55;
}
.training-shade {
  background: linear-gradient(
    90deg,
    #1c2119 0%,
    rgba(28, 33, 25, 0.7) 50%,
    rgba(28, 33, 25, 0.12) 100%
  );
}
.focus-content {
  position: relative;
  padding: 28rpx;
}
.focus-top {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  align-items: center;
}
.focus-label {
  background: #ff7a35;
  color: #20160f;
  padding: 6rpx 15rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 700;
}
.focus-mark {
  font-size: 18rpx;
  letter-spacing: 4rpx;
  color: #d1d6c8;
}
.focus-title {
  font-size: 47rpx;
  font-weight: 800;
  line-height: 1.25;
  margin: 28rpx 0 14rpx;
}
.focus-description {
  color: #c6ccbd;
  font-size: 24rpx;
  max-width: 92%;
  line-height: 1.7;
}
.focus-meta {
  display: flex;
  gap: 22rpx;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: #ebeee5;
}
.focus-button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 30rpx 0 0;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  color: #191c16;
  background: #e7eddf;
  font-size: 27rpx;
  font-weight: 700;
}
.quick-grid {
  display: flex;
  gap: 12rpx;
  margin-top: 20rpx;
}
.quick-grid button {
  flex: 1;
  min-width: 0;
  margin: 0;
  padding: 22rpx 15rpx;
  background: #1c1d1b;
  border: 1px solid #30322f;
  color: #f6f4ef;
  text-align: left;
  border-radius: 16rpx;
  line-height: 1.5;
}
.quick-number {
  color: #ff7a35;
  font-size: 20rpx;
  display: block;
  margin-bottom: 15rpx;
  letter-spacing: 2rpx;
}
.quick-title {
  font-weight: 600;
  font-size: 26rpx;
  display: block;
}
.quick-caption {
  display: block;
  font-size: 19rpx;
  color: #969b8e;
  margin-top: 6rpx;
}
.section-heading {
  margin: 40rpx 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
}
.section-caption {
  font-size: 23rpx;
  color: #969b8e;
  line-height: 1.7;
  margin-top: 8rpx;
}
.section-heading button {
  background: transparent;
  color: #b9bdb1;
  white-space: nowrap;
  margin: 0;
  padding: 12rpx 0 12rpx 14rpx;
  font-size: 22rpx;
  line-height: 1.5;
}
.booking-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: #1c1d1b;
  border: 1px solid #30322f;
  padding: 28rpx 24rpx;
  border-radius: 18rpx;
  margin-bottom: 12rpx;
}
.booking-clock {
  flex-shrink: 0;
  min-width: 95rpx;
  color: #ff9c66;
  font-size: 32rpx;
  font-weight: 700;
}
.booking-date {
  display: block;
  font-size: 20rpx;
  color: #a2a49c;
  font-weight: 400;
  margin-top: 4rpx;
}
.booking-main {
  flex: 1;
  min-width: 0;
  border-left: 1px solid #3b3f35;
  padding-left: 24rpx;
}
.booking-name {
  font-size: 29rpx;
  font-weight: 600;
}
.booking-caption {
  font-size: 22rpx;
  color: #a2a49c;
  margin-top: 8rpx;
}
.arrow {
  color: #7e8574;
  font-size: 30rpx;
}
.empty-booking,
.loading-card {
  border: 1px solid #30322f;
  border-radius: 18rpx;
  padding: 30rpx;
  background: #191b17;
}
.empty-title {
  font-size: 28rpx;
  font-weight: 600;
}
.empty-booking button {
  background: transparent;
  color: #ff9c66;
  padding: 18rpx 0 0;
  margin: 0;
  text-align: left;
  font-size: 24rpx;
  line-height: 1.5;
}
.summary-strip {
  display: flex;
  border-top: 1px solid #30322f;
  border-bottom: 1px solid #30322f;
  margin-top: 32rpx;
  padding: 24rpx 0;
}
.summary-strip > view {
  flex: 1;
  text-align: center;
  border-right: 1px solid #30322f;
}
.summary-strip > view:last-child {
  border-right: 0;
}
.summary-value {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}
.summary-label {
  color: #969b8e;
  font-size: 22rpx;
}
.inspiration-card {
  position: relative;
  border: 1px solid #33382d;
  border-radius: 20rpx;
  overflow: hidden;
  min-height: 270rpx;
  background: #242820;
}
.inspiration-card image {
  position: absolute;
  width: 48%;
  right: 0;
  height: 100%;
  opacity: 0.65;
}
.inspiration-copy {
  position: relative;
  padding: 28rpx;
  background: linear-gradient(90deg, #23271e 45%, transparent);
}
.small-label {
  font-size: 17rpx;
  color: #b0bba2;
  letter-spacing: 3rpx;
}
.inspiration-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 14rpx 0 24rpx;
  line-height: 1.4;
}
.inspiration-link {
  font-size: 22rpx;
  color: #e0e7d5;
}
.error-card {
  color: #ffc59d;
  background: #33271d;
  border-radius: 14rpx;
  padding: 20rpx;
  font-size: 25rpx;
  margin-bottom: 20rpx;
}
</style>
