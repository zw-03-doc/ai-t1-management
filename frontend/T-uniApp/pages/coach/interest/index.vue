<template>
  <view class="interest-page">
    <view class="intro-card"
      ><view class="hint"
        >近 90 天已授权统计的课程浏览记录。</view
      ><button class="text-btn" @tap="goStudents">前往我的学员 ›</button></view
    >
    <view class="list-head"
      ><text>课程关注记录</text
      ><button class="refresh-btn" :disabled="loading" @tap="load">
        {{ loading ? "加载中" : "刷新" }}
      </button></view
    >
    <view v-if="loading" class="state-card"
      ><text class="state-title">正在加载关注记录</text
      ></view
    >
    <view v-else-if="error" class="state-card"
      ><text class="state-title">暂时无法加载</text
      ><text class="hint">{{ error }}</text
      ><button class="secondary-btn" @tap="load">重新加载</button></view
    >
    <view v-else-if="!list.length" class="state-card"
      ><text class="state-title">暂无课程关注记录</text
      ></view
    >
    <block v-else>
      <view
        v-for="item in list"
        :key="item.memberId + '-' + item.courseId"
        class="interest-card"
      >
        <view class="member-row"
          ><view class="initial">{{
            (item.memberName || "会员").slice(0, 1)
          }}</view
          ><view class="member-main"
            ><text class="member-name">{{ item.memberName || "会员" }}</text
            ></view
          ></view
        >
        <view class="course-name">{{ item.courseName || "课程" }}</view>
        <view class="metrics"
          ><view class="metric"
            ><text class="metric-value">{{
              durationText(item.activeSeconds)
            }}</text
            ><text class="metric-label">有效浏览时长</text></view
          ><view class="metric"
            ><text class="metric-value"
              >{{ item.viewCount || 0 }} <text class="unit">次</text></text
            ><text class="metric-label">访问记录</text></view
          ></view
        >
        <view class="last-view"
          >最近浏览 · {{ formatDateTime(item.lastViewedAt) }}</view
        >
      </view>
      <view class="end-label">显示最近 {{ list.length }} 条课程关注记录</view>
    </block>
    <T1Nav role="coach" current="student" />
  </view>
</template>
<script>
import { request } from "../../../utils/request";
import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return { list: [], error: "", loading: false };
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      if (this.loading) return;
      this.loading = true;
      this.error = "";
      try {
        const result = await request({
          url: "/fitness/coachApp/interest/list",
          auth: "coach",
          data: { pageNum: 1, pageSize: 100 },
        });
        this.list = Array.isArray(result) ? result : result?.rows || [];
      } catch (e) {
        this.error = e.message || "请检查网络后重试";
      } finally {
        this.loading = false;
      }
    },
    goStudents() {
      uni.navigateTo({ url: "/pages/coach/student/index" });
    },
    durationText(value) {
      const seconds = Math.max(0, Number(value) || 0);
      return seconds < 60
        ? `${seconds} 秒`
        : `${Math.round(seconds / 6) / 10} 分钟`;
    },
    formatDateTime(value) {
      return value
        ? String(value).replace("T", " ").slice(0, 16)
        : "时间未提供";
    },
  },
};
</script>
<style scoped lang="scss">
.interest-page {
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 32rpx 32rpx calc(160rpx + env(safe-area-inset-bottom));
  background: var(--t1-bg);
  color: var(--t1-text);
}
button {
  padding: 0;
  margin: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  line-height: 1.5;
  border-radius: 0;
}
button::after {
  border: 0;
}
.intro-card {
  padding: 28rpx;
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
  border-radius: 20rpx;
}
.hint {
  display: block;
  color: var(--t1-muted);
  font-size: 24rpx;
  line-height: 1.8;
  margin-top: 12rpx;
}
.text-btn {
  display: flex;
  align-items: center;
  color: var(--t1-accent);
  min-height: 88rpx;
  font-size: 26rpx;
  text-align: left;
  margin-top: 8rpx;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
  min-height: 96rpx;
  font-size: 29rpx;
  font-weight: 600;
}
.refresh-btn {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  padding: 0 8rpx 0 24rpx;
  color: var(--t1-muted);
  font-size: 24rpx;
}
.interest-card {
  padding: 28rpx;
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}
.member-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
}
.initial {
  width: 72rpx;
  height: 72rpx;
  border: 1rpx solid var(--t1-border);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--t1-accent);
  font-size: 30rpx;
  flex-shrink: 0;
}
.member-name {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.course-name {
  margin: 28rpx 0 22rpx;
  font-size: 32rpx;
  line-height: 1.5;
  font-weight: 650;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-top: 1rpx solid var(--t1-border);
  padding-top: 22rpx;
  gap: 20rpx;
}
.metric + .metric {
  border-left: 1rpx solid var(--t1-border);
  padding-left: 26rpx;
}
.metric-value {
  display: block;
  color: var(--t1-accent);
  font-size: 32rpx;
  font-weight: 600;
}
.unit {
  font-size: 23rpx;
  font-weight: 400;
}
.metric-label {
  display: block;
  font-size: 22rpx;
  color: var(--t1-muted);
  margin-top: 8rpx;
}
.last-view {
  font-size: 22rpx;
  color: var(--t1-muted);
  margin-top: 24rpx;
}
.state-card {
  border: 1rpx solid var(--t1-border);
  background: var(--t1-surface);
  border-radius: 20rpx;
  text-align: center;
  padding: 48rpx 28rpx;
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.secondary-btn {
  margin-top: 24rpx;
  min-height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  background: var(--t1-inset);
  border-radius: 12rpx;
  font-size: 26rpx;
}
.end-label {
  color: var(--t1-muted);
  font-size: 22rpx;
  text-align: center;
  padding: 20rpx;
}
</style>
