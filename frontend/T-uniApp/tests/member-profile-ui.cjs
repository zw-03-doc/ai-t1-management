const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parse } = require('@vue/compiler-sfc')

const root = path.resolve(__dirname, '..')
const profileFile = path.join(root, 'pages/mine/profile.vue')
const avatarFile = path.join(root, 'components/MemberAvatar.vue')
const readSfc = file => parse(fs.readFileSync(file, 'utf8'), { filename: file }).descriptor
const profile = readSfc(profileFile)
const avatar = readSfc(avatarFile)
const serialized = value => JSON.parse(JSON.stringify(value))
const authSource = fs.readFileSync(path.join(root, 'utils/auth.js'), 'utf8').replace(/export /g, '')
const memberProfileTarget = vm.runInNewContext(authSource + '\nmemberProfileTarget')

function component(script, context) {
  return vm.runInNewContext(script.content.replace(/import\s+[^;\n]*?\s+from\s+['"][^'"]+['"];?/g, '')
    .replace('export default', 'const component =') + '\ncomponent', context)
}

function fixture(options = {}) {
  let session = { accessToken: 'test', clientId: 'test-client', memberInfo: { memberId: '41', profileSetupStatus: 'pending' } }
  const events = { saved: [], routes: [], uploads: [], toasts: [] }
  const ctx = {
    MemberAvatar: {}, MAX_AVATAR_BYTES: 5242880, memberProfileTarget,
    memberMineProfile: async () => ({ memberId: '41', profileSetupStatus: 'pending', mobile: '13800138000' }),
    memberUpdateProfile: async value => { events.saved.push(serialized(value)) },
    uploadMemberAvatar: async value => { events.uploads.push(value); return '/fitness/member/avatars/new.jpg' },
    getMemberSession: () => session,
    saveMemberSession: value => { session = value },
    uni: {
      reLaunch: ({ url }) => events.routes.push(url),
      showToast: value => events.toasts.push(value.title),
      chooseImage: opts => {
        opts.success({ tempFilePaths: ['local-file'], tempFiles: [{ size: 1000 }] })
        opts.complete()
      }
    },
    ...options
  }
  const spec = component(profile.script, ctx)
  const state = { ...spec.data(), onboarding: true, memberId: '41' }
  for (const [name, method] of Object.entries(spec.methods)) state[name] = method.bind(state)
  for (const [name, method] of Object.entries(spec.computed)) Object.defineProperty(state, name, { get: method.bind(state) })
  return { state, events, ctx, session: () => session }
}

test('跳过不上传所选照片、不提交输入、不改变验证绑定；下次登录记为已跳过', async () => {
  const { state, events, session } = fixture()
  await state.loadProfile()
  state.form.nickname = '未保存昵称'
  state.form.contactMobile = 'not a number'
  state.chooseAvatar()
  await state.skipProfile()
  assert.deepEqual(events.saved, [{ profileSetupAction: 'skip' }])
  assert.deepEqual(events.uploads, [])
  assert.equal(session().memberInfo.profileSetupStatus, 'skipped')
  assert.deepEqual(events.routes, ['/pages/index/index'])
})

test('不填写任何资料也能完成首登并回到原业务页面', async () => {
  const { state, events } = fixture()
  state.target = memberProfileTarget(encodeURIComponent('/pages/course/detail?id=15'))
  await state.loadProfile()
  await state.submitProfile()
  assert.deepEqual(events.saved, [{ profileSetupAction: 'complete' }])
  assert.deepEqual(events.routes, ['/pages/course/detail?id=15'])
})

test('手机号校验只针对手填的联系电话', async () => {
  const { state, events } = fixture()
  await state.loadProfile()
  state.form.contactMobile = '1'
  await state.submitProfile()
  assert.equal(events.saved.length, 0)
  assert.match(state.saveError, /手机号/)
  state.form.contactMobile = '+14155550123'
  await state.submitProfile()
  assert.deepEqual(events.saved, [{ contactMobile: '+14155550123', profileSetupAction: 'complete' }])
})

