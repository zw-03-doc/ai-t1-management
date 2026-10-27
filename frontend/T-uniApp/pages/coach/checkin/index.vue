<template>
  <view class="checkin-page">
    <view class="page-head"
      ><text class="eyebrow">到课管理</text
      ><view class="page-title">签到与核销</view
      ><view class="page-desc">确认会员到场，完成本次课程</view></view
    >
    <view class="stats-grid"
      ><view class="stat"
        ><text class="stat-value">{{ loaded ? pendingCount : "—" }}</text
        ><text class="stat-label">待处理</text></view
      ><view class="stat"
        ><text class="stat-value">{{ loaded ? confirmedCount : "—" }}</text
        ><text class="stat-label">已完成</text></view
      ><view class="stat"
        ><text class="stat-value">{{ loaded ? exceptionCount : "—" }}</text
        ><text class="stat-label">异常记录</text></view
      ></view
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
    <view v-if="loading" class="state-card"
      ><text class="state-title">正在加载签到记录</text
      ><text class="muted">请稍候</text></view
    >
    <view v-else-if="errorText" class="state-card"
      ><text class="state-title">签到记录加载失败</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="refreshAll">重新加载</button></view
    >
    <block v-else>
      <view v-if="!displayList.length" class="state-card"
        ><text class="empty-mark">—</text
        ><text class="state-title">暂无{{ tabLabel(activeTab) }}记录</text
        ><text class="muted">会员预约和签到后，可在这里处理。</text></view
      >
      <view
        v-for="item in displayList"
        :key="item.checkinId || item.reservationId"
        class="checkin-card"
      >
        <view class="card-head"
          ><text class="member-name">{{ item.memberName || "会员" }}</text
          ><text class="status-tag" :class="activeTab">{{
            itemStatus(item)
          }}</text></view
        >
        <view class="course-name">{{ item.courseName || "课程待定" }}</view>
        <view class="course-meta"
          >{{ courseTypeText(item.courseType) }} ·
          {{ item.storeName || "门店待定" }}</view
        >
        <view class="details"
          ><view class="detail-row"
            ><text class="label">上课时间</text
            ><text class="detail-value">{{
              formatDateTime(item.startTime || item.scheduleDate)
            }}</text></view
          ><view v-if="item.checkinTime" class="detail-row"
            ><text class="label">签到时间</text
            ><text class="detail-value">{{
              formatDateTime(item.checkinTime)
            }}</text></view
          ><view class="detail-row"
            ><text class="label">使用卡项</text
            ><text class="detail-value">{{
              item.cardName || "无卡到课"
            }}</text></view
          ><view v-if="item.remark" class="detail-row"
            ><text class="label">备注</text
            ><text class="detail-value">{{ item.remark }}</text></view
          ></view
        >
        <view v-if="activeTab === 'confirmed'" class="consume-note">{{
          consumeText(item)
        }}</view>
        <view v-if="activeTab === 'pending'" class="action-row"
          ><button
            class="primary-btn"
            :disabled="!!actioningReservationId"
            @tap="item.checkinId ? confirmItem(item) : proxyItem(item)"
          >
            {{
              actioningReservationId === item.reservationId
                ? "处理中…"
                : item.checkinId
                  ? "确认核销"
                  : "代签到"
            }}</button
          ><button
            v-if="item.checkinId"
            class="secondary-btn"
            :disabled="!!actioningReservationId"
            @tap="markException(item)"
          >
            标记异常
          </button></view
        >
        <text
          v-if="
            activeTab === 'pending' &&
            item.checkinId &&
            item.planType === 'stored_card'
          "
          class="action-hint"
          >储值卡核销需输入本次确认金额。</text
        >
      </view>
      <button
        v-if="displayList.length < totalMap[activeTab]"
        class="secondary-btn load-more"
        :disabled="loadingMore"
        @tap="loadMore"
      >
        {{ loadingMore ? "加载中…" : "加载更多记录" }}
      </button>
    </block>
    <T1Nav role="coach" current="checkin" />
  </view>
