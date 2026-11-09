<template>
  <view class="checkin-page">
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
      ></view
    >
    <view v-else-if="errorText" class="state-card"
      ><text class="state-title">签到记录加载失败</text
      ><text class="muted">{{ errorText }}</text
      ><button class="secondary-btn" @tap="refreshAll">重新加载</button></view
    >
    <block v-else>
      <view v-if="!displayList.length" class="state-card"
        ><text class="state-title">暂无{{ tabLabel(activeTab) }}记录</text
        ></view
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
        <button
          v-if="activeTab === 'exception'"
          class="secondary-btn"
          :disabled="!!actioningReservationId"
          @tap="restoreItem(item)"
        >
          恢复为待核销
        </button>
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
  coachRestoreCheckin,
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
    async restoreItem(item) {
      if (this.actioningReservationId) return;
      const result = await new Promise((resolve) =>
        uni.showModal({
          title: "恢复异常签到",
          content: "请说明恢复原因。恢复后需要重新确认核销。",
          editable: true,
          placeholderText: "必填：恢复原因",
          success: resolve,
          fail: () => resolve({ confirm: false }),
        }),
      );
      if (!result.confirm) return;
      const remark = String(result.content || "").trim();
      if (!remark) {
        uni.showToast({ title: "请填写恢复原因", icon: "none" });
        return;
      }
      this.actioningReservationId = item.reservationId;
      try {
        await coachRestoreCheckin(item.checkinId, remark);
        uni.showToast({ title: "已恢复待核销", icon: "success" });
        await this.refreshAll();
      } catch (error) {
        uni.showToast({ title: error.message || "恢复失败", icon: "none" });
      } finally {
        this.actioningReservationId = null;
      }
    },
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
      let consumeTimes = 0;
      if (
        item.cardId &&
        !["time_card", "stored_card"].includes(item.planType)
      ) {
        const input = await new Promise((resolve) =>
          uni.showModal({
            title: `本次课次（剩余${item.remainTimes || 0}次）`,
            editable: true,
            content: "1",
            success: resolve,
            fail: () => resolve({ confirm: false }),
          }),
        );
        if (!input.confirm) return;
        consumeTimes = Number(input.content);
        if (
          !Number.isInteger(consumeTimes) ||
          consumeTimes <= 0 ||
          consumeTimes > Number(item.remainTimes || 0)
        ) {
          uni.showToast({ title: "请填写有效课次", icon: "none" });
          return;
        }
      }
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
          consumeAmount > Number(item.balanceAmount || 0) ||
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
          ? `本次扣款 ¥${consumeAmount.toFixed(2)}，剩余 ¥${(Number(item.balanceAmount || 0) - consumeAmount).toFixed(2)}。确认会员已到课？`
          : item.cardId && !["time_card", "stored_card"].includes(item.planType)
            ? `本次扣 ${consumeTimes} 次，剩余 ${Number(item.remainTimes || 0) - consumeTimes} 次。确认会员已到课？`
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
          consumeTimes,
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
      const result = await new Promise((resolve) =>
        uni.showModal({
          title: "标记签到异常",
          content: "请填写实际异常原因，处理记录会保留。",
          editable: true,
          placeholderText: "必填：异常原因",
          success: resolve,
          fail: () => resolve({ confirm: false }),
        }),
      );
      if (!result.confirm) return;
      const remark = String(result.content || "").trim();
      if (!remark) {
        uni.showToast({ title: "请填写异常原因", icon: "none" });
        return;
      }
      this.actioningReservationId = item.reservationId;
      try {
        uni.showLoading({ title: "提交中..." });
        await coachExceptionCheckin({
          reservationId: item.reservationId,
          remark,
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
  background: var(--t1-bg);
  color: var(--t1-text);
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
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border: 1rpx solid var(--t1-border);
  background: var(--t1-surface);
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}
.stat {
  padding: 24rpx 8rpx;
  text-align: center;
}
.stat + .stat {
  border-left: 1rpx solid var(--t1-border);
}
.stat-value {
  display: block;
  color: var(--t1-accent);
  font-size: 40rpx;
  font-weight: 650;
}
.stat-label {
  display: block;
  font-size: 23rpx;
  color: var(--t1-muted);
  margin-top: 4rpx;
}
.filter-row {
  display: flex;
  padding: 6rpx;
  border-radius: 16rpx;
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
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
  color: var(--t1-muted);
  border-radius: 12rpx;
}
.filter-chip.active {
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-weight: 650;
}
.checkin-card {
  padding: 28rpx;
  border: 1rpx solid var(--t1-border);
  background: var(--t1-surface);
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
  background: var(--t1-inset);
  font-size: 21rpx;
  color: var(--t1-muted);
  flex-shrink: 0;
}
.status-tag.pending {
  color: var(--t1-accent);
  background: var(--t1-tint);
}
.status-tag.exception {
  color: var(--t1-danger);
}
.course-name {
  margin-top: 24rpx;
  font-size: 28rpx;
  line-height: 1.5;
}
.course-meta {
  font-size: 23rpx;
  color: var(--t1-muted);
  margin-top: 8rpx;
}
.details {
  border-top: 1rpx solid var(--t1-border);
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
  color: var(--t1-muted);
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
  color: var(--t1-on-dark);
  background: var(--t1-text);
}
.secondary-btn {
  color: var(--t1-text);
  background: var(--t1-inset);
}
.action-hint {
  display: block;
  font-size: 22rpx;
  color: var(--t1-muted);
  margin-top: 14rpx;
  line-height: 1.6;
}
.consume-note {
  color: var(--t1-accent);
  font-size: 25rpx;
  border-top: 1rpx solid var(--t1-border);
  padding-top: 20rpx;
  margin-top: 16rpx;
}
.state-card {
  padding: 48rpx 28rpx;
  text-align: center;
  border: 1rpx solid var(--t1-border);
  border-radius: 20rpx;
  background: var(--t1-surface);
}
.state-title {
  display: block;
  font-size: 29rpx;
  font-weight: 600;
}
.muted {
  display: block;
  font-size: 24rpx;
  color: var(--t1-muted);
  margin-top: 12rpx;
  line-height: 1.7;
}
.state-card .secondary-btn {
  margin-top: 24rpx;
}
.load-more {
  width: 100%;
  margin-top: 24rpx;
}
</style>
