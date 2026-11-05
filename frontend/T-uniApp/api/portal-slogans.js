import { API_BASE_URL } from '../utils/config'

const defaults = {
  eyebrow: 'YOUR NEXT REP',
  titleLead: '为下一次',
  titleAccent: '进步',
  titleSecondLine: '而练。',
  subtitle: '约一节好课，认真完成每一组。',
  galleryCaption: '在 T1，找到你的训练节奏',
  footerCaption: 'T1 · 每一组，都算数'
}
const base = API_BASE_URL.replace(/\/$/, '')
const cacheKey = 'T1_PORTAL_SLOGANS_V1:' + base

function sloganFields(value) {
  if (!value || typeof value !== 'object') return null
  const copy = {}
  for (const field of Object.keys(defaults)) {
    if (typeof value[field] !== 'string') return null
    copy[field] = value[field].trim()
    if (!copy[field] && field !== 'titleAccent') return null
  }
  return copy
}

export function readPortalSlogans() {
  try {
    const cached = sloganFields(uni.getStorageSync(cacheKey))
    if (cached) return cached
  } catch {
    // The page can render even if the device's storage is unavailable.
  }
  return { ...defaults }
}

export function cachePortalSlogans(slogans) {
  try {
    uni.setStorageSync(cacheKey, slogans)
  } catch {
    // The current page still displays the published text.
  }
}

export function fetchPortalSlogans() {
  // The portal loads before login. A failed copy refresh must leave login usable.
  return new Promise(resolve => uni.request({
    url: base + '/fitness/mini/portal/slogans',
    method: 'GET',
    timeout: 5000,
    success: response => {
      const result = response.data
      resolve(response.statusCode === 200 && result?.code === 200 ? sloganFields(result.data) : null)
    },
    fail: () => resolve(null)
  }))
}
