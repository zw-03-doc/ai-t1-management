<template>
  <view class="student-page">
    <view class="page-head"
      ><text class="eyebrow">学员管理</text
      ><view class="page-title">我的学员</view
      ><view class="page-desc">查看到课记录，持续跟进训练</view></view
    >
    <view class="search-row"
      ><input
        v-model.trim="keyword"
        class="search-input"
        placeholder="搜索姓名、昵称或手机号"
        placeholder-style="color:#74786f"
        confirm-type="search"
        @confirm="loadList()"
      /><button class="search-btn" :disabled="loading" @tap="loadList()">
        搜索
      </button></view
    >
    <view class="section-head"
      ><text class="section-title">绑定学员</text
      ><text class="count-label">{{
        loading ? "加载中" : "当前显示 " + students.length + " 位"
      }}</text></view
    >
    <view v-if="loading" class="state-card"
      ><text class="state-title">正在加载学员</text
      ><text class="muted">请稍候</text></view
    >
    <view v-else-if="errorText && !list.length" class="state-card"
      ><text class="state-title">学员加载失败</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="loadList()">重新加载</button></view
    >
    <block v-else>
      <view v-if="!students.length" class="state-card"
        ><text class="empty-mark">—</text
        ><text class="state-title">{{
          keyword ? "没有找到匹配的学员" : "暂无绑定学员"
        }}</text
        ><text class="muted">{{
          keyword
            ? "试试其他姓名或手机号。"
            : "会员与教练绑定后，会显示在这里。"
        }}</text
        ><button v-if="keyword" class="secondary-btn" @tap="clearSearch">
          清空搜索
        </button></view
      >
      <view v-for="item in students" :key="item.memberId" class="student-card">
        <view class="student-head"
          ><view class="initial">{{
            (item.realName || item.nickname || "会员").slice(0, 1)
          }}</view
          ><view class="student-main"
            ><text class="student-name">{{
              item.realName || item.nickname || "会员"
            }}</text
            ><text class="student-mobile">{{
              item.mobile || "未填写手机号"
            }}</text></view
          ></view
        >
        <view class="upcoming"
          ><text class="upcoming-label">下次课程</text
          ><text class="upcoming-title">{{
            item.nextCourseName || "暂无预约课程"
          }}</text
          ><text v-if="item.nextClassTime" class="muted">{{
            formatDateTime(item.nextClassTime)
          }}</text></view
        >
        <view class="visit-row"
          ><view
            ><text class="small-label">最近到店</text
            ><text class="visit-value">{{
              formatDateTime(item.lastVisitTime)
            }}</text></view
          ><view class="align-right"
            ><text class="small-label">累计核销</text
            ><text class="visit-value"
              >{{ item.totalCheckinCount || 0 }} 次</text
            ></view
          ></view
        >
        <view class="cards"
          ><view
            v-for="card in item.cards"
            :key="card.cardId || 'none'"
            class="card-row"
            ><view class="card-name">{{ card.cardName || "绑定卡项" }}</view
            ><view class="card-detail"
              ><text>剩余 {{ remainTimesText(card.remainTimes) }} 课次</text
              ><text v-if="Number(card.balanceAmount) > 0"
                >储值余额 ¥{{ Number(card.balanceAmount).toFixed(2) }}</text
              ></view
            ></view
          ></view
        >
        <button class="plan-btn" @tap="goTraining(item)">
          <text>定制 / 查看训练计划</text><text>›</text>
        </button>
      </view>
      <text v-if="errorText && list.length" class="inline-error">{{
        errorText
      }}</text>
      <button
        v-if="list.length < total"
        class="secondary-btn load-more"
        :disabled="loadingMore"
        @tap="loadList(true)"
      >
        {{ loadingMore ? "加载中…" : "加载更多学员" }}
      </button>
      <view v-if="students.length" class="end-label"
        >训练计划仅向有效绑定的学员开放</view
      >
    </block>
    <T1Nav role="coach" current="student" />
  </view>
