const MEMBER_SESSION_KEY = 'T1_MEMBER_SESSION'
const COACH_SESSION_KEY = 'T1_COACH_SESSION'
export const POLICY_VERSION = '2026-11-09'

function readSession(key) {
	const session = uni.getStorageSync(key)
	return session?.policyVersion === POLICY_VERSION ? session : {}
}

function writeSession(key, value) {
	uni.setStorageSync(key, value || {})
}

export function getMemberSession() {
	return readSession(MEMBER_SESSION_KEY)
}

export function saveMemberSession(loginVo) {
	writeSession(MEMBER_SESSION_KEY, {
		policyVersion: loginVo?.policyVersion,
		accessToken: (loginVo && loginVo.accessToken) || '',
		clientId: (loginVo && loginVo.clientId) || '',
		openid: (loginVo && loginVo.openid) || '',
		memberInfo: (loginVo && loginVo.memberInfo) || {}
	})
}

export function clearMemberSession() {
	uni.removeStorageSync(MEMBER_SESSION_KEY)
}

export function hasMemberSession() {
	const session = getMemberSession()
	return !!session.accessToken && !!session.clientId
}

export function getCoachSession() {
	return readSession(COACH_SESSION_KEY)
}

export function saveCoachSession(loginVo) {
	writeSession(COACH_SESSION_KEY, {
		policyVersion: loginVo?.policyVersion,
		accessToken: (loginVo && loginVo.accessToken) || '',
		clientId: (loginVo && loginVo.clientId) || '',
		openid: (loginVo && loginVo.openid) || '',
		activated: !!(loginVo && loginVo.activated),
		needActivation: !!(loginVo && loginVo.needActivation),
		message: (loginVo && loginVo.message) || '',
		profile: (loginVo && loginVo.profile) || {}
	})
}

export function clearCoachSession() {
	uni.removeStorageSync(COACH_SESSION_KEY)
}

export function hasCoachSession() {
	const session = getCoachSession()
	return !!session.accessToken && !!session.clientId
}

export function resolveTargetUrl(role, target) {
	if (target) {
		return target
	}
	return role === 'coach' ? '/pages/coach/home/index' : '/pages/index/index'
}

const memberDestinations = [
	'/pages/index/index', '/pages/mine/index', '/pages/course/index', '/pages/course/detail',
	'/pages/training/index', '/pages/activity/index', '/pages/activity/detail',
	'/pages/activity/plan', '/pages/activity/card', '/pages/activity/contact', '/pages/guide/index'
]

export function memberProfileTarget(value) {
	const home = '/pages/index/index'
	if (!value || typeof value !== 'string') return home
	try {
		const target = value.startsWith('/') ? value : decodeURIComponent(value)
		const [path, query = ''] = target.split('?')
		if (!memberDestinations.includes(path) || /[#\r\n]/.test(target)) return home
		if (path === '/pages/training/index' && /(?:^|&)role=coach(?:&|$)/.test(decodeURIComponent(query))) return home
		return target
	} catch {
		return home
	}
}
