<template>
  <view class="catalog-page with-nav">
    <view class="catalog-links">
      <button class="catalog-button secondary" @tap="open('activity/plan')">卡项价目表</button>
      <button class="catalog-button secondary" @tap="open('course/index')">课程目录</button>
    </view>
    <picker v-if="stores.length > 1 || storeId" class="catalog-select" :range="storeOptions" range-key="storeName" :value="storeIndex" @change="chooseStore">
      <text>{{ storeOptions[storeIndex]?.storeName || '全部门店' }} ﹀</text>
    </picker>
    <view v-if="loading" class="catalog-state">加载中…</view>
    <view v-else-if="error" class="catalog-state">{{ error }}<button class="catalog-button secondary" @tap="load">重试</button></view>
    <block v-else>
      <view v-for="item in activities" :key="item.activityId" class="catalog-event" @tap="open('activity/detail', { id: item.activityId })">
        <image v-if="item.coverUrl" class="catalog-cover" :src="image(item.coverUrl)" mode="aspectFill" />
        <view class="catalog-name">{{ item.title }}</view>
        <view class="catalog-sub">{{ period(item.startTime, item.endTime) }}</view>
        <view v-if="item.priceHighlight" class="catalog-body">{{ item.priceHighlight }}</view>
      </view>
      <view v-if="!activities.length" class="catalog-state">暂无进行中的活动</view>
    </block>
    <T1Nav role="member" current="home" />
  </view>
</template>
<script>
import T1Nav from '../../components/T1Nav.vue'
import { memberActivityList, memberStoreList } from '../../api/auth'
import { catalogImage, catalogLink, datePeriod } from '../../utils/catalog'
export default {
  components: { T1Nav },
  data() { return { stores: [], storeId: '', activities: [], loading: true, error: '', sequence: 0 } },
  computed: {
    storeOptions() { return [{storeId: '', storeName: '全部门店'}, ...this.stores] },
    storeIndex() { return Math.max(0, this.storeOptions.findIndex(s => String(s.storeId) === this.storeId)) }
  },
  onLoad(options) { this.storeId = options?.storeId || '' },
  onShow() { this.load() },
  methods: {
    image: catalogImage, period: datePeriod,
    async load() {
      const seq = ++this.sequence
      this.loading = true
      this.error = ''
      try {
        const [stores, activities] = await Promise.all([memberStoreList(), memberActivityList(this.storeId)])
        if (seq === this.sequence) { this.stores = stores || []; this.activities = activities || [] }
      } catch (e) { if (seq === this.sequence) this.error = e.message || '活动加载失败' }
      finally { if (seq === this.sequence) this.loading = false }
    },
    chooseStore(e) { this.storeId = String(this.storeOptions[e.detail.value].storeId); this.load() },
    open(path, params) { uni.navigateTo({ url: catalogLink(path, { storeId: this.storeId, ...params }) }) }
  }
}
</script>
<style scoped>
@import "../../styles/catalog.css";
</style>
