<template>
  <view class="schedule-page">
    <view class="page-head"
      ><text class="eyebrow">教学安排</text
      ><view class="page-title">我的排课</view
      ><view class="page-desc">查看授课时间与预约情况</view></view
    >
    <view class="filter-row"
      ><button
        v-for="item in tabs"
        :key="item.value"
        class="filter-chip"
        :class="{ active: activeTab === item.value }"
        @tap="changeTab(item.value)"
      >
        {{ item.label }}
      </button></view
    >
    <view class="list-head"
      ><text>{{ activeLabel }}课程</text
      ><text class="muted">{{ loading ? "加载中" : total + " 节" }}</text></view
    >
    <view v-if="loading" class="state-card"
      ><text class="state-title">正在加载排课</text
      ><text class="muted">请稍候</text></view
    >
    <view v-else-if="errorText && !list.length" class="state-card"
      ><text class="state-title">排课加载失败</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="loadData()">重新加载</button></view
    >
    <block v-else>
      <view v-if="!list.length" class="state-card"
        ><text class="empty-mark">—</text
        ><text class="state-title">{{ activeLabel }}暂无排课</text
        ><text class="muted">可切换日期查看其他授课安排。</text></view
      >
      <view v-for="item in list" :key="item.scheduleId" class="schedule-card">
        <view class="date-line"
          ><text>{{ formatDate(item.startTime || item.scheduleDate) }}</text
          ><text
            class="status-tag"
            :class="{
              muted: ['cancelled', 'closed', 'finished'].includes(
                item.scheduleStatus,
              ),
            }"
            >{{ statusText(item.scheduleStatus) }}</text
          ></view
        >
        <view class="lesson-main"
          ><view class="time-block"
            ><text class="start-time">{{ formatTime(item.startTime) }}</text
            ><text class="end-time"
              >至 {{ formatTime(item.endTime) }}</text
            ></view
          ><view class="course-main"
            ><view class="card-title">{{ item.courseName || "课程待定" }}</view
            ><view class="card-meta">{{ item.storeName || "门店待定" }}</view
            ><text class="type-tag">{{
              courseTypeText(item.courseType)
            }}</text></view
          ></view
        >
        <view class="attendance"
          ><view
            ><text class="count"
              >{{ item.reservedCount || 0
              }}<text class="capacity">
                / {{ item.capacity == null ? "—" : item.capacity }}</text
              ></text
            ><text class="count-label">预约 / 人数上限</text></view
          ><view
            ><text class="count">{{ item.signedCount || 0 }}</text
            ><text class="count-label">已确认到课</text></view
          ><button class="checkin-link" @tap="goCheckin">
            签到处理 ›
          </button></view
        >
      </view>
      <text v-if="errorText && list.length" class="inline-error">{{
        errorText
      }}</text>
      <button
        v-if="list.length < total"
        class="secondary-btn load-more"
        :disabled="loadingMore"
        @tap="loadData(true)"
      >
        {{ loadingMore ? "加载中…" : "加载更多课程" }}
      </button>
      <text v-else-if="list.length" class="end-label">已显示全部课程</text>
    </block>
    <T1Nav role="coach" current="schedule" />
  </view>
