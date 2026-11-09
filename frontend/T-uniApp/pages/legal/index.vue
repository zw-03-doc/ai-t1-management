<template>
  <view class="legal-page">
    <view class="legal-document">
      <view class="doc-title">{{ document.title }}</view>
      <text class="effective-date" selectable>生效日期 {{ version }}</text>
      <text class="doc-intro" selectable>{{ document.intro }}</text>
      <view v-for="section in document.sections" :key="section[0]" class="doc-section">
        <view class="doc-heading">{{ section[0] }}</view>
        <text class="doc-text" selectable>{{ section[1] }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { POLICY_VERSION } from '../../utils/auth'
import { policies } from './policies'

export default {
  data() {
    return { document: policies.terms, version: POLICY_VERSION }
  },
  onLoad(options) {
    this.document = options?.doc === 'privacy' ? policies.privacy : policies.terms
    uni.setNavigationBarTitle({ title: this.document.title })
  }
}
</script>

<style scoped>
.legal-page { min-height: 100vh; background: #fbfbf8; color: #252621; }
.legal-document { max-width: 680px; margin: auto; padding: 28px 22px calc(48px + env(safe-area-inset-bottom)); }
.doc-title { font-size: 29px; line-height: 1.3; font-weight: 650; }
.effective-date { display: block; margin-top: 8px; color: #65665f; font-size: 13px; }
.doc-intro, .doc-text { display: block; color: #41433d; font-size: 16px; line-height: 1.8; overflow-wrap: anywhere; }
.doc-intro { margin-top: 24px; }
.doc-section { margin-top: 24px; }
.doc-heading { margin-bottom: 6px; font-size: 18px; font-weight: 650; }
</style>
