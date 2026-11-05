// Offline regression harness. No network, real login, storage or production writes.
// Run from any directory: npm run test:training-ui
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const assert = require('assert/strict');
const repo = path.resolve(__dirname, '../../..');
const frontend = path.join(repo, 'frontend/T-uniApp');
const dep = name => require(path.join(frontend, 'node_modules', name));
const sfc = dep('@vue/compiler-sfc');
const dom = dep('@vue/compiler-dom');
const vue = dep('vue');
const { renderToString } = dep('@vue/server-renderer');
const sourcePath = path.join(frontend, 'pages/training/index.vue');
const source = fs.readFileSync(sourcePath, 'utf8');
const descriptor = sfc.parse(source, { filename: sourcePath }).descriptor;
const script = descriptor.script.content
  .replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '')
  .replace('export default', 'globalThis.component =');
const librarySource = fs.readFileSync(path.join(frontend, 'utils/exercise-library.js'), 'utf8')
  .replace(/export /g, '') + '\nglobalThis.library={EXERCISES,MUSCLE_FILTERS,EQUIPMENT_FILTERS,searchExercises};';
const libraryContext = {};
vm.createContext(libraryContext);
vm.runInContext(librarySource, libraryContext);
const library = libraryContext.library;
const copy = value => JSON.parse(JSON.stringify(value));

function setup(api = async () => []) {
  let now = Date.parse('2026-10-26T08:00:00Z');
  let sequence = 0;
  const tickers = new Map();
  const events = [];
  class TestDate extends Date {
    constructor(...args) { super(...(args.length ? args : [now])); }
    static now() { return now; }
  }
  const ctx = {
    console, T1Nav: {}, Date: TestDate, Math, Set, Number, JSON, ...library,
    URL: { revokeObjectURL: url => events.push(['revoke', url]) },
    hasMemberSession: () => true, hasCoachSession: () => true,
    trainingApi: api,
    uploadTrainingMedia: async () => ({ url: '/fitness/training/media/offline' }),
    resolveTrainingMedia: async url => 'blob:' + url,
    setInterval: fn => { const id = ++sequence; tickers.set(id, fn); return id; },
    clearInterval: id => tickers.delete(id),
    uni: {
      showToast: options => events.push(['toast', options.title]),
      showModal: options => { events.push(['modal', options.title]); options.success({ confirm: true }); },
      pageScrollTo() {},
      navigateTo: options => events.push(['navigate', options.url]),
      reLaunch: options => events.push(['navigate', options.url]),
      vibrateShort: () => events.push(['vibrate'])
    }
  };
  vm.createContext(ctx);
  vm.runInContext(script, ctx);
  const state = ctx.component.data();
  for (const [name, fn] of Object.entries(ctx.component.methods)) state[name] = fn.bind(state);
  for (const [name, fn] of Object.entries(ctx.component.computed))
    Object.defineProperty(state, name, { get: () => fn.call(state) });
  state.$nextTick = fn => fn();
  return { state, ctx, events, tickers, advance: ms => { now += ms; } };
}

const exercise = {
  key: 'bench', name: '卧推', instructions: '离线测试说明',
  imageUrls: ['/fitness/training/media/offline-image'],
  videoUrl: '/fitness/training/media/offline-video',
  targets: [
    { setType: 'warmup', reps: 10, weight: 20, unit: 'kg' },
    { setType: 'working', reps: 8, weight: 50, unit: 'lb' },
    { setType: 'working', reps: 8, weight: 50, unit: 'lb' }
  ]
};
const workout = {
  workoutId: 'offline-workout', plannedDate: '2026-10-26', status: 'planned',
  title: '推', exercises: [exercise], sets: []
};
const template = {
  planId: 'offline-public-plan', templateId: 'offline-public-plan', status: 'template',
  memberId: null, name: '公开模板', visibility: 'public', cycleDays: 1,
  days: [{ dayNumber: 1, title: '推', restDay: false, exercises: [exercise] }]
};
const validEditor = () => ({
  name: '离线新计划', visibility: 'private', cycleDays: 1,
  days: [{ dayNumber: 1, title: '推', restDay: false, exercises: [copy(exercise)] }]
});

