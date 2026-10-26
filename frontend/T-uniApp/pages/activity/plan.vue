<template>
  <view class="page-wrap">
    <view class="back-link" @tap="goBack">‹ 返回活动详情</view>
    <view class="eyebrow">T1 / MEMBERSHIP</view>
    <view class="page-title">找到适合你的方案</view>
    <view class="page-subtitle"
      >{{ detail.title || "门店会员卡"
      }}<view>了解权益，再与教练线下确认。</view></view
    >
    <scroll-view class="scroll" scroll-x :show-scrollbar="false">
      <view
        class="chip"
        :class="{ active: currentType === '' }"
        @tap="switchType('')"
        >全部</view
      >
      <view
        class="chip"
        :class="{ active: currentType === item.value }"
        v-for="item in planTypeOptions"
        :key="item.value"
        @tap="switchType(item.value)"
        >{{ item.label }}</view
      >
    </scroll-view>
    <view v-if="loading" class="empty">正在加载会员卡方案…</view>
    <view v-else-if="errorText" class="empty error" @tap="loadPlanList"
      ><text class="empty-title">方案暂时没能加载</text>{{ errorText
      }}<view>轻点重试</view></view
    >
    <block v-else>
      <view
        class="card plan-card"
        v-for="item in filteredPlanList"
        :key="item.planId"
      >
        <view class="row"
          ><text class="eyebrow">{{ planTypeText(item.planType) }}</text
          ><text class="tag" v-if="item.discountLabel">{{
            item.discountLabel
          }}</text></view
        >
        <view class="plan-title">{{ item.planName }}</view>
        <view class="price-row"
          ><text class="sale-price">{{ priceText(item.salePrice) }}</text
          ><text
            class="origin-price"
            v-if="
              item.salePrice != null &&
              Number(item.originalPrice) > Number(item.salePrice)
            "
            >¥{{ formatAmount(item.originalPrice) }}</text
          ></view
        >
        <view class="price-note">门店参考价 · 线下咨询</view>
        <view class="benefits">
          <view v-if="item.totalTimes != null" class="benefit"
            ><text class="benefit-number"
              >{{ item.totalTimes }}<text>次</text></text
            ><text class="muted">包含次数</text></view
          >
          <view v-if="item.giftTimes" class="benefit"
            ><text class="benefit-number"
              >+{{ item.giftTimes }}<text>次</text></text
            ><text class="muted">活动赠送</text></view
          >
          <view v-if="item.validDays" class="benefit"
            ><text class="benefit-number"
              >{{ item.validDays }}<text>天</text></text
            ><text class="muted">有效期</text></view
          >
        </view>
        <view class="rule-note">{{
          item.ruleNote || "使用门店、适用课程及完整权益，请咨询门店。"
        }}</view>
        <button class="btn ghost" @tap="goContact">咨询这项方案 →</button>
      </view>
      <view v-if="!filteredPlanList.length" class="empty"
        ><text class="empty-title">暂时没有这类方案</text
        >换个类型看看，或直接联系门店了解。</view
      >
    </block>
  </view>
</template>
<script>
import {
  memberActivityCardPlanList,
  memberActivityDetail,
} from "../../api/auth";

