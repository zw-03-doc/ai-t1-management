<template>
  <view class="interest-page">
    <view class="page-head"
      ><text class="eyebrow">会员沟通</text
      ><view class="page-title">课程兴趣</view
      ><view class="page-desc">了解会员最近关注的训练方向</view></view
    >
    <view class="intro-card"
      ><view class="intro-title">从关注到沟通</view
      ><view class="hint"
        >近 90
        天的有效浏览可作为沟通参考，不代表购买意向。为会员定制训练前，需先建立有效的教练绑定。</view
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
      ><text class="hint">请稍候</text></view
    >
    <view v-else-if="error" class="state-card"
      ><text class="state-title">暂时无法加载</text
      ><text class="hint">{{ error }}</text
      ><button class="secondary-btn" @tap="load">重新加载</button></view
    >
    <view v-else-if="!list.length" class="state-card"
      ><text class="empty-mark">—</text
      ><text class="state-title">暂无课程关注记录</text
      ><text class="hint">与你关联的课程浏览记录会显示在这里。</text></view
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
            ><text class="member-label">课程关注</text></view
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
  background: #111211;
  color: #f6f4ef;
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
.page-head {
  padding: 8rpx 0 32rpx;
}
.eyebrow {
  color: #ff7a35;
  font-size: 22rpx;
}
.page-title {
  font-size: 46rpx;
  font-weight: 700;
  margin: 12rpx 0;
}
.page-desc {
  font-size: 26rpx;
  color: #a2a49c;
}
.intro-card {
  padding: 28rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
}
.intro-title {
  font-size: 28rpx;
  font-weight: 600;
}
.hint {
  display: block;
  color: #a2a49c;
  font-size: 24rpx;
  line-height: 1.8;
  margin-top: 12rpx;
}
.text-btn {
  display: flex;
  align-items: center;
  color: #ff7a35;
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
  color: #a2a49c;
  font-size: 24rpx;
}
.interest-card {
  padding: 28rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
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
  border: 1rpx solid #30322f;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff7a35;
  font-size: 30rpx;
  flex-shrink: 0;
}
.member-name {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.member-label {
  display: block;
  color: #a2a49c;
  font-size: 22rpx;
  margin-top: 4rpx;
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
  border-top: 1rpx solid #30322f;
  padding-top: 22rpx;
  gap: 20rpx;
}
.metric + .metric {
  border-left: 1rpx solid #30322f;
  padding-left: 26rpx;
}
.metric-value {
  display: block;
  color: #ff7a35;
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
  color: #a2a49c;
  margin-top: 8rpx;
}
.last-view {
  font-size: 22rpx;
  color: #a2a49c;
  margin-top: 24rpx;
}
.state-card {
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
  text-align: center;
  padding: 48rpx 28rpx;
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.empty-mark {
  color: #ff7a35;
  display: block;
  font-size: 50rpx;
  margin-bottom: 14rpx;
}
.secondary-btn {
  margin-top: 24rpx;
  min-height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  background: #30322f;
  border-radius: 12rpx;
  font-size: 26rpx;
}
.end-label {
  color: #a2a49c;
  font-size: 22rpx;
  text-align: center;
  padding: 20rpx;
}
</style>
