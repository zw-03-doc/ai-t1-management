export const DEFAULT_API_BASE_URL = 'http://127.0.0.1:8080'

export const API_BASE_URL = uni.getStorageSync('T1_API_BASE_URL') || DEFAULT_API_BASE_URL

export const PUBLIC_ROUTES = ['/pages/portal/index', '/pages/activity/index']

export const MEMBER_ROUTES = [
	'/pages/index/index',
	'/pages/course/index',
	'/pages/mine/index',
	'/pages/mine/profile'
]

export const COACH_ROUTE_PREFIX = '/pages/coach/'