export default {
  data() {
    return {
      activityId: undefined,
      detail: {},
      planList: [],
      loading: false,
      errorText: "",
      currentType: "",
      planTypeOptions: [
        { label: "体验卡", value: "experience" },
        { label: "周卡", value: "week_card" },
        { label: "月卡", value: "month_card" },
        { label: "季卡", value: "season_card" },
        { label: "年卡", value: "year_card" },
        { label: "团课卡", value: "group_card" },
        { label: "时长卡", value: "time_card" },
        { label: "次卡", value: "count_card" },
        { label: "储值卡", value: "stored_card" },
      ],
    };
  },
  computed: {
    filteredPlanList() {
      if (!this.currentType) {
        return this.planList;
      }
      return this.planList.filter((item) => item.planType === this.currentType);
    },
  },
  onLoad(options) {
    this.activityId = options && options.id;
    this.loadPlanList();
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => uni.redirectTo({ url: "/pages/activity/index" }),
      });
    },
    priceText(value) {
      return value === null || value === undefined || value === ""
        ? "价格待咨询"
        : "¥" + this.formatAmount(value);
    },

    async loadPlanList() {
      if (!this.activityId) {
        this.errorText = "活动参数缺失";
        return;
      }
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        const [detail, plans] = await Promise.all([
          memberActivityDetail(this.activityId),
          memberActivityCardPlanList(this.activityId),
        ]);
        this.detail = detail || {};
        this.planList = plans || [];
      } catch (error) {
        this.errorText = error.message || "活动卡项加载失败";
        this.planList = [];
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    switchType(type) {
      if (this.currentType === type) {
        return;
      }
      this.currentType = type;
    },
    goContact() {
      const phone = this.detail.storeContactPhone;
      if (!phone) {
        uni.showToast({ title: "当前门店暂无联系电话", icon: "none" });
        return;
      }
      uni.makePhoneCall({ phoneNumber: phone });
    },
    planTypeText(value) {
      const hit = this.planTypeOptions.find((item) => item.value === value);
      return hit ? hit.label : value || "卡项";
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2);
    },
  },
};
</script>
<style scoped>
.page-wrap {
  min-height: 100vh;
  box-sizing: border-box;
  background: #111211;
  color: #f6f4ef;
  padding: 32rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  max-width: 960px;
  margin: auto;
}
.with-nav {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}
.eyebrow {
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #ff7a35;
  font-weight: 700;
}
.page-title {
  font-size: 52rpx;
  line-height: 1.25;
  font-weight: 700;
  margin-top: 12rpx;
  letter-spacing: -1rpx;
}
.page-subtitle,
.muted {
  font-size: 25rpx;
  line-height: 1.7;
  color: #a2a49c;
}
.page-subtitle {
  margin-top: 14rpx;
}
.panel,
.card {
  background: #1c1d1b;
  border: 1rpx solid #30322f;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-top: 24rpx;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
}
.tag {
  font-size: 21rpx;
  line-height: 1.5;
  padding: 8rpx 14rpx;
  background: #30271f;
  color: #ff7a35;
  border-radius: 8rpx;
  white-space: nowrap;
  flex-shrink: 0;
}
.btn {
  margin: 0;
  min-height: 88rpx;
  line-height: 88rpx;
  padding: 0 28rpx;
  border-radius: 14rpx;
  background: #ff7a35;
  color: #111211;
  font-size: 28rpx;
  font-weight: 700;
}
.btn::after {
  border: 0;
}
.btn.ghost {
  background: transparent;
  border: 1rpx solid #42453f;
  color: #f6f4ef;
}
.btn[disabled] {
  background: #30322f;
  color: #85887f;
  opacity: 0.7;
}
.empty {
  padding: 64rpx 28rpx;
  text-align: center;
  border: 1rpx dashed #30322f;
  border-radius: 20rpx;
  margin-top: 24rpx;
  color: #a2a49c;
  font-size: 26rpx;
  line-height: 1.8;
}
.empty-title {
  display: block;
  color: #f6f4ef;
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.error {
  color: #ffb18c;
}
.scroll {
  white-space: nowrap;
  width: 100%;
  margin-top: 28rpx;
}
.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
  padding: 0 26rpx;
  margin-right: 12rpx;
  border-radius: 14rpx;
  border: 1rpx solid #30322f;
  background: #1c1d1b;
  color: #a2a49c;
  font-size: 26rpx;
  box-sizing: border-box;
}
.chip.active {
  color: #111211;
  background: #ff7a35;
  border-color: #ff7a35;
  font-weight: 700;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 80rpx;
  color: #a2a49c;
  font-size: 26rpx;
  margin: -12rpx 0 12rpx;
}

.plan-title {
  font-size: 37rpx;
  line-height: 1.4;
  font-weight: 700;
  margin-top: 24rpx;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 18rpx;
  flex-wrap: wrap;
  margin-top: 28rpx;
}
.sale-price {
  font-size: 48rpx;
  font-weight: 700;
  color: #ff7a35;
}
.origin-price {
  font-size: 25rpx;
  color: #85887f;
  text-decoration: line-through;
}
.price-note {
  font-size: 22rpx;
  color: #a2a49c;
  margin-top: 12rpx;
}
.benefits {
  display: flex;
  gap: 28rpx;
  flex-wrap: wrap;
  border-top: 1rpx solid #30322f;
  margin-top: 28rpx;
  padding-top: 24rpx;
}
.benefit {
  min-width: 120rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.benefit-number {
  font-size: 34rpx;
  font-weight: 700;
}
.benefit-number text {
  font-size: 21rpx;
  font-weight: 400;
  color: #a2a49c;
  margin-left: 8rpx;
}
.benefit .muted {
  font-size: 22rpx;
}
.rule-note {
  font-size: 24rpx;
  line-height: 1.8;
  color: #a2a49c;
  margin: 24rpx 0;
}
.plan-card .btn {
  width: 100%;
}
</style>