test('保存失败保留草稿和待绑定头像，可重试且仅上传一次', async () => {
  const { state, events, ctx, session } = fixture()
  await state.loadProfile()
  state.form.nickname = '  小林  '
  state.chooseAvatar()
  const save = ctx.memberUpdateProfile
  ctx.memberUpdateProfile = async () => { throw new Error('连接失败') }
  await state.submitProfile()
  assert.equal(state.form.nickname, '  小林  ')
  assert.equal(state.avatarFile, 'local-file')
  assert.equal(events.routes.length, 0)
  assert.equal(session().memberInfo.profileSetupStatus, 'pending')
  ctx.memberUpdateProfile = save
  await state.submitProfile()
  assert.equal(events.uploads.length, 1)
  assert.deepEqual(events.saved, [{ nickname: '小林', avatarUrl: '/fitness/member/avatars/new.jpg', profileSetupAction: 'complete' }])
  assert.equal(session().memberInfo.nickname, '小林')
})

test('普通资料编辑可以清空联系电话和生日，同时保留未改的头像、绑定手机号与姓名', async () => {
  const { state, events } = fixture({
    memberMineProfile: async () => ({
      memberId: '41', profileSetupStatus: 'completed', nickname: '会员乙', realName: '李青',
      avatarUrl: 'https://images.example.test/profile.jpg', contactMobile: '+14155550123', mobile: '13800138000', birthday: '2000-11-06'
    })
  })
  state.onboarding = false
  await state.loadProfile()
  state.form.contactMobile = ''
  state.form.birthday = ''
  await state.submitProfile()
  assert.deepEqual(events.saved, [{ contactMobile: '', clearBirthday: true }])
  assert.equal(events.routes.length, 0)
})

test('读取失败时拒绝用空白表单覆盖会员资料，仍可明确跳过', async () => {
  const { state, events } = fixture({ memberMineProfile: async () => { throw new Error('网络异常') } })
  await state.loadProfile()
  await state.submitProfile()
  assert.equal(events.saved.length, 0)
  await state.skipProfile()
  assert.deepEqual(events.saved, [{ profileSetupAction: 'skip' }])
})

test('首登回跳仅允许会员业务页', () => {
  for (const target of ['https://untrusted.test', '/pages/mine/profile?onboarding=1', '/pages/coach/home/index',
    '/pages/training/index?role=coach', '/pages/training/index?role=%63oach', '/pages/portal/index']) {
    assert.equal(memberProfileTarget(target), '/pages/index/index')
  }
})

test('旧会员头像下载回包晚于新选择时，释放旧图片且保留新选择', async () => {
  let resolve
  const released = []
  const spec = component(avatar.script, {
    MEMBER_AVATAR_PREFIX: '/fitness/member/avatars/', downloadMemberAvatar: () => new Promise(done => { resolve = done }),
    releaseMemberAvatar: url => { if (url) released.push(url) }
  })
  const state = spec.data()
  const update = spec.watch.source.handler.bind(state)
  const first = update('/fitness/member/avatars/a.jpg')
  await update('temporary-preview-file')
  resolve('blob:outdated')
  await first
  assert.equal(state.displayUrl, 'temporary-preview-file')
  assert.deepEqual(released, ['blob:outdated'])
})

test('资料页和头像组件可编译为微信小程序模板', () => {
  process.env.UNI_INPUT_DIR = root
  const errors = []
  for (const [descriptor, filename] of [[profile, profileFile], [avatar, avatarFile]]) {
    require('@dcloudio/uni-mp-compiler').compile(descriptor.template.content, {
      filename, mode: 'module', prefixIdentifiers: true, onError: error => errors.push(error.message)
    })
  }
  assert.deepEqual(errors, [])
})
