const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parse } = require('@vue/compiler-sfc')

const root = path.resolve(__dirname, '..')
const read = name => fs.readFileSync(path.join(root, name), 'utf8')
const stripImports = source => source.replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '')
const portal = stripImports(parse(read('pages/portal/index.vue')).descriptor.script.content)
const auth = read('utils/auth.js').replace(/export /g, '')
const booking = '/pages/course/index?tab=schedule'
const checkin = '/pages/course/index?tab=reservation'

function openPortal(platform, profileStatus = 'completed') {
  const storage = new Map()
  const routes = []
  const requests = []
  const result = { accessToken: 'member', clientId: 'app', policyVersion: '2026-11-09', memberInfo: { profileSetupStatus: profileStatus } }
  const login = async data => { requests.push(data); return result }
  const uni = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    navigateTo: ({ url }) => routes.push(['push', url]),
    reLaunch: ({ url }) => routes.push(['reset', url]),
    login: ({ success }) => { requests.push('wx.login'); success({ code: 'wx-code' }) },
    showToast() {}, showLoading() {}, hideLoading() {}
  }
  const script = portal.replace(/\/\/ #(ifdef|ifndef) MP-WEIXIN\r?\n([\s\S]*?)\/\/ #endif/g,
    (_, type, code) => (type === 'ifdef') === (platform === 'mp') ? code : '')
  const component = vm.runInNewContext(auth + '\n' + script.replace('export default', 'const component =') + '\ncomponent', {
    uni, LoginConsent: {}, readVenuePhotos: () => [], refreshVenuePhotos: async () => [],
    memberWechatLogin: login, memberWebLogin: login, coachWechatLogin: login, coachPasswordLogin: login
  })
  const state = component.data()
  for (const [name, method] of Object.entries(component.methods)) state[name] = method.bind(state)
  return { state, routes, storage, result, requests, ready: () => component.onReady.call(state) }
}

test('已登录会员可从同一首页反复进入两个不同标签，已有教练身份不会抢占会员入口', async () => {
  const { state, routes, storage, result } = openPortal('mp')
  storage.set('T1_MEMBER_SESSION', result)
  storage.set('T1_COACH_SESSION', { accessToken: 'coach', clientId: 'coach-client', policyVersion: result.policyVersion })
  state.targetRole = 'coach'
  state.targetRoute = '/pages/coach/checkin/index'
  state.refreshLoginState()
  await state.openMemberAction('booking')
  await state.openMemberAction('checkin')
  await state.openCoach()
  assert.deepEqual(routes, [['push', booking], ['push', checkin], ['reset', state.targetRoute]])
})

test('小程序首次会员登录后，选填资料保留签到落点', async () => {
  const { state, routes } = openPortal('mp', 'pending')
  await state.openMemberAction('checkin')
  assert.deepEqual(routes, [])
  await state.agreeToLogin()
  assert.deepEqual(routes, [['reset', '/pages/mine/profile?onboarding=1&target=' + encodeURIComponent(checkin)]])
})

test('浏览器取消签到登录后可重新选约课，登录后打开最新选择', async () => {
  const { state, routes } = openPortal('h5')
  await state.openMemberAction('checkin')
  await state.agreeToLogin()
  assert.equal(state.browserRole, 'member')
  state.closeBrowserLogin()
  await state.openMemberAction('booking')
  assert.deepEqual(routes, [])
  await state.agreeToLogin()
  await state.submitBrowserLogin()
  assert.deepEqual(routes, [['push', booking]])
})

test('拒绝协议不会请求微信身份、消费登录码或导航，换身份后须重新同意', async () => {
  for (const platform of ['mp', 'h5']) {
    const { state, requests, routes } = openPortal(platform)
    await state.openMemberAction('booking')
    assert.equal(state.consentRole, 'member')
    state.declineLogin()
    await state.submitBrowserLogin()
    await state.doMemberLogin()
    assert.deepEqual(requests, [])
    assert.deepEqual(routes, [])
    await state.openCoach()
    assert.equal(state.consentRole, 'coach')
    assert.deepEqual(requests, [])
    await state.agreeToLogin()
    if (platform === 'h5') await state.submitBrowserLogin()
    assert.equal(requests.at(-1).consentVersion, '2026-11-09')
  }
})

test('旧版会员会话须重新同意，通过当前版本登录后再回到原预约', async () => {
  const { state, storage, result, requests, routes } = openPortal('mp')
  storage.set('T1_MEMBER_SESSION', { ...result, policyVersion: '2026-01-01' })
  state.refreshLoginState()
  assert.equal(state.memberLoggedIn, false)
  await state.openMemberAction('checkin')
  assert.equal(requests.length, 0)
  await state.agreeToLogin()
  assert.equal(storage.get('T1_MEMBER_SESSION').policyVersion, '2026-11-09')
  assert.deepEqual(routes, [['push', checkin]])
})

test('已登录用户从官网直接回到所选标签，普通首页和无效目标停在首页', () => {
  for (const [target, expected] of [
    [booking, booking], [checkin, checkin], ['', ''], ['/pages/portal/index', ''],
    ['/pages/coach/home/index', ''], ['/pages/training/index?role=coach', ''], ['https://example.com', '']
  ]) {
    const { state, storage, result, routes, ready } = openPortal('h5')
    storage.set('T1_MEMBER_SESSION', result)
    state.targetRoute = state.decodeTarget(encodeURIComponent(target))
    state.refreshLoginState()
    ready()
    assert.deepEqual(routes, expected ? [['reset', expected]] : [])
  }
})

test('进入后会话过期，重新登录仍可返回所选约课或签到标签', () => {
  const redirects = []
  for (const target of [booking, checkin]) {
    const tab = target.split('=')[1]
    const script = stripImports(read('utils/request.js')).replace(/export /g, '')
    const redirect = vm.runInNewContext(script + '\nredirectToPortal', {
      getCurrentPages: () => [{ route: 'pages/course/index', options: { tab } }],
      uni: { showToast() {}, reLaunch: ({ url }) => redirects.push(url) }, setTimeout: fn => fn()
    })
    redirect('member', '请重新登录')
    assert.equal(redirects.at(-1), '/pages/portal/index?role=member&target=' + encodeURIComponent(target))
  }
})
