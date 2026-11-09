<template>
  <view class="gallery">
    <view v-if="!photos.length" class="photo-error">暂无场馆照片</view>
    <swiper v-else class="gallery-swiper" :current="current" @change="current = $event.detail.current">
      <swiper-item v-for="(photo, index) in photos" :key="photo.id">
        <view v-if="failed[photo.id]" class="photo-error">这张照片暂时无法显示</view>
        <image v-else class="gallery-photo" :src="photo.src" mode="aspectFit"
          :aria-label="'场馆环境照片 ' + (index + 1)" @error="failed[photo.id] = true" />
      </swiper-item>
    </swiper>
    <view v-if="photos.length" class="gallery-controls">
      <button class="gallery-prev" :disabled="current === 0" @tap="current--">上一张</button>
      <text class="gallery-counter">{{ current + 1 }} / {{ photos.length }}</text>
      <button class="gallery-next" :disabled="current === photos.length - 1" @tap="current++">下一张</button>
    </view>
  </view>
</template>

<script>
import { readVenuePhotos, refreshVenuePhotos, venueStoreId, watchVenueQuery } from '../../utils/venue-photos'

export default {
  data() {
    return { photos: [], current: 0, failed: {}, albumRequest: 0 }
  },
  onLoad(options) {
    this.loadAlbum(options)
    this.stopVenueQuery = watchVenueQuery('/pages/portal/gallery', this.loadAlbum)
  },
  onUnload() {
    this.albumRequest++
    this.stopVenueQuery?.()
  },
  methods: {
    loadAlbum(options) {
      const request = ++this.albumRequest
      this.current = 0
      this.failed = {}
      const store = venueStoreId(options?.storeId)
      this.photos = readVenuePhotos(store)
      const index = Number(options?.index)
      const byId = this.photos.findIndex(photo => photo.id === options?.id)
      if (byId >= 0) this.current = byId
      else if (Number.isInteger(index) && index >= 0 && index < this.photos.length) this.current = index
      const initial = this.current
      refreshVenuePhotos(store).then(photos => {
        if (request !== this.albumRequest) return
        const id = this.current === initial && options?.id ? options.id : this.photos[this.current]?.id
        const selected = photos.findIndex(photo => photo.id === id)
        this.current = selected >= 0 ? selected : Math.max(0, Math.min(this.current, photos.length - 1))
        this.photos = photos
      })
    }
  }
}
</script>

<style scoped>
.gallery {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  background: #101110;
  color: #f6f4ef;
}
.gallery-swiper { flex: 1; min-height: 0; width: 100%; }
.gallery-photo, .photo-error { width: 100%; height: 100%; }
.photo-error { display: flex; align-items: center; justify-content: center; }
.gallery-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  padding: 12px 16px calc(16px + env(safe-area-inset-bottom));
}
.gallery-controls button {
  margin: 0;
  padding: 0 16px;
  border-radius: 8px;
  background: #272824;
  color: #f6f4ef;
  font-size: 15px;
  line-height: 44px;
}
.gallery-controls button[disabled] { opacity: .45; }
.gallery-counter { font-size: 14px; font-variant-numeric: tabular-nums; }
</style>