</template>
<script>
import {
  coachCheckinList,
  coachConfirmCheckin,
  coachExceptionCheckin,
  coachProxyCheckin,
} from "../../../api/auth";

import T1Nav from "../../../components/T1Nav.vue";
export default {
  components: { T1Nav },
  data() {
    return {
      activeTab: "pending",
      tabs: [
        { label: "待处理", value: "pending" },
        { label: "已完成", value: "confirmed" },
        { label: "异常", value: "exception" },
      ],
      dataMap: {
        pending: [],
        confirmed: [],
        exception: [],
      },
      totalMap: { pending: 0, confirmed: 0, exception: 0 },
      pageMap: { pending: 1, confirmed: 1, exception: 1 },
      loadingMore: false,
      loaded: false,
      loading: false,
      errorText: "",
      actioningReservationId: null,
    };
  },
  computed: {
    displayList() {
      return this.dataMap[this.activeTab] || [];
    },
    pendingCount() {
      return this.totalMap.pending;
    },
    confirmedCount() {
      return this.totalMap.confirmed;
    },
    exceptionCount() {
      return this.totalMap.exception;
    },
  },
  onShow() {
    this.refreshAll();
  },
  methods: {
    async proxyItem(item) {
      if (!item || !item.reservationId || this.actioningReservationId) return;
      this.actioningReservationId = item.reservationId;
      try {
        const result = await new Promise((resolve) =>
          uni.showModal({
            title: "为会员代签到",
            content: "请确认会员已到场，并填写代签到原因。该操作会留存记录。",
            editable: true,
            placeholderText: "例如：会员忘带手机，已到场",
            success: resolve,
            fail: () => resolve({ confirm: false }),
          }),
        );
        if (!result.confirm) return;
        const remark = (result.content || "").trim();
        if (!remark) {
          uni.showToast({ title: "请填写代签到原因", icon: "none" });
          return;
        }
        let source = "mini";
        // #ifdef H5
        source = "web";
        // #endif
        await coachProxyCheckin({
          reservationId: item.reservationId,
          remark,
          source,
        });
        uni.showToast({ title: "已签到，请确认到课", icon: "none" });
        await this.refreshAll();
      } catch (e) {
        uni.showToast({ title: e.message || "代签到失败", icon: "none" });
      } finally {
        this.actioningReservationId = null;
      }
    },
    itemStatus(item) {
      return this.activeTab === "pending"
        ? item.checkinId
          ? "已签到 · 待核销"
          : "尚未签到"
        : this.tabLabel(this.activeTab);
    },
    courseTypeText(value) {
      return (
        {
          group: "团体课",
          private: "私教课",
          small: "私教小班",
          experience: "体验课",
        }[value] || "课程"
      );
    },
    consumeText(item) {
      if (Number(item.consumeAmount) > 0)
        return `核销金额 ¥${Number(item.consumeAmount).toFixed(2)}`;
      if (Number(item.consumeTimes) > 0)
        return `已核销 ${item.consumeTimes} 课次`;
      return "已确认到课";
    },
    async loadMore() {
      if (this.loading || this.loadingMore) return;
      const status = this.activeTab;
      this.loadingMore = true;
      try {
        const page = this.pageMap[status] + 1;
        const table =
          (await coachCheckinList({ status, pageNum: page, pageSize: 30 })) ||
          {};
        this.dataMap[status] = this.dataMap[status].concat(table.rows || []);
        this.pageMap[status] = page;
        this.totalMap[status] = Number(
          table.total || this.dataMap[status].length,
        );
      } catch (e) {
        uni.showToast({ title: e.message || "加载失败，请重试", icon: "none" });
      } finally {
        this.loadingMore = false;
      }
    },
    tabLabel(value) {
      const map = { pending: "待处理", confirmed: "已完成", exception: "异常" };
      return map[value] || "状态";
    },
    async refreshAll() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        const [pending, confirmed, exception] = await Promise.all([
          coachCheckinList({ status: "pending", pageNum: 1, pageSize: 30 }),
          coachCheckinList({ status: "confirmed", pageNum: 1, pageSize: 30 }),
          coachCheckinList({ status: "exception", pageNum: 1, pageSize: 30 }),
        ]);
        this.dataMap = {
          pending: (pending && pending.rows) || [],
          confirmed: (confirmed && confirmed.rows) || [],
          exception: (exception && exception.rows) || [],
        };
        this.totalMap = {
          pending: Number(pending?.total || this.dataMap.pending.length),
          confirmed: Number(confirmed?.total || this.dataMap.confirmed.length),
          exception: Number(exception?.total || this.dataMap.exception.length),
        };
        this.pageMap = { pending: 1, confirmed: 1, exception: 1 };
        this.loaded = true;
      } catch (error) {
        this.errorText = error.message || "核销列表加载失败";
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    changeTab(tab) {
      if (this.activeTab === tab) {
        return;
      }
      this.activeTab = tab;
    },
    goPage(url) {
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      if (current && `/${current.route}` === url) return;
      uni.redirectTo({ url });
    },
    confirmDialog(content) {
      return new Promise((resolve) => {
        uni.showModal({
          title: "请确认",
          content,
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false),
        });
      });
    },
    async confirmItem(item) {
      if (!item || !item.reservationId || this.actioningReservationId) {
        return;
      }
      let consumeAmount = 0;
      if (item.cardId && item.planType === "stored_card") {
        const input = await new Promise((resolve) =>
          uni.showModal({
            title: "储值卡核销金额（元）",
            editable: true,
            placeholderText: "请输入本次线下确认的金额",
            success: resolve,
            fail: () => resolve({ confirm: false }),
          }),
        );
        if (!input.confirm) return;
        consumeAmount = Number(input.content);
        if (
          !Number.isFinite(consumeAmount) ||
          consumeAmount <= 0 ||
          !/^\d+(\.\d{1,2})?$/.test(input.content.trim())
        ) {
          uni.showToast({
            title: "请输入正确金额，最多两位小数",
            icon: "none",
          });
          return;
        }
      }
      const ok = await this.confirmDialog(
        consumeAmount
          ? `确认从该储值卡核销 ${consumeAmount} 元吗？`
          : item.cardId && !["time_card", "stored_card"].includes(item.planType)
            ? "确认会员已到课，并核销 1 课次？"
            : "确认会员已到课？本次不扣费。",
      );
      if (!ok) {
        return;
      }
      this.actioningReservationId = item.reservationId;
      try {
        uni.showLoading({ title: "核销中..." });
        await coachConfirmCheckin({
          reservationId: item.reservationId,
          consumeTimes:
            item.cardId && !["time_card", "stored_card"].includes(item.planType)
              ? 1
              : 0,
          consumeAmount,
          remark: "教练端确认核销",
        });

        uni.showToast({ title: "核销成功", icon: "success" });
        await this.refreshAll();
      } catch (error) {
        uni.showToast({ title: error.message || "核销失败", icon: "none" });
      } finally {
        this.actioningReservationId = null;
        uni.hideLoading();
      }
    },
    async markException(item) {
      if (!item || !item.reservationId || this.actioningReservationId) {
        return;
      }
      const ok = await this.confirmDialog("确认将该签到标记为异常吗？");
      if (!ok) {
        return;
      }
      this.actioningReservationId = item.reservationId;
      try {
        uni.showLoading({ title: "提交中..." });
        await coachExceptionCheckin({
          reservationId: item.reservationId,
          remark: "教练端标记异常",
        });
        uni.showToast({ title: "已标记异常", icon: "success" });
        await this.refreshAll();
      } catch (error) {
        uni.showToast({ title: error.message || "标记失败", icon: "none" });
      } finally {
        this.actioningReservationId = null;
        uni.hideLoading();
      }
    },
    formatDateTime(value) {
      if (!value) return "待定";
      const match = String(value).match(
        /^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})/,
      );
      if (match)
        return `${Number(match[2])}/${Number(match[3])} ${match[4]}:${match[5]}`;
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return "待定";
      const hh = `${date.getHours()}`.padStart(2, "0");
      const mm = `${date.getMinutes()}`.padStart(2, "0");
      return `${date.getMonth() + 1}/${date.getDate()} ${hh}:${mm}`;
    },
  },
};
</script>
<style scoped lang="scss">
.checkin-page {
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
  color: inherit;
  background: transparent;
  line-height: 1.5;
  border-radius: 0;
}
button::after {
  border: 0;
}
button[disabled] {
  opacity: 0.45;
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
  color: #a2a49c;
  font-size: 26rpx;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}
