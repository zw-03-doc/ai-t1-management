<template>
  <view class="member-avatar">
    <image v-if="displayUrl" :src="displayUrl" mode="aspectFill" @error="displayUrl = ''" />
    <text v-else>T1</text>
  </view>
</template>

<script>
import { MEMBER_AVATAR_PREFIX, downloadMemberAvatar, releaseMemberAvatar } from '../api/member-avatar'

export default {
  props: { source: { type: String, default: '' } },
  data() {
    return { displayUrl: '', downloadedUrl: '', revision: 0 }
  },
  watch: {
    source: {
      immediate: true,
      async handler(source) {
        const revision = ++this.revision
        releaseMemberAvatar(this.downloadedUrl)
        this.downloadedUrl = ''
        this.displayUrl = ''
        if (!source?.startsWith(MEMBER_AVATAR_PREFIX)) {
          this.displayUrl = source || ''
          return
        }
        try {
          const url = await downloadMemberAvatar(source)
          if (this.revision !== revision) {
            releaseMemberAvatar(url)
            return
          }
          this.downloadedUrl = url
          this.displayUrl = url
        } catch {
          // Keep the initials visible when the photo is unavailable.
        }
      }
    }
  },
  beforeUnmount() {
    this.revision++
    releaseMemberAvatar(this.downloadedUrl)
  }
}
</script>

<style scoped>
.member-avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #30271f;
  color: #ff7a35;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 700;
  flex-shrink: 0;
}
.member-avatar image { width: 100%; height: 100%; }
</style>
