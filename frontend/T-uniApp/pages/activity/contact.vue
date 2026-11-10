<template>
  <view class="catalog-page">
    <view v-if="error" class="catalog-state">{{ error }}<button class="catalog-button secondary" @tap="load">重试</button></view>
    <view v-else-if="!record" class="catalog-state">加载中…</view>
    <block v-else>
      <view class="catalog-heading">{{ record.coachName || '咨询教练' }}</view>
      <view v-if="record.coachContactQrUrl">
        <image class="catalog-qr" :src="image(record.coachContactQrUrl)" mode="aspectFit" show-menu-by-longpress @tap="preview" />
        <view class="catalog-sub" style="text-align:center">长按识别教练微信二维码</view>
      </view>
      <view v-if="record.coachContactWechat" class="catalog-pair"><text>微信号</text><text selectable>{{ record.coachContactWechat }}</text></view>
      <button v-if="record.coachContactWechat" class="catalog-button" @tap="copy">复制微信号</button>
      <view v-if="!record.coachContactWechat && !record.coachContactQrUrl" class="catalog-state">教练暂未提供咨询微信</view>
    </block>
  </view>
</template>
<script>
import { memberActivityDetail, memberCourseDetail } from '../../api/auth'
import { catalogImage } from '../../utils/catalog'
export default {
  data() { return {activityId: '', courseId: '', record: null, error: ''} },
  onLoad(options) { this.activityId = options.activityId; this.courseId = options.courseId; this.load() },
  methods: {
    image: catalogImage,
    async load() {
      this.error = ''
      try { this.record = this.activityId ? await memberActivityDetail(this.activityId) : await memberCourseDetail(this.courseId) }
      catch(e) { this.error = e.message || '联系方式加载失败' }
    },
    preview() { uni.previewImage({urls: [catalogImage(this.record.coachContactQrUrl)]}) },
    copy() { uni.setClipboardData({data: this.record.coachContactWechat, fail: () => uni.showToast({title: '请长按微信号复制', icon: 'none'})}) }
  }
}
</script>
<style scoped>
@import "../../styles/catalog.css";
</style>
