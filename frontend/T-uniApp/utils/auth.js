const MEMBER_SESSION_KEY = 'T1_MEMBER_SESSION'
const COACH_SESSION_KEY = 'T1_COACH_SESSION'

function readSession(key) {
	return uni.getStorageSync(key) || {}
}

function writeSession(key, value) {
	uni.setStorageSync(key, value || {})
}

export function getMemberSession() {
	return readSession(MEMBER_SESSION_KEY)
}

export function saveMemberSession(loginVo) {
	writeSession(MEMBER_SESSION_KEY, {
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
