import { API_BASE_URL, DEFAULT_VENUE_STORE_ID } from './config'

const api = API_BASE_URL.replace(/\/$/, '')
const storageKey = 'T1_VENUE_ALBUM:' + api
let localBase = '/'
// #ifdef H5
localBase = import.meta.env.BASE_URL
// #endif
const defaults = ['live-1', 'live-2', 'live-3'].map(id => ({ id, src: localBase + 'static/images/' + id + '.jpg' }))
const latest = new Map()
const pending = new Map()

export function venueStoreId(value) {
  // Preserve the full database ID. JavaScript numbers cannot represent every store.
  return String(value ?? '').trim() || String(DEFAULT_VENUE_STORE_ID || '').trim()
}

function normalize(album, store) {
  if (!Number.isInteger(album?.version) || album.version < 0 || !Array.isArray(album.photos)) return null
  if (store && String(album.storeId) !== store) return null
  const photos = []
  for (const item of album.photos) {
    const id = item?.id
    if (typeof id !== 'string') return null
    if (/^live-[123]$/.test(id) && item.url === '/app/static/images/' + id + '.jpg') {
      photos.push({ id, src: localBase + 'static/images/' + id + '.jpg' })
    } else if (/^[a-f0-9-]{36}$/.test(id) && item.url === '/fitness/mini/portal/venue/images/' + id + (store ? '?storeId=' + store : '')) {
      photos.push({ id, src: api + item.url })
    } else return null
  }
  return { photos }
}

function cacheKey(store) { return store ? storageKey + ':' + store : storageKey }

export function readVenuePhotos(value) {
  const store = venueStoreId(value)
  if (store && !/^[1-9]\d{0,18}$/.test(store)) return []
  if (!latest.has(store)) {
    try {
      const cached = normalize(uni.getStorageSync(cacheKey(store)), store)
      if (cached) latest.set(store, cached)
    } catch { /* The album also works without persistent storage. */ }
  }
  return latest.get(store)?.photos ?? (store ? [] : defaults)
}

export function refreshVenuePhotos(value) {
  const store = venueStoreId(value)
  if (store && !/^[1-9]\d{0,18}$/.test(store)) return Promise.resolve([])
  if (!pending.has(store)) {
    const request = new Promise(resolve => uni.request({
      url: api + '/fitness/mini/portal/venue' + (store ? '?storeId=' + store : ''),
      timeout: 6000,
      success(response) {
        let record = response.statusCode === 200 && response.data?.code === 200 ? response.data.data : null
        if (response.statusCode === 404 || response.data?.code === 404) record = { storeId: store, version: 0, photos: [] }
        const album = normalize(record, store)
        if (album) {
          latest.set(store, album)
          try { uni.setStorageSync(cacheKey(store), record) } catch { /* Keep the in-memory album. */ }
        }
        resolve(readVenuePhotos(store))
      },
      fail: () => resolve(readVenuePhotos(store))
    })).finally(() => pending.delete(store))
    pending.set(store, request)
  }
  return pending.get(store)
}


// uni-app H5 reuses a page when only its hash query changes.
export function watchVenueQuery(page, changed) {
  // #ifdef H5
  const sync = () => {
    const [route, query = ''] = window.location.hash.slice(1).split('?')
    if (route === page) changed(Object.fromEntries(new URLSearchParams(query)))
  }
  window.addEventListener('hashchange', sync)
  return () => window.removeEventListener('hashchange', sync)
  // #endif
  // #ifndef H5
  return () => {}
  // #endif
}
