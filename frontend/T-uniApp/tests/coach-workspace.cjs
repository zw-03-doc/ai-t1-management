const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

function mount(file, platform, overrides) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?;\s*$/gm, '')
    .replace(/\/\/ #(ifdef|ifndef) MP-WEIXIN\r?\n([\s\S]*?)\/\/ #endif/g,
      (_, condition, code) => (condition === 'ifdef') === (platform === 'mp') ? code : '')
    .replace('export default', 'const component =')
  const context = {
    T1Nav: {}, LoginConsent: {}, POLICY_VERSION: '2026-11-09', readVenuePhotos: () => [],
    uni: { showToast() {}, showLoading() {}, hideLoading() {} }, ...overrides
  }
  const component = vm.runInNewContext(script + '\ncomponent', context)
  const state = component.data()
  for (const [name, fn] of Object.entries(component.methods)) state[name] = fn.bind(state)
  for (const [name, fn] of Object.entries(component.computed || {})) Object.defineProperty(state, name, { get: fn.bind(state) })
  return state
}
const payload = value => JSON.parse(JSON.stringify(value))

test('首次绑定校验密码后向同一次激活请求提交，完成后清空表单', async () => {
  const calls = []
  const state = mount('pages/portal/index.vue', 'mp', {
    coachWechatActivate: async data => { calls.push(payload(data)); return { activated: true } },
    saveCoachSession: result => assert(!('webPassword' in result))
  })
  state.openActivationPopup({})
  state.agreedLoginRole = 'coach'
  state.activationForm.mobile = '13900000001'
  state.activationForm.activationCode = 'activation-code'
  let wxCalls = 0
  state.wxLogin = async () => { wxCalls++; return {code: 'fresh-code'} }
  state.refreshLoginState = () => {}
  state.navigateAfterLogin = () => {}
  await state.submitCoachActivation()
  assert.equal(wxCalls, 0)
  state.activationForm.webPassword = 'test-secret'
  await state.submitCoachActivation()
  assert.deepEqual(calls, [{
    code: 'fresh-code', mobile: '13900000001', activationCode: 'activation-code',
    webPassword: 'test-secret', consentVersion: '2026-11-09'
  }])
  assert.equal(state.activationForm.webPassword, '')
  assert.equal(state.activationPopupVisible, false)
})

for (const platform of ['mp', 'h5']) {
  test(platform + ' 设密使用对应身份凭据', async () => {
    const calls = []
    const state = mount('pages/coach/mine/index.vue', platform, {
      coachUpdatePassword: async data => calls.push(payload(data)),
      uni: { showToast() {}, showLoading() {}, hideLoading() {}, login: req => {
        assert.equal(platform, 'mp')
        req.success({code: 'fresh-binding-code'})
      } }
    })
    state.passwordForm.newPassword = state.passwordForm.confirmPassword = 'new-test-secret'
    if (platform === 'h5') {
      await state.savePassword()
      assert.equal(calls.length, 0)
      state.passwordForm.oldPassword = 'old-test-secret'
    }
    await state.savePassword()
    assert.deepEqual(calls, [platform === 'mp' ? {code:'fresh-binding-code',newPassword:'new-test-secret'} :
      {oldPassword:'old-test-secret',newPassword:'new-test-secret'}])
    assert.equal(state.profile.webPasswordSet, true)
    assert.equal(state.passwordForm.newPassword, '')
  })
}

test('从排课表新建只提交课程和时间，防止重复提交，返回时选择正确开课日', async () => {
  const calls = []
  const navigations = []
  const date = '2099-03-05'
  let finish
  const state = mount('pages/coach/schedule/create.vue', 'mp', {
    coachScheduleCourses: async () => [{courseId:'2042601000000000041', courseName:'体能', durationMinutes:55, capacity:8}],
    coachCreateSchedule: data => { calls.push(payload(data)); return new Promise(resolve => { finish = resolve }) },
    getCurrentPages: () => [{route:'pages/coach/schedule/index'}, {route:'pages/coach/schedule/create'}],
    uni: { showToast() {}, navigateBack: () => navigations.push('back') }
  })
  const schedules = mount('pages/coach/schedule/index.vue', 'mp', {
    coachScheduleList: async query => { navigations.push(payload(query)); return {rows:[],total:0} }
  })
  state.getOpenerEventChannel = () => ({emit: (event, value) => {
    assert.equal(event, 'scheduled')
    schedules.selectDate(value)
  }})
  await state.loadCourses()
  state.selectTime({detail:{value:'11:30'}})
  state.selectDate({detail:{value:'2000-01-01'}})
  await state.submitSchedule()
  assert.equal(calls.length, 0)
  state.selectDate({detail:{value:date}})
  assert.equal(state.endTime, '12:25')
  const pending = state.submitSchedule()
  await state.submitSchedule()
  assert.deepEqual(calls, [{courseId:'2042601000000000041', scheduleDate:date, startTime:'11:30'}])
  finish({scheduleId:'created'})
  await pending
  await schedules.loadData()
  assert.deepEqual(navigations, ['back', {scope:'all',scheduleDate:date,pageNum:1,pageSize:30}])
})

test('拒绝重叠排课后保留输入供修改，直接从工作台发布后查看该日课程', async () => {
  const routes = []
  let fail = true
  const state = mount('pages/coach/schedule/create.vue', 'h5', {
    coachScheduleCourses: async () => [{courseId:'10', durationMinutes:45}],
    coachCreateSchedule: async () => { if (fail) throw new Error('授课时间冲突') },
    getCurrentPages: () => [{route:'pages/coach/home/index'}, {route:'pages/coach/schedule/create'}],
    uni: { showToast() {}, redirectTo: ({url}) => routes.push(url) }
  })
  await state.loadCourses()
  state.selectDate({detail:{value:'2099-09-08'}})
  state.selectTime({detail:{value:'12:30'}})
  await state.submitSchedule()
  assert.equal(state.submitError, '授课时间冲突')
  assert.equal(state.startTime, '12:30')
  assert.equal(state.published, false)
  state.selectTime({detail:{value:'14:00'}})
  fail = false
  await state.submitSchedule()
  assert.deepEqual(routes, ['/pages/coach/schedule/index?date=2099-09-08'])
})
