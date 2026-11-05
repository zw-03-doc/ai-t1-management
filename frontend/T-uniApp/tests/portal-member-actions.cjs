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
  const result = { accessToken: 'member', clientId: 'app', memberInfo: { profileSetupStatus: profileStatus } }
  const uni = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    navigateTo: ({ url }) => routes.push(['push', url]),
    reLaunch: ({ url }) => routes.push(['reset', url]),
    login: ({ success }) => success({ code: 'wx-code' }),
    showToast() {}, showLoading() {}, hideLoading() {}
  }
  const script = portal.replace(/\/\/ #(ifdef|ifndef) MP-WEIXIN\r?\n([\s\S]*?)\/\/ #endif/g,
    (_, type, code) => (type === 'ifdef') === (platform === 'mp') ? code : '')
  const component = vm.runInNewContext(auth + '\n' + script.replace('export default', 'const component =') + '\ncomponent', {
    uni, readPortalSlogans: () => ({}), memberWechatLogin: async () => result, memberWebLogin: async () => result
  })
  const state = component.data()
  for (const [name, method] of Object.entries(component.methods)) state[name] = method.bind(state)
  return { state, routes, storage, result }
}

test('已登录会员可从同一首页反复进入两个不同标签，已有教练身份不会抢占会员入口', async () => {
  const { state, routes, storage, result } = openPortal('mp')
  storage.set('T1_MEMBER_SESSION', result)
  storage.set('T1_COACH_SESSION', { accessToken: 'coach', clientId: 'coach-client' })
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
  assert.deepEqual(routes, [['reset', '/pages/mine/profile?onboarding=1&target=' + encodeURIComponent(checkin)]])
})

test('浏览器取消签到登录后可重新选约课，登录后打开最新选择', async () => {
  const { state, routes } = openPortal('h5')
  await state.openMemberAction('checkin')
  assert.equal(state.browserRole, 'member')
  state.closeBrowserLogin()
  await state.openMemberAction('booking')
  assert.deepEqual(routes, [])
  await state.submitBrowserLogin()
  assert.deepEqual(routes, [['push', booking]])
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
