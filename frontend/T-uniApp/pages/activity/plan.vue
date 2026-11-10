<template>
  <view class="catalog-page">
    <view v-if="activity" class="catalog-heading">{{ activity.title }}</view>
    <view v-if="error" class="catalog-state">{{ error }}<button class="catalog-button secondary" @tap="load">重试</button></view>
    <view v-else-if="loading" class="catalog-state">加载中…</view>
    <block v-else>
      <view v-for="plan in plans" :key="plan.planId" class="catalog-row" @tap="open(plan)">
        <view><view class="catalog-name">{{ plan.planName }}</view><view class="catalog-sub">{{ summary(plan) }}</view><view v-if="plan.storeName" class="catalog-sub">{{ plan.storeName }}</view></view>
        <text class="catalog-price">{{ priceText(plan.salePrice) }} ›</text>
      </view>
      <view v-if="!plans.length" class="catalog-state">暂无上架卡项</view>
    </block>
  </view>
</template>
<script>
import { memberActivityDetail, memberCardPlanList } from '../../api/auth'
import { catalogLink, planSummary, price } from '../../utils/catalog'
export default {
  data() { return { id: '', storeId: '', activity: null, plans: [], loading: true, error: '' } },
  onLoad(options) { this.id = options.id || ''; this.storeId = options.storeId || ''; this.load() },
  methods: {
    priceText: price, summary: planSummary,
    async load() {
      this.loading = true; this.error = ''
      try {
        this.activity = this.id ? await memberActivityDetail(this.id) : null
        this.plans = this.activity && this.activity.actionType !== 'catalog'
          ? this.activity.planList || [] : await memberCardPlanList(this.activity?.storeId || this.storeId) || []
      } catch(e) { this.error = e.message || '价目表加载失败' }
      finally { this.loading = false }
    },
    open(plan) { uni.navigateTo({url: catalogLink('activity/card', {id: plan.planId, activityId: this.id})}) }
  }
}
</script>
<style scoped>
@import "../../styles/catalog.css";
</style>
