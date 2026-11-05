const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parse } = require('@vue/compiler-sfc')
const { compile } = require('@vue/compiler-dom')
const vue = require('vue')
const { renderToString } = require('@vue/server-renderer')

const root = path.resolve(__dirname, '..')
const pagePath = path.join(root, 'pages/portal/index.vue')
const descriptor = parse(fs.readFileSync(pagePath, 'utf8'), { filename: pagePath }).descriptor
const withoutImports = source => source.replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '')
const api = withoutImports(fs.readFileSync(path.join(root, 'api/portal-slogans.js'), 'utf8')).replace(/export /g, '')
const script = withoutImports(descriptor.script.content).replace('export default', 'const component =')
const display = {
  eyebrow: 'T1 TEAM', titleLead: '为更好的', titleAccent: '自己', titleSecondLine: '继续训练。',
  subtitle: '今天也出发', galleryCaption: '熟悉我们的训练空间', footerCaption: '明天再见'
}

function setup({ base = 'https://fitness.example/api', storage = new Map() } = {}) {
  const requests = []
  const context = {
    API_BASE_URL: base,
    hasMemberSession: () => false, hasCoachSession: () => false,
    uni: {
      getStorageSync: key => storage.get(key),
      setStorageSync: (key, value) => storage.set(key, value),
      request: options => requests.push(options)
    }
  }
  const spec = vm.runInNewContext(api + '\n' + script + '\ncomponent', context)
  const state = spec.data()
  for (const [name, method] of Object.entries(spec.methods)) state[name] = method.bind(state)
  return { state, requests, spec, storage }
}

function deliver(request, value) {
  request.success({ statusCode: 200, data: { code: 200, data: value } })
}

test('未登录即可更新标语，允许管理员清空橙色强调', async () => {
  const { state, requests } = setup()
  assert.equal(state.slogans.eyebrow, 'YOUR NEXT REP')
  const finished = state.refreshSlogans()
  assert.equal(requests[0].url, 'https://fitness.example/api/fitness/mini/portal/slogans')
  assert.equal(requests[0].method, 'GET')
  assert.equal(requests[0].header?.Authorization, undefined)
  deliver(requests[0], { ...display, titleAccent: '', updateTime: 'metadata' })
  await finished
  assert.equal(state.slogans.titleAccent, '')
  assert.equal(state.slogans.footerCaption, display.footerCaption)
  assert.equal(state.slogans.updateTime, undefined)
})

test('旧回包不能覆盖更新的文案，返回首页会再次发起读取', async () => {
  const { state, spec, requests, storage } = setup()
  spec.onShow.call(state)
  spec.onHide.call(state)
  spec.onShow.call(state)
  assert.equal(requests.length, 2)
  deliver(requests[1], display)
  deliver(requests[0], { ...display, titleLead: '过期' })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(state.slogans.titleLead, display.titleLead)
  assert.equal(setup({ storage }).state.slogans.titleLead, display.titleLead)
  assert.equal(setup({ base: 'http://127.0.0.1:18871', storage }).state.slogans.titleLead, '为下一次')
})

test('断网、未授权和残缺响应均保留完整的最近成功值', async () => {
  const { state, requests } = setup()
  const success = state.refreshSlogans()
  deliver(requests[0], display)
  await success
  for (const fail of [
    request => request.fail({ errMsg: 'timeout' }),
    request => request.success({ statusCode: 401, data: { code: 401 } }),
    request => deliver(request, { titleLead: '半份内容' }),
    request => deliver(request, { ...display, eyebrow: '   ' })
  ]) {
    const finished = state.refreshSlogans()
    fail(requests.at(-1))
    await finished
    assert.equal(state.slogans.titleLead, display.titleLead)
    assert.equal(state.slogans.eyebrow, display.eyebrow)
  }
})

test('离开后才完成的请求不会写入页面或缓存', async () => {
  const { state, spec, requests, storage } = setup()
  const finished = state.refreshSlogans()
  spec.onUnload.call(state)
  deliver(requests[0], display)
  await finished
  assert.equal(state.slogans.titleLead, '为下一次')
  assert.equal(storage.size, 0)
})

test('首页实际模板显示七段文案，强调同行、第二行独立，并转义管理员输入', async () => {
  const { state } = setup()
  const { code } = compile(descriptor.template.content, {
    mode: 'function', prefixIdentifiers: true, isCustomElement: () => true
  })
  const render = new Function('Vue', code)(vue)
  const app = vue.createSSRApp({ data: () => ({ ...state, slogans: { ...display, footerCaption: '<b>footer</b>' } }), render })
  app.config.warnHandler = () => {}
  const html = await renderToString(app)
  assert.match(html, /为更好的<text class="accent">自己<\/text><\/view><view>继续训练。<\/view>/)
  for (const text of [display.eyebrow, display.subtitle, display.galleryCaption, '&lt;b&gt;footer&lt;/b&gt;']) assert.ok(html.includes(text))
  assert.ok(html.includes('看看空间'))
  assert.ok(html.includes('教练工作台'))
})

test('微信小程序模板编译通过', () => {
  process.env.UNI_INPUT_DIR = root
  const errors = []
  require('@dcloudio/uni-mp-compiler').compile(descriptor.template.content, {
    filename: pagePath, mode: 'module', prefixIdentifiers: true, onError: error => errors.push(error.message)
  })
  assert.deepEqual(errors, [])
})
