<template>
  <view class="page-wrap with-nav">

    <scroll-view class="scroll" scroll-x :show-scrollbar="false">
      <view
        class="chip"
        :class="{
          active: selectedStoreId === '',
          disabled: loading || switchingStore,
        }"
        @tap="switchStore('')"
        >全部门店</view
      >
      <view
        class="chip"
        v-for="item in stores"
        :key="item.storeId"
        :class="{
          active: selectedStoreId === item.storeId,
          disabled: loading || switchingStore,
        }"
        @tap="switchStore(item.storeId)"
        >{{ item.storeName }}</view
      >
    </scroll-view>
    <view class="section-row"
      ><text class="section-title">正在发生</text
      ><text class="muted" v-if="!loading && !errorText"
        >{{ activityList.length }} 个活动</text
      ></view
    >
    <view v-if="loading" class="empty">正在加载活动…</view>
    <view
      v-else-if="errorText"
      class="empty error"
      @tap="loadStoresAndActivities"
      ><text class="empty-title">暂时没能加载活动</text>{{ errorText
      }}<view>轻点重试</view></view
    >
    <block v-else>
      <view
        class="card activity-card"
        v-for="item in activityList"
        :key="item.activityId"
        @tap="goActivityDetail(item)"
      >
        <image
          v-if="item.coverUrl"
          class="activity-cover"
          :src="item.coverUrl"
          mode="aspectFill"
        />
        <view class="row"
          ><text class="activity-store">{{ item.storeName || "全门店" }}</text
          ><text class="tag" v-if="item.weeklyFeatured === '1'"
            >本周推荐</text
          ></view
        >
        <view class="activity-title">{{ item.title }}</view>
        <view v-if="item.subTitle" class="muted activity-subtitle">{{
          item.subTitle
        }}</view>
        <view v-if="item.priceHighlight" class="highlight">{{
          item.priceHighlight
        }}</view>
        <view v-if="item.targetAudience" class="muted audience"
          >适合 {{ item.targetAudience }}</view
        >
        <view class="activity-footer"
          ><text>{{
            formatCountdown(item.countdownEndTime || item.endTime)
          }}</text
          ><text class="detail-link">了解活动 →</text></view
        >
      </view>
      <view v-if="!activityList.length" class="empty"
        ><text class="empty-title">暂无活动</text
        >试试其他门店，或下次再来看看。</view
      >
    </block>
    <T1Nav role="member" current="home" />
  </view>
</template>
<script>
import T1Nav from "../../components/T1Nav.vue";
import { memberActivityList, memberStoreList } from "../../api/auth";

export default {
  components: { T1Nav },
  data() {
    return {
      stores: [],
      selectedStoreId: "",
      activityList: [],
      loading: false,
      switchingStore: false,
      errorText: "",
    };
  },
  onShow() {
    this.loadStoresAndActivities();
  },
  methods: {
    async loadStoresAndActivities() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      this.errorText = "";
      try {
        const [stores, activities] = await Promise.all([
          memberStoreList(""),
          memberActivityList(),
        ]);
        this.stores = stores || [];
        this.activityList = activities || [];
      } catch (error) {
        this.errorText = error.message || "活动暂时没能加载";
        this.activityList = [];
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.loading = false;
      }
    },
    async switchStore(storeId) {
      if (
        this.selectedStoreId === storeId ||
        this.loading ||
        this.switchingStore
      ) {
        return;
      }
      const prevStoreId = this.selectedStoreId;
      this.selectedStoreId = storeId;
      this.switchingStore = true;
      this.errorText = "";
      try {
        this.activityList = (await memberActivityList(storeId)) || [];
      } catch (error) {
        this.selectedStoreId = prevStoreId;
        this.errorText = error.message || "门店切换失败";
        uni.showToast({ title: this.errorText, icon: "none" });
      } finally {
        this.switchingStore = false;
      }
    },
    goActivityDetail(item) {
      if (!item || !item.activityId) {
        return;
      }
      uni.navigateTo({ url: `/pages/activity/detail?id=${item.activityId}` });
    },
    formatCountdown(value) {
      if (!value) {
        return "时间请咨询门店";
      }
      const end = new Date(value);
      if (Number.isNaN(end.getTime())) {
        return "时间请咨询门店";
      }
      const diff = end.getTime() - Date.now();
      if (diff <= 0) {
        return "活动已结束";
      }
      const day = Math.floor(diff / (24 * 60 * 60 * 1000));
      const hour = Math.floor(
        (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000),
      );
      if (day > 0) {
        return `还剩 ${day}天${hour}小时`;
      }
      return `还剩 ${hour}小时`;
    },
  },
};
</script>
<style scoped>
.page-wrap {
  min-height: 100vh;
  box-sizing: border-box;
  background: var(--t1-bg);
  color: var(--t1-text);
  padding: 32rpx 32rpx calc(48rpx + env(safe-area-inset-bottom));
  max-width: 960px;
  margin: auto;
}
.with-nav {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}
.page-subtitle,
.muted {
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--t1-muted);
}
.panel,
.card {
  background: var(--t1-surface);
  border: 1rpx solid var(--t1-border);
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
  background: var(--t1-tint);
  color: var(--t1-accent);
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
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-size: 28rpx;
  font-weight: 700;
}
.btn::after {
  border: 0;
}
.btn.ghost {
  background: transparent;
  border: 1rpx solid var(--t1-border);
  color: var(--t1-text);
}
.btn[disabled] {
  background: var(--t1-inset);
  color: var(--t1-muted);
  opacity: 0.7;
}
.empty {
  padding: 64rpx 28rpx;
  text-align: center;
  border: 1rpx dashed var(--t1-border);
  border-radius: 20rpx;
  margin-top: 24rpx;
  color: var(--t1-muted);
  font-size: 26rpx;
  line-height: 1.8;
}
.empty-title {
  display: block;
  color: var(--t1-text);
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.error {
  color: var(--t1-danger);
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
  border: 1rpx solid var(--t1-border);
  background: var(--t1-surface);
  color: var(--t1-muted);
  font-size: 26rpx;
  box-sizing: border-box;
}
.chip.active {
  color: var(--t1-on-dark);
  background: var(--t1-text);
  border-color: var(--t1-accent);
  font-weight: 700;
}
.back-link {
  display: inline-flex;
  align-items: center;
  min-height: 80rpx;
  color: var(--t1-muted);
  font-size: 26rpx;
  margin: -12rpx 0 12rpx;
}
.hero-image,
.hero-shade {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.section-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 36rpx;
}
.section-row .muted {
  font-size: 22rpx;
}
.disabled {
  opacity: 0.5;
}
.activity-cover {
  display: block;
  width: 100%;
  height: 300rpx;
  border-radius: 14rpx;
  margin-bottom: 24rpx;
}
.activity-store {
  font-size: 23rpx;
  color: var(--t1-muted);
}
.activity-title {
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.45;
  margin-top: 20rpx;
  word-break: break-all;
}
.activity-subtitle {
  margin-top: 14rpx;
}
.highlight {
  font-size: 29rpx;
  line-height: 1.6;
  color: var(--t1-accent);
  margin-top: 22rpx;
}
.audience {
  font-size: 23rpx;
  margin-top: 12rpx;
}
.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
  border-top: 1rpx solid var(--t1-border);
  margin-top: 24rpx;
  padding-top: 24rpx;
  font-size: 23rpx;
  color: var(--t1-muted);
}
.detail-link {
  color: var(--t1-accent);
  white-space: nowrap;
}
</style>
