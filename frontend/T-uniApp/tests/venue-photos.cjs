const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const source = fs.readFileSync(path.join(__dirname, '../utils/venue-photos.js'), 'utf8')
  .replace(/^import .*/m, '').replace(/\/\/ #ifdef H5[\s\S]*?\/\/ #endif/, '').replace(/export /g, '')
function client(storage, reply) {
  return vm.runInNewContext(source + '\n;({ readVenuePhotos, refreshVenuePhotos })', {
    API_BASE_URL: 'https://api.example.test', DEFAULT_VENUE_STORE_ID: '',
    uni: {
      getStorageSync: k => storage.get(k),
      setStorageSync: (k, v) => storage.set(k, v),
      request: options => reply(options)
    }
  })
}

test('删除全部场馆照片后，下次离线进入仍显示空相册', async () => {
  const storage = new Map()
  const first = client(storage, r => r.success({statusCode: 200, data: { code: 200, data: { version: 3, photos: [] } }}))
  assert.equal(first.readVenuePhotos().length, 3)
  assert.equal((await first.refreshVenuePhotos()).length, 0)
  const offline = client(storage, r => r.fail())
  assert.equal(offline.readVenuePhotos().length, 0)
  assert.equal((await offline.refreshVenuePhotos()).length, 0)
})

test('首页和相册共享同一次请求，保留管理员排序、本机预置图和同源上传图', async () => {
  const storage = new Map()
  let request, calls = 0
  const c = client(storage, r => { calls++; request = r })
  const id = '5f0b8567-26a8-4d6c-b7fb-339768146754'
  const pending = c.refreshVenuePhotos()
  assert.equal(c.refreshVenuePhotos(), pending)
  request.success({statusCode: 200, data: {code: 200, data: {version: 2, photos: [
    {id, url: '/fitness/mini/portal/venue/images/' + id}, {id:'live-2',url:'/app/static/images/live-2.jpg'}
  ]}}})
  const photos = await pending
  assert.equal(calls, 1)
  assert.equal(photos[0].src, 'https://api.example.test/fitness/mini/portal/venue/images/' + id)
  assert.equal(photos[1].src, '/static/images/live-2.jpg')
})

test('不同门店的请求、相册缓存及图片地址相互隔离', async () => {
  const requests = [], storage = new Map()
  const c = client(storage, r => requests.push(r))
  const a = '2119000000000000001', b = '2119000000000000002'
  assert.equal(c.readVenuePhotos(b).length, 0)
  const first = c.refreshVenuePhotos(a), second = c.refreshVenuePhotos(b)
  assert.notEqual(first, second)
  assert(requests[0].url.endsWith('?storeId=' + a))
  const id = '5f0b8567-26a8-4d6c-b7fb-339768146754'
  const url = '/fitness/mini/portal/venue/images/' + id + '?storeId=' + a
  requests[1].success({statusCode:200,data:{code:200,data:{storeId:b,version:0,photos:[]}}})
  requests[0].success({statusCode:200,data:{code:200,data:{storeId:a,version:1,photos:[{id,url}]}}})
  assert.equal((await first)[0].src, 'https://api.example.test'+url)
  assert.equal((await second).length, 0)
  const offline = client(storage, r => r.fail())
  assert.equal((await offline.refreshVenuePhotos(a)).length, 1)
  assert.equal((await offline.refreshVenuePhotos(b)).length, 0)
  const gone = c.refreshVenuePhotos(a)
  requests[2].success({statusCode:200,data:{code:404}})
  assert.equal((await gone).length, 0)
  assert.equal(client(storage, r => r.fail()).readVenuePhotos(a).length, 0)
})