</template>
<script>
import { coachStudentList } from "../../../api/auth";
import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return {
      keyword: "",
      list: [],
      total: 0,
      pageNum: 1,
      loading: false,
      loadingMore: false,
      errorText: "",
    };
  },
  computed: {
    students() {
      const grouped = new Map();
      this.list.forEach((item) => {
        const key = String(item.memberId);
        if (!grouped.has(key)) grouped.set(key, { ...item, cards: [] });
        const student = grouped.get(key);
        if (
          !student.cards.some(
            (card) => String(card.cardId) === String(item.cardId),
          )
        )
          student.cards.push(item);
      });
      return Array.from(grouped.values());
    },
  },
  onShow() {
    this.loadList();
  },
  methods: {
    async loadList(append = false) {
      if (this.loading || this.loadingMore) return;
      const page = append ? this.pageNum + 1 : 1;
      if (append) this.loadingMore = true;
      else {
        this.loading = true;
        this.list = [];
      }
      this.errorText = "";
      try {
        const table =
          (await coachStudentList({
            keyword: this.keyword,
            pageNum: page,
            pageSize: 50,
          })) || {};
        this.list = append
          ? this.list.concat(table.rows || [])
          : table.rows || [];
        this.total = Number(table.total || this.list.length);
        this.pageNum = page;
      } catch (e) {
        this.errorText = e.message || "请检查网络后重试";
      } finally {
        this.loading = false;
        this.loadingMore = false;
      }
    },
    clearSearch() {
      this.keyword = "";
      this.loadList();
    },
    goTraining(item) {
      uni.navigateTo({
        url:
          "/pages/training/index?role=coach&memberId=" +
          encodeURIComponent(item.memberId),
      });
    },
    formatDateTime(value) {
      return value ? String(value).replace("T", " ").slice(0, 16) : "暂无记录";
    },
    remainTimesText(value) {
      return value === 0 || value ? value : "—";
    },
  },
};
</script>
<style scoped lang="scss">
.student-page {
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
  font: inherit;
  line-height: 1.5;
  color: inherit;
  background: transparent;
  border-radius: 0;
}
button::after {
  border: 0;
}
button[disabled] {
  opacity: 0.5;
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
.search-row {
  display: flex;
  gap: 12rpx;
}
.search-input {
  flex: 1;
  min-width: 0;
  height: 90rpx;
  box-sizing: border-box;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 14rpx;
  padding: 0 22rpx;
  font-size: 25rpx;
  color: #f6f4ef;
}
.search-btn {
  min-width: 116rpx;
  min-height: 90rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #111211;
  background: #ff7a35;
  border-radius: 14rpx;
  font-size: 26rpx;
  font-weight: 600;
}
.section-head {
  min-height: 100rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
}
.count-label {
  color: #a2a49c;
  font-size: 23rpx;
}
.student-card {
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  padding: 28rpx;
}
.student-head {
  display: flex;
  gap: 20rpx;
  align-items: center;
}
.initial {
  height: 84rpx;
  width: 84rpx;
  flex-shrink: 0;
  border-radius: 18rpx;
  border: 1rpx solid #30322f;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff7a35;
  font-size: 32rpx;
}
.student-main {
  flex: 1;
  min-width: 0;
}
.student-name {
  display: block;
  font-size: 34rpx;
  font-weight: 650;
}
.student-mobile {
  display: block;
  color: #a2a49c;
  font-size: 24rpx;
  margin-top: 8rpx;
}
.upcoming {
  padding: 24rpx;
  border-radius: 14rpx;
  background: #252722;
  margin-top: 26rpx;
}
.upcoming-label {
  display: block;
  color: #a2a49c;
  font-size: 22rpx;
}
.upcoming-title {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.muted {
  color: #a2a49c;
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  margin-top: 10rpx;
}
.visit-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 24rpx 0;
}
.small-label {
  display: block;
  color: #a2a49c;
  font-size: 22rpx;
}
.visit-value {
  display: block;
  font-size: 25rpx;
  margin-top: 10rpx;
}
.align-right {
  text-align: right;
}
.cards {
  border-top: 1rpx solid #30322f;
}
.card-row {
  padding: 20rpx 0;
}
.card-row + .card-row {
  border-top: 1rpx solid #30322f;
}
.card-name {
  color: #c4c6bd;
  font-size: 24rpx;
}
.card-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 24rpx;
  color: #a2a49c;
  font-size: 22rpx;
  margin-top: 8rpx;
}
.plan-btn {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  padding: 0 22rpx;
  box-sizing: border-box;
  border: 1rpx solid #62422e;
  color: #ff7a35;
  border-radius: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  margin-top: 6rpx;
}
.state-card {
  text-align: center;
  padding: 48rpx 28rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
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
  margin-top: 24rpx;
  min-height: 88rpx;
  line-height: 88rpx;
  border-radius: 12rpx;
  padding: 0 24rpx;
  color: #f6f4ef;
  background: #30322f;
  font-size: 26rpx;
}
.end-label,
.inline-error {
  display: block;
  text-align: center;
  color: #a2a49c;
  font-size: 22rpx;
  line-height: 1.7;
  padding: 20rpx 0;
}
.inline-error {
  color: #ff7a35;
}
</style>