</template>
<script>
import { coachScheduleList } from "../../../api/auth";
import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return {
      activeTab: "today",
      tabs: [
        { label: "今日", value: "today" },
        { label: "明日", value: "tomorrow" },
        { label: "本周", value: "week" },
        { label: "全部", value: "all" },
      ],
      list: [],
      total: 0,
      pageNum: 1,
      loading: false,
      loadingMore: false,
      errorText: "",
      requestId: 0,
    };
  },
  computed: {
    activeLabel() {
      return (
        this.tabs.find((item) => item.value === this.activeTab)?.label || ""
      );
    },
  },
  onShow() {
    this.loadData();
  },
  methods: {
    changeTab(tab) {
      if (this.activeTab === tab) return;
      this.activeTab = tab;
      this.loadData();
    },
    async loadData(append = false) {
      if (append && (this.loading || this.loadingMore)) return;
      const id = ++this.requestId;
      const page = append ? this.pageNum + 1 : 1;
      if (append) this.loadingMore = true;
      else {
        this.loading = true;
        this.list = [];
        this.total = 0;
      }
      this.errorText = "";
      try {
        const table =
          (await coachScheduleList({
            scope: this.activeTab,
            pageNum: page,
            pageSize: 30,
          })) || {};
        if (id !== this.requestId) return;
        this.list = append
          ? this.list.concat(table.rows || [])
          : table.rows || [];
        this.total = Number(table.total || this.list.length);
        this.pageNum = page;
      } catch (e) {
        if (id === this.requestId)
          this.errorText = e.message || "请检查网络后重试";
      } finally {
        if (id === this.requestId) {
          this.loading = false;
          this.loadingMore = false;
        }
      }
    },
    goCheckin() {
      uni.navigateTo({ url: "/pages/coach/checkin/index" });
    },
    statusText(v) {
      return (
        {
          draft: "草稿",
          open: "可预约",
          closed: "已关闭",
          cancelled: "已取消",
          finished: "已结束",
        }[v] || "状态待确认"
      );
    },
    courseTypeText(v) {
      return (
        {
          group: "团体课",
          private: "私教课",
          small_group: "私教小班",
          small: "私教小班",
          experience: "体验课",
        }[v] || "课程"
      );
    },
    formatDate(value) {
      if (!value) return "日期待定";
      const text = String(value).slice(0, 10);
      return text.replace(/-/g, " / ");
    },
    formatTime(value) {
      if (!value) return "--:--";
      const match = String(value).match(/(?:T|\s)(\d{2}:\d{2})/);
      if (match) return match[1];
      const d = new Date(value);
      return Number.isNaN(d.getTime())
        ? "--:--"
        : `${d.getHours()}`.padStart(2, "0") +
            ":" +
            `${d.getMinutes()}`.padStart(2, "0");
    },
  },
};
</script>
<style scoped lang="scss">
.schedule-page {
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
  margin: 0;
  padding: 0;
  color: inherit;
  font: inherit;
  line-height: 1.5;
  background: transparent;
  border-radius: 0;
}
button::after {
  border: 0;
}
.page-head {
  padding: 8rpx 0 32rpx;
}
.eyebrow {
  font-size: 22rpx;
  color: #ff7a35;
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
.filter-row {
  display: flex;
  padding: 6rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 16rpx;
  gap: 6rpx;
}
.filter-chip {
  flex: 1;
  min-height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #a2a49c;
  border-radius: 12rpx;
}
.filter-chip.active {
  background: #ff7a35;
  color: #111211;
  font-weight: 650;
}
.list-head {
  display: flex;
  justify-content: space-between;
  margin: 32rpx 0 20rpx;
  font-size: 27rpx;
}
.muted {
  color: #a2a49c;
  font-size: 24rpx;
  line-height: 1.7;
}
.schedule-card {
  padding: 24rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}
.date-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #a2a49c;
  font-size: 24rpx;
  gap: 16rpx;
}
.status-tag {
  padding: 6rpx 12rpx;
  border: 1rpx solid #30322f;
  border-radius: 8rpx;
  font-size: 21rpx;
  color: #ff7a35;
}
.lesson-main {
  display: flex;
  align-items: flex-start;
  gap: 26rpx;
  padding: 28rpx 0;
}
.time-block {
  flex-shrink: 0;
  min-width: 120rpx;
  border-right: 1rpx solid #30322f;
  padding-right: 24rpx;
}
.start-time {
  display: block;
  font-size: 36rpx;
  font-weight: 650;
}
.end-time {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #a2a49c;
}
.course-main {
  flex: 1;
  min-width: 0;
}
.card-title {
  font-size: 31rpx;
  font-weight: 650;
  line-height: 1.5;
}
.card-meta {
  color: #a2a49c;
  font-size: 24rpx;
  margin: 8rpx 0 14rpx;
}
.type-tag {
  color: #c4c6bd;
  font-size: 21rpx;
  padding: 4rpx 10rpx;
  background: #282a26;
  border-radius: 6rpx;
}
.attendance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid #30322f;
}
.count {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}
.capacity {
  font-size: 24rpx;
  color: #a2a49c;
  font-weight: 400;
}
.count-label {
  display: block;
  font-size: 21rpx;
  color: #a2a49c;
  margin-top: 4rpx;
}
.checkin-link {
  min-height: 88rpx;
  display: flex;
  align-items: center;
  color: #ff7a35;
  font-size: 24rpx;
  flex-shrink: 0;
}
.state-card {
  padding: 54rpx 28rpx;
  text-align: center;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  background: #1c1d1b;
}
.state-card .muted {
  display: block;
  margin-top: 12rpx;
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.empty-mark {
  display: block;
  color: #ff7a35;
  font-size: 50rpx;
  margin-bottom: 14rpx;
}
.secondary-btn {
  min-height: 88rpx;
  line-height: 88rpx;
  background: #30322f;
  border-radius: 12rpx;
  font-size: 26rpx;
  padding: 0 28rpx;
  margin-top: 24rpx;
}
.secondary-btn[disabled] {
  opacity: 0.5;
}
.inline-error {
  display: block;
  text-align: center;
  color: #ff7a35;
  font-size: 24rpx;
  padding: 12rpx;
}
.end-label {
  display: block;
  text-align: center;
  color: #a2a49c;
  font-size: 22rpx;
  padding: 20rpx 0;
}
</style>
