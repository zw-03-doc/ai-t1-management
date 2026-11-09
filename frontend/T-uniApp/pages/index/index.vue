<template>
  <view class="home-page">
    <view class="greeting"
      ><view
        ><view class="date-line">{{ dateLabel }}</view
        ><view class="greeting-title"
          >今日安排</view
        ></view
      ><view class="avatar" @tap="go('/pages/mine/index')">{{
        (displayName || "T1").slice(0, 2)
      }}</view></view
    >
    <view v-if="error" class="error-card" @tap="load"
      >{{ error }} · 重新加载 ↗</view
    >
    <view class="training-focus">

      <view class="focus-content">
        <view class="focus-top"
          ><text class="focus-label">{{
            nextWorkout ? workoutLabel : "我的训练"
          }}</text
          ></view
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
                  : "暂无训练计划"
        }}</view>
        <view v-if="trainingDescription" class="focus-description">{{ trainingDescription }}</view>
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
      <button @tap="go('/pages/course/index?tab=schedule')"><text class="quick-title">预约课程</text
        >
      </button>
      <button @tap="go('/pages/course/index?tab=reservation')"><text class="quick-title">签到</text
        >
      </button>
      <button @tap="go('/pages/guide/index')"><text class="quick-title">动作指南</text
        >
      </button>
    </view>
    <view class="section-heading"
      ><view
        ><text class="section-title">我的预约</text
        ></view
      ><button @tap="go('/pages/course/index?tab=reservation')">
        全部预约 ↗
      </button></view
    >
    <view v-if="loading" class="loading-card">正在读取预约…</view>
    <view v-else-if="!homeData && error" class="empty-booking"
      ><view class="empty-title">预约暂时没能加载</view
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
      ><button @tap="go('/pages/course/index?tab=schedule')">去约课</button></view
    >
    <view class="summary-strip"
      ><view v-for="item in stats" :key="item.label"
        ><text class="summary-value">{{ item.value }}</text
        ><text class="summary-label">{{ item.label }}</text></view
      ></view
    ><T1Nav role="member" current="home" />
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
      if (this.loading) return "";
      if (this.trainingError) return "训练安排暂时未能读取，点下方重新查看。";
      if (this.nextWorkout?.status === "rest")
        return "休息日";
      if (this.nextWorkout)
        return `${this.activePlan.name} · ${this.date(this.nextWorkout.plannedDate)}`;
      if (this.activePlan) return "本轮已完成";
      return "";
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
  color: var(--t1-muted);
  font-size: 23rpx;
  margin-bottom: 12rpx;
}
.greeting-title {
  color: var(--t1-text);
  font-size: 37rpx;
  font-weight: 700;
  line-height: 1.4;
}
.avatar {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  color: var(--t1-muted);
  text-align: center;
  line-height: 72rpx;
  font-size: 24rpx;
}
.training-focus {
  overflow: hidden;
  border: 1px solid var(--t1-border);
  border-radius: 16rpx;
  background: var(--t1-surface);
}
.training-photo,
.training-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
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
  color: var(--t1-muted);
  font-size: 24rpx;
  font-weight: 500;
}
.focus-title {
  font-size: 47rpx;
  font-weight: 800;
  line-height: 1.25;
  margin: 28rpx 0 14rpx;
}
.focus-description {
  color: var(--t1-muted);
  font-size: 24rpx;
  max-width: 92%;
  line-height: 1.7;
}
.focus-meta {
  display: flex;
  gap: 22rpx;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: var(--t1-text);
}
.focus-button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 30rpx 0 0;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  color: var(--t1-on-dark);
  background: var(--t1-text);
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
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  color: var(--t1-text);
  text-align: center;
  border-radius: 12rpx;
  line-height: 1.5;
}
.quick-title {
  font-weight: 600;
  font-size: 26rpx;
  display: block;
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
.section-heading button {
  background: transparent;
  color: var(--t1-muted);
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
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  padding: 28rpx 24rpx;
  border-radius: 18rpx;
  margin-bottom: 12rpx;
}
.booking-clock {
  flex-shrink: 0;
  min-width: 95rpx;
  color: var(--t1-accent);
  font-size: 32rpx;
  font-weight: 700;
}
.booking-date {
  display: block;
  font-size: 20rpx;
  color: var(--t1-muted);
  font-weight: 400;
  margin-top: 4rpx;
}
.booking-main {
  flex: 1;
  min-width: 0;
  border-left: 1px solid var(--t1-border);
  padding-left: 24rpx;
}
.booking-name {
  font-size: 29rpx;
  font-weight: 600;
}
.booking-caption {
  font-size: 22rpx;
  color: var(--t1-muted);
  margin-top: 8rpx;
}
.arrow {
  color: var(--t1-muted);
  font-size: 30rpx;
}
.empty-booking,
.loading-card {
  border: 1px solid var(--t1-border);
  border-radius: 18rpx;
  padding: 30rpx;
  background: var(--t1-inset);
}
.empty-title {
  font-size: 28rpx;
  font-weight: 600;
}
.empty-booking button {
  background: transparent;
  color: var(--t1-accent);
  padding: 18rpx 0 0;
  margin: 0;
  text-align: left;
  font-size: 24rpx;
  line-height: 1.5;
}
.summary-strip {
  display: flex;
  border-top: 1px solid var(--t1-border);
  border-bottom: 1px solid var(--t1-border);
  margin-top: 32rpx;
  padding: 24rpx 0;
}
.summary-strip > view {
  flex: 1;
  text-align: center;
  border-right: 1px solid var(--t1-border);
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
  color: var(--t1-muted);
  font-size: 22rpx;
}
.error-card {
  color: var(--t1-danger);
  background: var(--t1-tint);
  border-radius: 14rpx;
  padding: 20rpx;
  font-size: 25rpx;
  margin-bottom: 20rpx;
}
</style>
