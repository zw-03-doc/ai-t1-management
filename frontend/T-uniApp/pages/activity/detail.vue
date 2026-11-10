<template>
  <view class="catalog-page">
    <view v-if="error" class="catalog-state">{{ error }}<button class="catalog-button secondary" @tap="load">重试</button></view>
    <view v-else-if="!detail" class="catalog-state">加载中…</view>
    <block v-else>
      <image v-if="detail.coverUrl" class="catalog-cover" :src="image(detail.coverUrl)" mode="aspectFill" />
      <view class="catalog-heading">{{ detail.title }}</view>
      <view v-if="detail.subTitle" class="catalog-body">{{ detail.subTitle }}</view>
      <view class="catalog-pair"><text>活动日期</text><text>{{ period(detail.startTime, detail.endTime) }}</text></view>
      <view class="catalog-pair"><text>适用门店</text><text>{{ detail.storeName || '全部门店' }}</text></view>
      <view v-if="detail.priceHighlight || detail.discountLabel" class="catalog-pair"><text>活动优惠</text><text>{{ [detail.priceHighlight, detail.discountLabel].filter(Boolean).join(' · ') }}</text></view>
      <view v-if="detail.targetAudience" class="catalog-pair"><text>适用人群</text><text>{{ detail.targetAudience }}</text></view>
      <view v-if="detail.ruleNote" class="catalog-section">活动规则</view>
      <view v-if="detail.ruleNote" class="catalog-body">{{ detail.ruleNote }}</view>
      <view v-if="detail.detailContent" class="catalog-section">活动详情</view>
      <view v-if="detail.detailContent" class="catalog-body">{{ detail.detailContent }}</view>
      <image v-if="detail.detailImageUrl" class="catalog-detail-image" mode="widthFix" :src="image(detail.detailImageUrl)" />
      <button v-if="primary" class="catalog-button" @tap="act">{{ primary }}</button>
      <view v-else class="catalog-state">{{ detail.actionType === 'plan' ? '当前没有可查看的活动卡项' : '当前暂无可用的咨询方式' }}</view>
      <button v-if="detail.actionType !== 'coach' && hasCoach" class="catalog-button secondary" @tap="contact">联系教练微信</button>
    </block>
  </view>
</template>
<script>
import { memberActivityDetail } from '../../api/auth'
import { catalogImage, catalogLink, datePeriod } from '../../utils/catalog'
export default {
  data() { return { id: '', storeId: '', detail: null, error: '' } },
  computed: {
    hasCoach() { return !!(this.detail?.coachContactWechat || this.detail?.coachContactQrUrl) },
    primary() {
      const d = this.detail
      if (d.actionType === 'contact') return d.storeContactPhone ? '致电门店' : ''
      if (d.actionType === 'coach') return this.hasCoach ? '联系教练微信' : ''
      if (d.actionType === 'plan') return d.planList?.length ? '查看卡项详情' : ''
      if (d.actionType === 'course') return d.courseId ? '查看课程' : ''
      return '查看卡项价目表'
    }
  },
  onLoad(options) { this.id = options.id; this.storeId = options.storeId || ''; this.load() },
  methods: {
    image: catalogImage, period: datePeriod,
    async load() { this.error = ''; try { this.detail = await memberActivityDetail(this.id) } catch(e) { this.error = e.message || '活动加载失败' } },
    go(path, params) { uni.navigateTo({url: catalogLink(path, params)}) },
    contact() { this.go('activity/contact', {activityId: this.id}) },
    act() {
      const d = this.detail
      if (d.actionType === 'contact') return uni.makePhoneCall({phoneNumber: d.storeContactPhone})
      if (d.actionType === 'coach') return this.contact()
      if (d.actionType === 'plan') return this.go('activity/card', {id: d.planList[0].planId, activityId: this.id})
      if (d.actionType === 'course') return this.go('course/detail', {id: d.courseId})
      this.go('activity/plan', {id: this.id, storeId: d.storeId || this.storeId})
    }
  }
}
</script>
<style scoped>
@import "../../styles/catalog.css";
</style>
