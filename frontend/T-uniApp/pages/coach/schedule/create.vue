<template>
  <view class="t1-page create-page">
    <view v-if="loading" class="t1-card">正在加载课程…</view>
    <view v-else-if="loadError" class="t1-card">
      <text>{{ loadError }}</text>
      <button class="retry" @tap="loadCourses">重新加载</button>
    </view>
    <view v-else-if="!courses.length" class="t1-card">
      <view class="state-title">暂无可排课程</view>
      <view class="muted">请联系管理员将已上架课程分配给你。</view>
    </view>
    <block v-else>
      <view class="t1-card fields">
        <picker class="course-picker" mode="selector" :range="courses" range-key="courseName"
          :value="courseIndex" :disabled="submitting || published" @change="selectCourse">
          <view class="field"><text>课程</text><text class="value">{{ course.courseName }} ›</text></view>
        </picker>
        <view class="terms">
          <text>{{ course.durationMinutes }} 分钟</text>
          <text>限 {{ course.capacity }} 人</text>
          <text v-if="course.displayPrice != null">¥{{ course.displayPrice }}</text>
        </view>
        <picker class="date-picker" mode="date" :start="today" :value="scheduleDate" :disabled="submitting || published" @change="selectDate">
          <view class="field"><text>开课日期</text><text class="value">{{ scheduleDate }} ›</text></view>
        </picker>
        <picker class="time-picker" mode="time" :value="startTime || '09:00'" :disabled="submitting || published" @change="selectTime">
          <view class="field"><text>开始时间</text><text class="value">{{ startTime || "选择时间" }} ›</text></view>
        </picker>
        <view v-if="endTime" class="field end-time"><text>结束时间</text><text>{{ endTime }}</text></view>
      </view>
      <view v-if="submitError" class="submit-error">{{ submitError }}</view>
      <button class="t1-primary submit-schedule" :disabled="submitting || !startTime || published"
        :loading="submitting" @tap="submitSchedule">确认排课</button>
      <button v-if="published" class="retry" @tap="showSchedule">查看本日排课</button>
    </block>
  </view>
</template>

<script>
import { coachScheduleCourses, coachCreateSchedule } from "../../../api/auth";

function studioToday() {
  return new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

export default {
  data() {
    const today = studioToday();
    return {
      today,
      scheduleDate: today,
      startTime: "",
      courses: [],
      courseIndex: 0,
      loading: true,
      submitting: false,
      published: false,
      loadError: "",
      submitError: "",
    };
  },
  computed: {
    course() {
      return this.courses[this.courseIndex];
    },
    endTime() {
      if (!this.startTime || !this.course) return "";
      const [hour, minute] = this.startTime.split(":").map(Number);
      const end = hour * 60 + minute + Number(this.course.durationMinutes);
      return (end >= 1440 ? "次日 " : "") +
        String(Math.floor(end / 60) % 24).padStart(2, "0") + ":" + String(end % 60).padStart(2, "0");
    },
  },
  onLoad() {
    this.loadCourses();
  },
  onShow() {
    this.today = studioToday();
  },
  methods: {
    async loadCourses() {
      this.loading = true;
      this.loadError = "";
      try {
        this.courses = await coachScheduleCourses();
        this.courseIndex = 0;
      } catch (e) {
        this.loadError = e.message || "课程加载失败";
      } finally {
        this.loading = false;
      }
    },
    selectCourse(event) {
      this.courseIndex = Number(event.detail.value);
      this.submitError = "";
    },
    selectDate(event) {
      this.scheduleDate = event.detail.value;
      this.submitError = "";
    },
    selectTime(event) {
      this.startTime = event.detail.value;
      this.submitError = "";
    },
    async submitSchedule() {
      if (!this.course || !this.startTime || this.submitting || this.published) return;
      if (Date.parse(this.scheduleDate + "T" + this.startTime + ":00+08:00") <= Date.now()) {
        this.submitError = "请选择未来的开课时间";
        return;
      }
      this.submitting = true;
      this.submitError = "";
      try {
        await coachCreateSchedule({
          courseId: this.course.courseId,
          scheduleDate: this.scheduleDate,
          startTime: this.startTime,
        });
        this.published = true;
        uni.showToast({ title: "排课已发布", icon: "success" });
        this.showSchedule();
      } catch (e) {
        this.submitError = e.message || "排课失败，请重试";
      } finally {
        this.submitting = false;
      }
    },
    showSchedule() {
      const pages = getCurrentPages();
      const previous = pages[pages.length - 2];
      const list = "pages/coach/schedule/index";
      const channel = this.getOpenerEventChannel?.();
      if (previous?.route === list && channel?.emit) {
        channel.emit("scheduled", this.scheduleDate);
        uni.navigateBack({ delta: 1 });
      } else {
        uni.redirectTo({ url: "/" + list + "?date=" + this.scheduleDate });
      }
    },
  },
};
</script>

<style scoped>
.fields { padding: 0 26rpx; }
.field {
  min-height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
}
.value { max-width: 72%; text-align: right; overflow-wrap: anywhere; }
.date-picker, .time-picker { display: block; border-top: 1px solid var(--t1-border); }
.terms { display: flex; flex-wrap: wrap; gap: 20rpx; padding-bottom: 24rpx; }
.terms, .end-time, .muted { color: var(--t1-muted); font-size: 25rpx; }
.end-time { min-height: 70rpx; padding-bottom: 20rpx; }
.submit-schedule, .retry { margin-top: 28rpx; }
.submit-schedule { display: flex; align-items: center; justify-content: center; }
.retry { min-height: 80rpx; font-size: 26rpx; background: var(--t1-inset); }
.state-title { font-weight: 650; margin-bottom: 12rpx; }
.submit-error { color: var(--t1-danger); margin-top: 20rpx; }
</style>