.stat {
  padding: 24rpx 8rpx;
  text-align: center;
}
.stat + .stat {
  border-left: 1rpx solid #30322f;
}
.stat-value {
  display: block;
  color: #ff7a35;
  font-size: 40rpx;
  font-weight: 650;
}
.stat-label {
  display: block;
  font-size: 23rpx;
  color: #a2a49c;
  margin-top: 4rpx;
}
.filter-row {
  display: flex;
  padding: 6rpx;
  border-radius: 16rpx;
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  gap: 6rpx;
  margin-bottom: 24rpx;
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
.checkin-card {
  padding: 28rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}
.member-name {
  font-size: 32rpx;
  font-weight: 650;
}
.status-tag {
  padding: 8rpx 12rpx;
  border-radius: 8rpx;
  background: #282a26;
  font-size: 21rpx;
  color: #c4c6bd;
  flex-shrink: 0;
}
.status-tag.pending {
  color: #ff7a35;
  background: #30251d;
}
.status-tag.exception {
  color: #f0a48f;
}
.course-name {
  margin-top: 24rpx;
  font-size: 28rpx;
  line-height: 1.5;
}
.course-meta {
  font-size: 23rpx;
  color: #a2a49c;
  margin-top: 8rpx;
}
.details {
  border-top: 1rpx solid #30322f;
  margin-top: 24rpx;
  padding-top: 12rpx;
}
.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 10rpx 0;
  font-size: 24rpx;
  line-height: 1.65;
}
.label {
  color: #a2a49c;
  flex-shrink: 0;
  width: 120rpx;
}
.detail-value {
  flex: 1;
  text-align: right;
  overflow-wrap: anywhere;
}
.action-row {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}
.primary-btn,
.secondary-btn {
  flex: 1;
  min-height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  border-radius: 12rpx;
  font-size: 27rpx;
  font-weight: 600;
}
.primary-btn {
  color: #111211;
  background: #ff7a35;
}
.secondary-btn {
  color: #f6f4ef;
  background: #30322f;
}
.action-hint {
  display: block;
  font-size: 22rpx;
  color: #a2a49c;
  margin-top: 14rpx;
  line-height: 1.6;
}
.consume-note {
  color: #ff7a35;
  font-size: 25rpx;
  border-top: 1rpx solid #30322f;
  padding-top: 20rpx;
  margin-top: 16rpx;
}
.state-card {
  padding: 48rpx 28rpx;
  text-align: center;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  background: #1c1d1b;
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.muted {
  display: block;
  font-size: 24rpx;
  color: #a2a49c;
  margin-top: 12rpx;
  line-height: 1.7;
}
.state-card .secondary-btn {
  margin-top: 24rpx;
}
.empty-mark {
  display: block;
  font-size: 50rpx;
  color: #ff7a35;
  margin-bottom: 14rpx;
}
.load-more {
  width: 100%;
  margin-top: 24rpx;
}
</style>