function findChartWeight(node) {
  if (node.type === 1 && node.props.some(p => p.name === 'class' && p.value?.content === 'chart-weight'))
    return node.loc.source;
  for (const child of node.children || []) {
    const match = findChartWeight(child);
    if (match) return match;
  }
}
const chartSnippet = findChartWeight(dom.parse(descriptor.template.content));
assert(chartSnippet, 'Actual chart weight template node must exist');
const compiledWeight = dom.compile(chartSnippet, { mode: 'function', prefixIdentifiers: true, isCustomElement: () => true });
const weightRender = new Function('Vue', compiledWeight.code)(vue);
async function renderWeight(value) {
  const state = setup().state;
  const app = vue.createSSRApp({
    data: () => ({ point: { maxWorkingWeightKg: value } }),
    methods: { formatWeight: state.formatWeight }, render: weightRender
  });
  app.config.warnHandler = () => {};
  return (await renderToString(app)).replace(/<!--[\s\S]*?-->/g, '').replace(/<[^>]+>/g, '').replace(/\s/g, '');
}

const cases = [];
const test = (name, fn) => cases.push({ name, fn });
for (const platform of ['MP-WEIXIN', 'H5']) {
  test(platform + ' accepts long videos at 50 MiB, enforces exact file-size boundaries, and handles cancel', async () => {
    const media = fs.readFileSync(path.join(frontend, 'utils/training-media.js'), 'utf8')
      .replace(/\/\/ #ifdef ([\w-]+)\r?\n([\s\S]*?)\/\/ #endif/g, (_, target, body) => target === platform ? body : '')
      .replace('export function selectTrainingMedia', 'function selectTrainingMedia');
    let options;
    const pick = value => { options = value; };
    const uni = platform === 'H5' ? { chooseFile: pick, chooseImage: pick } : { chooseMedia: pick, chooseImage: pick };
    const select = vm.runInNewContext(media + '\nselectTrainingMedia;', { uni });
    const mib = 1024 * 1024;
    const selected = (type, size, duration) => {
      const promise = select(type);
      if (type === 'video') {
        assert.deepEqual(Array.from(options.sourceType), ['album']);
        assert.equal(options.maxDuration, undefined);
        assert.equal(options.type || options.mediaType[0], 'video');
      }
      const video = platform === 'MP-WEIXIN' && type === 'video';
      const file = { [video ? 'tempFilePath' : 'path']: 'local/file', size, duration };
      options.success({ tempFiles: [file] });
      return promise.then(value => {
        assert.equal(value, platform === 'H5' ? file : 'local/file');
        return true;
      });
    };
    await selected('video', 50 * mib, 3600);
    await selected('video', 2000);
    await assert.rejects(selected('video', 50 * mib + 1, 3), /50 MB/);
    await selected('image', 10 * mib);
    await assert.rejects(selected('image', 10 * mib + 1), /10 MB/);
    await assert.rejects(selected('image', undefined), /无法读取/);
    await assert.rejects(selected('video', 0), /无法读取/);
    const cancel = select('video'); options.fail({ errMsg: 'chooseMedia:fail cancel' }); assert.equal(await cancel, null);
  });
}
test('a new plan opens the picker on tap and only saves after a valid selection', async () => {
  const { ctx, state } = setup();
  state.editor = validEditor();
  const ex = state.editor.days[0].exercises[0];
  const steps = [];
  ctx.selectTrainingMedia = async () => { steps.push('picker'); return null; };
  state.persistEditor = async () => { steps.push('save'); state.editor.planId = 'offline-plan'; };
  ctx.uploadTrainingMedia = async (path, id) => { steps.push([path, id]); return { url: 'offline-video' }; };
  await state.upload(ex, 'video');
  assert.deepEqual(steps, ['picker']);
  ctx.selectTrainingMedia = async () => { steps.push('picker'); return 'selected-file'; };
  await state.upload(ex, 'video');
  assert.deepEqual(steps, ['picker', 'picker', 'save', ['selected-file', 'offline-plan']]);
  assert.equal(ex.videoUrl, 'offline-video');
  assert.equal(state.mediaViews['offline-video'], 'selected-file');
  assert.equal(state.busy, false);
});
test('new browser media uses the original file, falls back once, and only saves the protected URL', async () => {
  const { ctx, state, events } = setup();
  state.editor = validEditor();
  state.editor.planId = 'offline-plan';
  const ex = state.editor.days[0].exercises[0];
  const file = {};
  ctx.selectTrainingMedia = async () => file;
  ctx.URL.createObjectURL = chosen => { assert.equal(chosen, file); return 'blob:local'; };
  const resource = '/fitness/training/media/accepted';
  ctx.uploadTrainingMedia = async () => ({ url: resource });
  let reads = 0;
  ctx.resolveTrainingMedia = async () => { ++reads; return 'blob:authenticated'; };
  await state.upload(ex, 'video');
  assert.equal(reads, 0);
  assert.equal(state.mediaViews[resource], 'blob:local');
  assert.equal(state.planPayload().days[0].exercises[0].videoUrl, resource);
  await state.mediaPreviewFailed(resource);
  assert.equal(reads, 1);
  assert.equal(state.mediaViews[resource], 'blob:authenticated');
  await state.mediaPreviewFailed(resource);
  assert.equal(reads, 1);
  assert.equal(state.mediaFailed[resource], true);
  assert.deepEqual(events.filter(e => e[0] === 'revoke'), [['revoke','blob:local'], ['revoke','blob:authenticated']]);
});
test('exercise library has 24 unique keys and combined filters work', () => {
  assert.equal(library.EXERCISES.length, 24);
  assert.equal(new Set(library.EXERCISES.map(x => x.key)).size, 24);
  assert(library.searchExercises('卧推', '胸部', '杠铃').some(x => x.key === 'bench'));
  assert.equal(library.searchExercises('卧推', '腿部', '杠铃').length, 0);
});
test('opening targets never pre-fills actual weight or completed flags', async () => {
  const { state } = setup();
  await state.openLog(workout);
  assert(state.logSets.every(set => set.weight === '' && !set.done));
  assert.equal(state.logSets[1].targetWeight, 50);
  assert.equal(state.logSets[1].targetUnit, 'lb');
});
test('actual blank weight is rejected but bodyweight zero is valid', () => {
  const { state } = setup();
  const set = { setType: 'working', reps: 10, weight: '', unit: 'kg', rpe: '' };
  assert.throws(() => state.validateSet(set, true));
  assert.equal(state.validateSet({ ...set, weight: 0 }, true).weight, 0);
});
test('saveLog submits only checked actual sets and preserves warmup/working plus kg/lb', async () => {
  const writes = [];
  const { state, tickers } = setup(async (url, method, data, role) => { if (method === 'PUT') writes.push({ url, method, data, role }); return { planId: 'offline-plan', status: 'active' }; });
  state.selected = { planId: 'offline-plan', startDate: '2026-10-01' };
  state.refreshPlan = async () => {};
  await state.openLog(workout);
  state.logSets[0].weight = 0; state.completeSet(state.logSets[0]);
  state.logSets[1].weight = 55.5; state.completeSet(state.logSets[1]);
  await state.saveLog();
  assert.equal(writes.length, 1);
  const sets = writes[0].data.sets;
  assert.equal(sets.length, 2);
  assert.equal(sets[0].setType, 'warmup'); assert.equal(sets[0].weight, 0); assert.equal(sets[0].unit, 'kg');
  assert.equal(sets[1].setType, 'working'); assert.equal(sets[1].weight, 55.5); assert.equal(sets[1].unit, 'lb');
  assert(!('done' in sets[0]) && !('localId' in sets[0]));
  assert.equal(state.logging, null); assert.equal(tickers.size, 0);
});
test('saved records reopen as actual values rather than targets', async () => {
  const { state } = setup();
  await state.openLog({ ...workout, status: 'completed', performedDate: '2026-10-25',
    sets: [{ exerciseKey: 'bench', exerciseName: '卧推', setType: 'working', reps: 9, weight: 62.5, unit: 'lb' }] });
  assert.equal(state.logSets[0].done, true); assert.equal(state.logSets[0].weight, 62.5);
  assert.equal(state.logSets[0].unit, 'lb'); assert.equal(state.performedDate, '2026-10-25');
});
test('reuse previous fills only unfinished working sets and retains their units', async () => {
  const { state } = setup();
  await state.openLog(workout);
  state.logSets[0].weight = 10;
  state.logSets[1].weight = 20; state.logSets[1].done = true;
  state.workouts = [{ workoutId: 'previous', status: 'completed', plannedDate: '2026-10-20', performedDate: '2026-10-20',
    sets: [{ exerciseKey: 'bench', setType: 'working', reps: 6, weight: 100, unit: 'lb' }] }];
  await state.usePrevious(exercise);
  assert.equal(state.logSets[0].weight, 10); assert.equal(state.logSets[1].weight, 20);
  assert.equal(state.logSets[2].weight, 100); assert.equal(state.logSets[2].unit, 'lb');
  assert.equal(state.logSets[2].done, false);
});
test('public template becomes private with coach memberId and original protected media references', async () => {
  const { state } = setup(); state.role = 'coach'; state.memberId = 'offline-member';
  await state.copyTemplate(template);
  const payload = state.planPayload();
  assert.equal(payload.memberId, 'offline-member'); assert.equal(payload.visibility, 'private');
  assert.equal(payload.days[0].exercises[0].imageUrls[0], exercise.imageUrls[0]);
  assert.equal(payload.days[0].exercises[0].videoUrl, exercise.videoUrl);
  assert(!state.editor.planId); assert.equal(template.visibility, 'public');
  state.editor.days[0].exercises[0].imageUrls.push('offline-change');
  assert.equal(template.days[0].exercises[0].imageUrls.length, 1);
});
test('guide queries select both three-split and five-split editors', async () => {
  for (const key of ['three-split', 'five-split']) {
    const { state, ctx } = setup(async url => url === '/templates' ? [{ ...template, templateId: key }] : []);
    ctx.component.onLoad.call(state, { template: key });
    for (let i = 0; i < 8; i++) await Promise.resolve();
    assert(state.editor); assert.equal(state.pendingTemplate, ''); assert.equal(state.editor.visibility, 'private');
  }
});
test('rest timer stops ticking while hidden, resumes by deadline and cleans up on unload', () => {
  const { state, ctx, tickers, events, advance } = setup(); state._loaded = true; state.authenticated = true;
  state.startRestTimer(); assert.equal(tickers.size, 1);
  ctx.component.onHide.call(state); assert.equal(tickers.size, 0); assert.equal(state.restRunning, true);
  advance(30000); ctx.component.onShow.call(state); assert.equal(state.restRemaining, 60); assert.equal(tickers.size, 1);
  advance(61000); state.tickRest(); assert.equal(state.restRemaining, 0); assert.equal(state.restFinished, true);
  assert.equal(tickers.size, 0); assert.equal(events.filter(e => e[0] === 'vibrate').length, 1);
  state.mediaViews = { cached: 'blob:cached' }; ctx.component.onUnload.call(state);
  assert.equal(events.filter(e => e[0] === 'revoke').length, 1);
});
test('media resolving after unload is revoked and never published to the view', async () => {
  const { state, ctx, events } = setup(); let resolve;
  ctx.resolveTrainingMedia = () => new Promise(done => { resolve = done; });
  const promise = state.ensureMedia('/fitness/training/media/delayed');
  ctx.component.onUnload.call(state); resolve('blob:delayed'); await promise;
  assert.equal(Object.keys(state.mediaViews).length, 0);
  assert(events.some(e => e[0] === 'revoke' && e[1] === 'blob:delayed'));
});
test('busy back/cancel/close cannot discard editor or log during save', async () => {
  let settle;
  const { state, ctx, events } = setup(() => new Promise(resolve => { settle = resolve; }));
  state.editor = validEditor(); const original = state.editor; state.logging = { workoutId: 'offline' }; state.busy = true;
  const save = state.persistEditor();
  assert.equal(ctx.component.onBackPress.call(state), true);
  await state.cancelEditor(); await state.closeLog();
  assert.equal(state.editor, original); assert(state.logging);
  assert.equal(events.filter(e => e[0] === 'modal').length, 0);
  settle({ planId: 'saved-offline', status: 'draft' }); await save;
  assert.equal(state.editor.planId, 'saved-offline');
});
test('persistEditor retains its original object across asynchronous response', async () => {
  let settle; const { state } = setup(() => new Promise(resolve => { settle = resolve; }));
  state.editor = validEditor(); const original = state.editor; const save = state.persistEditor();
  state.editor = null; settle({ planId: 'saved-captured', status: 'draft' });
  await save; assert.equal(original.planId, 'saved-captured'); assert.equal(state.editor, null);
});
test('actual chart template renders warmup-only null as no working sets', async () => {
  assert.equal(await renderWeight(null), '暂无正式组');
});
test('actual chart template still renders a real bodyweight working set as zero kg', async () => {
  assert.equal(await renderWeight(0), '0kg');
});
test('chart uses server-normalized kg without double converting lb', async () => {
  assert.equal(await renderWeight(25.174376535), '25.17kg');
  const { state } = setup();
  state.points = [{ exerciseKey: 'bench', exerciseName: '卧推', maxWorkingWeightKg: 25.174376535 }];
  assert.equal(state.barWidth(state.points[0]), 100);
});
test('full training template passes the uni-app mini-program compiler', () => {
  process.env.UNI_INPUT_DIR = frontend;
  const errors = [];
  dep('@dcloudio/uni-mp-compiler').compile(descriptor.template.content, {
    filename: sourcePath, mode: 'module', prefixIdentifiers: true, onError: error => errors.push(error)
  });
  assert.equal(errors.length, 0, errors.map(e => e.message).join('; '));
});

test('saved in-progress target positions restore remaining groups without counting them as actual', async () => {
  const { state } = setup();
  await state.openLog({ ...workout, status: 'in_progress', sets: [
    { exerciseKey: 'bench', exerciseName: '卧推', targetSetIndex: 0, setType: 'warmup', reps: 12, weight: 22, unit: 'kg' },
    { exerciseKey: 'bench', exerciseName: '卧推', targetSetIndex: null, setType: 'working', reps: 6, weight: 45, unit: 'lb' }
  ] });
  assert.equal(state.logSets.length, 4);
  assert.equal(state.logSets[0].weight, 22); assert.equal(state.logSets[0].done, true);
  assert.equal(state.logSets[1].weight, ''); assert.equal(state.logSets[1].done, false);
  assert.equal(state.logSets[1].targetSetIndex, 1); assert.equal(state.logSets[2].targetSetIndex, 2);
  assert.equal(state.logSets[3].weight, 45); assert.equal(state.logSets[3].targetSetIndex, null);
});

test('saving progress keeps the workout open, sends finish false, and does not claim final statistics', async () => {
  const writes = [];
  const { state } = setup(async (url, method, data) => {
    if (method === 'PUT') { writes.push(data); return { sets: data.sets, status: 'in_progress' }; }
    return { planId: 'offline-plan', status: 'active' };
  });
  state.selected = { planId: 'offline-plan', status: 'active', startDate: '2026-10-01' };
  state.refreshPlan = async () => {};
  await state.openLog(workout); state.logSets[0].weight = 20; state.completeSet(state.logSets[0]);
  await state.saveLog(false);
  assert.equal(writes[0].finish, false); assert.equal(writes[0].sets[0].targetSetIndex, 0);
  assert.equal(state.logging.status, 'in_progress'); assert.equal(state.logSets.length, 3);
  assert.equal(state.logSets[1].done, false);
});

test('shortening a cycle requires consent and restoration keeps edits to retained days', async () => {
  const { state, ctx, events } = setup(); state.newPlan();
  state.editor.days[6] = { dayNumber: 7, title: '已配置腿日', restDay: false, exercises: [copy(exercise)] };
  state.editor.cycleDays = 6;
  ctx.uni.showModal = options => { events.push(['modal', options.title]); options.success({ confirm: false }); };
  await state.changeCycleDays(); assert.equal(state.editor.days.length, 7); assert.equal(state.editor.cycleDays, 7);
  ctx.uni.showModal = options => options.success({ confirm: true });
  state.editor.cycleDays = 6; await state.changeCycleDays();
  assert.equal(state.editor.days.length, 6); assert.equal(state.cycleBackup[6].exercises.length, 1);
  state.editor.days[0].title = '保留的新修改'; state.restoreCycleDays();
  assert.equal(state.editor.cycleDays, 7); assert.equal(state.editor.days[6].title, '已配置腿日');
  assert.equal(state.editor.days[0].title, '保留的新修改');
});

test('changing the shift date previews and submits chosen days with the preview version', async () => {
  const calls = [];
  const { state } = setup(async (url, method, data) => {
    calls.push({ url, method, data });
    if (url.endsWith('/preview')) return { fromDate: data.fromDate, days: data.days, version: 4, canApply: true, affectedCount: 1, changes: [] };
    return [];
  });
  state.selected = { planId: 'offline-plan', status: 'active' }; state.refreshPlan = async () => {};
  state.shifting = copy(workout); state.shiftDate = '2026-10-29';
  await state.previewShift(); await state.applyShift();
  const write = calls.find(call => call.method === 'POST');
  assert.equal(write.data.days, 3); assert.equal(write.data.expectedVersion, 4);
  assert.equal(state.shifting, null);
});

test('archived workouts are read-only and can never submit actual corrections', async () => {
  const calls = []; const { state } = setup(async (...args) => calls.push(args));
  state.selected = { status: 'archived' };
  await state.openLog({ ...workout, status: 'completed', sets: [{ exerciseKey: 'bench', setType: 'working', weight: 50, reps: 8, unit: 'kg' }] });
  state.completeSet(state.logSets[0]); await state.saveLog(true); state.addLogSet(exercise, 'working');
  assert.equal(state.logSets[0].done, true); assert.equal(state.logSets.length, 1); assert.equal(calls.length, 0);
});

test('admin A/B member requests cannot display stale plans under the new owner', async () => {
  const ts = require(path.join(repo, 'frontend/T-plus-ui/node_modules/typescript'));
  const adminPath = path.join(repo, 'frontend/T-plus-ui/src/views/fitness/training/index.vue');
  const admin = sfc.parse(fs.readFileSync(adminPath, 'utf8')).descriptor.scriptSetup.content;
  const adminScript = ts.transpileModule(admin.replace(/^import.*;$/gm, '').replace('import.meta.env.VITE_MEMBER_APP_URL', 'undefined'), {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.None }
  }).outputText + '\nglobalThis.api={load,detail,memberId,plans,context,workouts,visible};';
  const waiting = new Map();
  const ctx = { ref: value => ({ value }), computed: getter => ({ get value(){ return getter(); } }), useRoute: () => ({query:{}}),
    watch() {}, onBeforeUnmount() {}, loadMemberOptions: async () => [],
    request: options => new Promise(resolve => waiting.set(options.url + ':' + options.params.memberId, resolve)) };
  vm.createContext(ctx); vm.runInContext(adminScript, ctx);
  ctx.api.memberId.value = 'A'; const first = ctx.api.load();
  ctx.api.memberId.value = 'B'; const second = ctx.api.load();
  waiting.get('/fitness/training/plans:B')({data:[{name:'B计划'}]}); waiting.get('/fitness/training/context:B')({data:{displayName:'B会员'}}); await second;
  waiting.get('/fitness/training/plans:A')({data:[{name:'A计划'}]}); waiting.get('/fitness/training/context:A')({data:{displayName:'A会员'}}); await first;
  assert.equal(ctx.api.memberId.value, 'B'); assert.equal(ctx.api.plans.value[0].name, 'B计划'); assert.equal(ctx.api.context.value.displayName, 'B会员');
});

(async () => {
  let passed = 0;
  for (const { name, fn } of cases) {
    await fn(); passed++; console.log(`PASS ${passed}. ${name}`);
  }
  console.log(`RESULT ${passed}/${cases.length} passed; 0 failed. Offline only.`);
})().catch(error => { console.error('FAIL', error); process.exitCode = 1; });
