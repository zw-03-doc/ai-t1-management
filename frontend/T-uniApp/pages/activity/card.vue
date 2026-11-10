<template>
  <view class="catalog-page">
    <view v-if="error" class="catalog-state">{{ error }}<button class="catalog-button secondary" @tap="load">重试</button></view>
    <view v-else-if="!plan" class="catalog-state">加载中…</view>
    <block v-else>
      <view class="catalog-heading">{{ plan.planName }}</view>
      <view><text class="catalog-price">{{ priceText(plan.salePrice) }}</text><text v-if="plan.salePrice != null && Number(plan.originalPrice) > Number(plan.salePrice)" class="original">{{ priceText(plan.originalPrice) }}</text></view>
      <view class="catalog-pair"><text>卡项类型</text><text>{{ typeText(plan.planType) }}</text></view>
      <view class="catalog-pair"><text>适用门店</text><text>{{ plan.storeName || '全部门店' }}</text></view>
      <view v-if="plan.validDays > 0" class="catalog-pair"><text>有效期</text><text>{{ plan.validDays }} 天</text></view>
      <block v-if="total > 0">
        <view class="catalog-pair"><text>次数</text><text>{{ plan.totalTimes || 0 }} 次</text></view>
        <view class="catalog-pair"><text>赠送次数</text><text>{{ plan.giftTimes || 0 }} 次</text></view>
        <view class="catalog-pair"><text>总次数</text><text>{{ total }} 次</text></view>
      </block>
      <view v-if="plan.storedAmount > 0" class="catalog-pair"><text>储值金额</text><text>{{ priceText(plan.storedAmount) }}</text></view>
      <view v-if="plan.supportCourseType" class="catalog-pair"><text>适用课程类型</text><text>{{ courseText(plan.supportCourseType) }}</text></view>
      <view v-if="plan.isPrivateBindCoach === '1'" class="catalog-pair"><text>私教预约</text><text>限绑定教练</text></view>
      <view v-if="activity && (activity.ruleNote || activity.discountLabel)" class="catalog-section">活动权益</view>
      <view v-if="activity?.discountLabel" class="catalog-body">{{ activity.discountLabel }}</view>
      <view v-if="activity?.ruleNote" class="catalog-body">{{ activity.ruleNote }}</view>
      <view v-if="courses.length" class="catalog-section">适用课程</view>
      <view v-for="course in courses" :key="course.courseId" class="catalog-row" @tap="go('course/detail', {id: course.courseId})"><text>{{ course.courseName }}</text><text class="catalog-arrow">›</text></view>
      <button v-if="activity && (activity.coachContactWechat || activity.coachContactQrUrl)" class="catalog-button" @tap="go('activity/contact', {activityId})">联系教练微信</button>
    </block>
  </view>
</template>
<script>
import { memberActivityDetail, memberCardPlanDetail } from '../../api/auth'
import { catalogLink, planType, courseType, price, grantedTimes } from '../../utils/catalog'
export default {
  data() { return { id: '', activityId: '', plan: null, activity: null, error: '' } },
  computed: {
    total() { return this.plan ? grantedTimes(this.plan) : 0 },
    courses() {
      if (!this.plan) return []
      const { bindCourseId, bindCourseName, additionalCourses = [] } = this.plan
      const first = bindCourseId ? [{ courseId: bindCourseId, courseName: bindCourseName || '查看课程' }] : []
      return first.concat(additionalCourses || [])
    }
  },
  onLoad(options) { this.id = options.id; this.activityId = options.activityId || ''; this.load() },
  methods: {
    typeText: planType, priceText: price, courseText: courseType,
    go(path, params) { uni.navigateTo({url: catalogLink(path, params)}) },
    async load() {
      this.error = ''
      try {
        const [plan, activity] = await Promise.all([memberCardPlanDetail(this.id), this.activityId ? memberActivityDetail(this.activityId) : null])
        if (activity && (activity.actionType === 'catalog' ? activity.storeId && plan.storeId && activity.storeId !== plan.storeId : !activity.planList?.some(p => p.planId === plan.planId))) throw new Error('该卡项已移出本次活动')
        this.plan = plan; this.activity = activity
      } catch(e) { this.error = e.message || '卡项加载失败' }
    }
  }
}
</script>
<style scoped>
@import "../../styles/catalog.css";
.original { color: var(--t1-muted); margin-left: 18rpx; text-decoration: line-through; font-size: 25rpx; }
</style>
