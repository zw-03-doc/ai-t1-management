import { API_BASE_URL } from './config'
import { beijingDate } from './booking-date'

const types = {
  experience: '体验卡', week_card: '周卡', month_card: '月卡', season_card: '季卡', year_card: '年卡',
  group_card: '团课卡', time_card: '时长卡', count_card: '次卡', stored_card: '储值卡',
  private_package: '私教包', small_group_package: '小班包'
}
const courses = { group: '团课', private: '私教课', small: '小班课', small_group: '小班课', experience: '体验课', all: '全部课程' }
export const planType = kind => types[kind] || '会员卡'
export const courseType = kind => courses[kind] || kind || ''
export function price(value) {
  return value == null || value === '' ? '价格待定' : '¥' + Number(Number(value).toFixed(2))
}
export function catalogImage(value) {
  if (!value) return ''
  const api = API_BASE_URL.replace(/\/$/, '')
  if (value.startsWith('/fitness/')) return api + value
  if (value.startsWith('/')) return api.replace(/\/prod-api$/, '') + value
  return value
}
export function datePeriod(start, end) {
  const a = beijingDate(start), b = beijingDate(end)
  if (!a || a === b) return b
  return b ? a + ' 至 ' + b : a
}
export function catalogLink(path, params = {}) {
  const query = Object.keys(params).filter(key => params[key] != null && params[key] !== '')
    .map(key => key + '=' + encodeURIComponent(params[key])).join('&')
  return '/pages/' + path + (query ? '?' + query : '')
}
export function planSummary(plan) {
  return [planType(plan.planType), plan.totalTimes > 0 ? plan.totalTimes + ' 次' : '', plan.validDays > 0 ? plan.validDays + ' 天' : ''].filter(Boolean).join(' · ')
}
