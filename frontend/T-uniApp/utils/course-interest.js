import { getMemberSession } from './auth'

export function interestPreferenceKey() {
  const member = getMemberSession().memberInfo
  return member?.memberId ? 'T1_INTEREST_CONSENT:' + member.tenantId + ':' + member.memberId : ''
}

export function courseInterestEnabled(key) {
  return !!key && uni.getStorageSync(key) === true
}
