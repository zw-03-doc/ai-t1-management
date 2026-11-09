import { API_BASE_URL } from './config'

const api = API_BASE_URL.replace(/\/$/, '')
const storageKey = 'T1_VENUE_ALBUM:' + api
let localBase = '/'
// #ifdef H5
localBase = import.meta.env.BASE_URL
// #endif
const defaults = ['live-1', 'live-2', 'live-3'].map(id => ({ id, src: localBase + 'static/images/' + id + '.jpg' }))
let latest
let pending

function normalize(album) {
  if (!Number.isInteger(album?.version) || album.version < 0 || !Array.isArray(album.photos)) return null
  const photos = []
  for (const item of album.photos) {
    const id = item?.id
    if (typeof id !== 'string') return null
    if (/^live-[123]$/.test(id)) {
      photos.push({ id, src: localBase + 'static/images/' + id + '.jpg' })
    } else if (/^[a-f0-9-]{36}$/.test(id) && item.url === '/fitness/mini/portal/venue/images/' + id) {
      photos.push({ id, src: api + item.url })
    } else return null
  }
  return { version: album.version, photos }
}

export function readVenuePhotos() {
  if (!latest) {
    try { latest = normalize(uni.getStorageSync(storageKey)) } catch { /* Optional offline cache. */ }
  }
  return latest?.photos ?? defaults
}

export function refreshVenuePhotos() {
  if (!pending) {
    pending = new Promise(resolve => uni.request({
      url: api + '/fitness/mini/portal/venue',
      timeout: 6000,
      success(response) {
        const record = response.statusCode === 200 && response.data?.code === 200 ? response.data.data : null
        const album = normalize(record)
        if (album) {
          latest = album
          try { uni.setStorageSync(storageKey, record) } catch { /* Keep the in-memory album. */ }
        }
        resolve(readVenuePhotos())
      },
      fail: () => resolve(readVenuePhotos())
    })).finally(() => { pending = null })
  }
  return pending
}
