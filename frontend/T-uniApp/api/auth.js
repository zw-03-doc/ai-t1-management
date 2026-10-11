import { request } from '../utils/request'

// ==================== 会员端 ====================
export function memberWechatLogin(data) {
	return request({
		url: '/fitness/mini/auth/wechatLogin',
		method: 'POST',
		data
	})
}

export function memberHomeIndex() {
	return request({
		url: '/fitness/mini/home/index',
		auth: 'member'
	})
}

export function memberBindPhone(data) {
	return request({
		url: '/fitness/mini/auth/bindPhone',
		method: 'POST',
		data,
		auth: 'member'
	})
}

export function memberMineSummary() {
	return request({
		url: '/fitness/mini/mine/summary',
		auth: 'member'
	})
}

export function memberMineProfile() {
	return request({
		url: '/fitness/mini/mine/profile',
		auth: 'member'
	})
}

export function memberUpdateProfile(data) {
	return request({
		url: '/fitness/mini/mine/profile',
		method: 'PUT',
		data,
		auth: 'member'
	})
}

export function memberStoreList(keyword) {
	return request({
		url: '/fitness/mini/store/list',
		data: { keyword },
		auth: 'member'
	})
}

export function memberCourseList(params = {}) {
	return request({
		url: '/fitness/mini/course/list',
		data: params,
		auth: 'member'
	})
}

export function memberScheduleList(params = {}) {
	return request({
		url: '/fitness/mini/schedule/list',
		data: params,
		auth: 'member'
	})
}

export function memberCreateReservation(data) {
	return request({
		url: '/fitness/mini/reservation',
		method: 'POST',
		data,
		auth: 'member'
	})
}

export function memberReservationList(params = {}) {
	return request({
		url: '/fitness/mini/reservation/my',
		data: params,
		auth: 'member'
	})
}

export function memberCancelReservation(reservationId) {
	return request({
		url: `/fitness/mini/reservation/${reservationId}/cancel`,
		method: 'PUT',
		auth: 'member'
	})
}

export function memberCardPlanList(storeId) {
	const params = {}
	if (storeId !== undefined && storeId !== null && storeId !== '') {
		params.storeId = storeId
	}
	return request({
		url: '/fitness/mini/cardPlan/list',
		data: params,
		auth: 'member'
	})
}

export function memberActivityList(storeId) {
	const params = {}
	if (storeId !== undefined && storeId !== null && storeId !== '') {
		params.storeId = storeId
	}
	return request({
		url: '/fitness/mini/activity/list',
		data: params,
		auth: 'member'
	})
}

export function memberActivityDetail(activityId) {
	return request({
		url: `/fitness/mini/activity/${activityId}`,
		auth: 'member'
	})
}

export function memberActivityCardPlanList(activityId) {
	return request({
		url: `/fitness/mini/activity/${activityId}/plans`,
		auth: 'member'
	})
}

export function memberCreateOrder(data) {


	return request({
		url: '/fitness/mini/order',
		method: 'POST',
		data,
		auth: 'member'
	})
}

export function memberMyOrders(params = {}) {
	return request({
		url: '/fitness/mini/order/my',
		data: params,
		auth: 'member'
	})
}

export function memberMyCards() {
	return request({
		url: '/fitness/mini/memberCard/my',
		auth: 'member'
	})
}

export function memberCreateCheckin(data) {
	return request({
		url: '/fitness/mini/checkin',
		method: 'POST',
		data,
		auth: 'member'
	})
}

export function memberCheckinStatus(reservationId) {
	return request({
		url: `/fitness/mini/checkin/${reservationId}/status`,
		auth: 'member'
	})
}

// ==================== 教练端 ====================
export function coachWechatLogin(data) {
	return request({
		url: '/fitness/coachApp/auth/wechatLogin',
		method: 'POST',
		data
	})
}

export function coachWechatActivate(data) {
	return request({
		url: '/fitness/coachApp/auth/wechatActivate',
		method: 'POST',
		data
	})
}

export function coachLogout() {
	return request({
		url: '/fitness/coachApp/auth/logout',
		method: 'POST',
		auth: 'coach'
	})
}

export function coachHomeOverview() {
	return request({
		url: '/fitness/coachApp/home/overview',
		auth: 'coach'
	})
}

export function coachScheduleList(params = {}) {
	return request({
		url: '/fitness/coachApp/schedule/my',
		data: params,
		auth: 'coach'
	})
}

export function coachCheckinList(params = {}) {
	return request({
		url: '/fitness/coachApp/checkin/pending',
		data: params,
		auth: 'coach'
	})
}

export function coachConfirmCheckin(data) {
	return request({
		url: '/fitness/coachApp/checkin/confirm',
		method: 'POST',
		data,
		auth: 'coach'
	})
}

export function coachExceptionCheckin(data) {
	return request({
		url: '/fitness/coachApp/checkin/exception',
		method: 'POST',
		data,
		auth: 'coach'
	})
}

export function coachStudentList(params = {}) {
	return request({
		url: '/fitness/coachApp/student/my',
		data: params,
		auth: 'coach'
	})
}

export function coachProfile() {
	return request({
		url: '/fitness/coachApp/mine/profile',
		auth: 'coach'
	})
}

export function coachUpdateProfile(data) {
	return request({
		url: '/fitness/coachApp/mine/profile',
		method: 'PUT',
		data,
		auth: 'coach'
	})
}

export function coachUpdatePassword(data) {
	return request({
		url: '/fitness/coachApp/mine/password',
		method: 'PUT',
		data,
		auth: 'coach'
	})
}

export function coachMessageList(params = {}) {
	return request({
		url: '/fitness/coachApp/message/list',
		data: params,
		auth: 'coach'
	})
}
