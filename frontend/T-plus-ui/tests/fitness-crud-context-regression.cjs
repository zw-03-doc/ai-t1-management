// Runs the actual Vue scripts with in-memory responses. No network or business writes.
// Run: node tests/fitness-crud-context-regression.cjs
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const assert = require('node:assert/strict');
const ts = require('typescript');
const sfc = require('@vue/compiler-sfc');
const root = path.resolve(__dirname, '..');

function script(file, exposed) {
  const filename = path.join(root, file);
  const { descriptor, errors } = sfc.parse(fs.readFileSync(filename, 'utf8'), { filename });
  assert.equal(errors.length, 0);
  const source = descriptor.scriptSetup.content.replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '');
  return ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.None }
  }).outputText + `\nglobalThis.api={${exposed}};`;
}

const crudFile = 'src/views/fitness/components/FitnessCrudPage.vue';
const crudScript = script(crudFile,
  'queryParams,form,dialog,clearContext,handleUpdate,handleAdd,cancel,closeDialog,reset,formRef,submitForm,loadRemoteOptions,remoteOptionsMap,remoteLoadingMap,remoteMoreMap,onFilterChange,onTextInput,handleQuery,setDateStart');
const copy = value => JSON.parse(JSON.stringify(value));
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done; }); return { promise, resolve }; };
const base = {
  ref: value => ({ value }), reactive: value => value,
  computed: getter => ({ get value() { return getter(); } }),
  getCurrentInstance: () => ({ proxy: { $modal: { msgSuccess() {} } } })
};

function setup(overrides = {}, query = {}) {
  const watchers = [], unmount = [], lists = [];
  const route = { path: '/fitness-course/reservation', query };
  const props = {
    title: '预约', idKey: 'reservationId', queryFields: [], formFields: [],
    initialQuery: { pageNum: 1, pageSize: 10, scheduleId: undefined },
    initialForm: { reservationId: undefined, memberId: undefined, scheduleId: undefined },
    listFn: async value => { lists.push(copy(value)); return { rows: [], total: 0 }; },
    getFn: async id => ({ data: { reservationId: id } }),
    addFn: async () => {}, updateFn: async () => {}, ...overrides
  };
  const ctx = {
    ...base, setTimeout, clearTimeout, defineProps: () => props, withDefaults: (value, defaults) => ({ ...defaults, ...value }),
    useRoute: () => route, useRouter: () => ({ replace: target => changeRoute(target.query, target.path) }),
    onMounted() {}, onBeforeUnmount: callback => unmount.push(callback),
    watch: (_source, callback) => watchers.push(callback)
  };
  const changeRoute = (nextQuery, nextPath = route.path) => {
    route.query = nextQuery; route.path = nextPath;
    watchers.forEach(callback => callback());
  };
  vm.createContext(ctx); vm.runInContext(crudScript, ctx);
  return { api: ctx.api, route, props, lists, changeRoute, unmount: () => unmount.forEach(callback => callback()) };
}

const tests = [];
const test = (name, run) => tests.push({ name, run });

test('a date alone matches one day; moving or clearing it keeps the range valid', async () => {
  const field = { type: 'date', prop: 'scheduleDateStart', endProp: 'scheduleDateEnd' };
  const { api, lists } = setup({ queryFields: [field] });
  Object.assign(api.queryParams.value, { pageNum: 2, scheduleStatus: 'draft' });
  for (const [start, expectedEnd] of [
    ['2026-11-11', '2026-11-11'], ['2026-11-10', '2026-11-10'],
    ['2026-11-12', '2026-11-15'], ['2026-11-18', '2026-11-18'], [null, undefined]
  ]) {
    if (start === '2026-11-12') api.queryParams.value.scheduleDateEnd = '2026-11-15';
    await api.setDateStart(field, start);
    const query = lists.at(-1);
    assert.equal(query.scheduleDateStart, start || undefined);
    assert.equal(query.scheduleDateEnd, expectedEnd);
    assert.equal(query.scheduleStatus, 'draft');
    assert.equal(query.pageNum, 1);
  }
});

test('reservation starts with route context, and View all removes the initial schedule filter', () => {
  const noop = () => {};
  const ctx = { ...base, useRoute: () => ({ query: { scheduleId: 'A' } }),
    addReservation: noop, cancelReservation: noop, delReservation: noop, getReservation: noop,
    listReservation: noop, updateReservation: noop, resolveReservation: noop,
    loadCourseOptions: noop, loadMemberCardOptions: noop, loadMemberOptions: noop, loadScheduleOptions: noop };
  vm.createContext(ctx);
  vm.runInContext(script('src/views/fitness/reservation/index.vue', 'pageConfig'), ctx);
  const { initialQuery, initialForm } = ctx.api.pageConfig;
  const run = setup({ initialQuery, initialForm }, { scheduleId: 'A', tab: 'reserved' });
  assert.equal(run.api.queryParams.value.scheduleId, 'A');
  run.api.clearContext();
  assert.equal(run.api.queryParams.value.scheduleId, undefined);
  assert.equal(run.lists.at(-1).scheduleId, undefined);
  assert.equal(run.route.query.tab, 'reserved');
});

test('changing a course clears the previous session before querying and ignores its late options', async () => {
  const oldOptions = deferred();
  const course = { prop: 'courseId', type: 'remoteSelect' };
  const schedule = { prop: 'scheduleId', type: 'remoteSelect', dependsOn: 'courseId', remoteMethod: () => oldOptions.promise };
  const { api, lists } = setup({ queryFields: [course, schedule] }, { scheduleId: 'old' });
  const loading = api.loadRemoteOptions(schedule, 'query');
  Object.assign(api.queryParams.value, { courseId: 'course2', pageNum: 4 });
  await api.onFilterChange(course);
  assert.equal(lists.length, 1);
  assert.equal(lists[0].courseId, 'course2');
  assert.equal(lists[0].pageNum, 1);
  assert.equal(lists[0].scheduleId, undefined);
  oldOptions.resolve([{ value: 'old', label: '原场次' }]); await loading;
  assert.equal(api.remoteOptionsMap.query_scheduleId, undefined);
});

test('typing waits for a pause and an immediate selection cancels the pending duplicate search', async () => {
  const { api, lists } = setup();
  for (const keyword of ['a', 'ab']) { api.queryParams.value.keyword = keyword; api.onTextInput(keyword); }
  await new Promise(resolve => setTimeout(resolve, 360));
  assert.equal(lists.length, 1);
  assert.equal(lists[0].keyword, 'ab');
  api.queryParams.value.keyword = 'abc'; api.onTextInput('abc');
  await api.onFilterChange({ prop: 'status' });
  await new Promise(resolve => setTimeout(resolve, 360));
  assert.equal(lists.length, 2);
  assert.equal(lists[1].keyword, 'abc');
});

test('editing A then B keeps B when A responds last', async () => {
  const a = deferred(), b = deferred();
  const { api } = setup({ getFn: id => (id === 'A' ? a : b).promise });
  const first = api.handleUpdate({ reservationId: 'A' });
  const second = api.handleUpdate({ reservationId: 'B' });
  b.resolve({ data: { reservationId: 'B', memberId: 'member-B' } }); await second;
  a.resolve({ data: { reservationId: 'A', memberId: 'member-A' } }); await first;
  assert.equal(api.form.value.reservationId, 'B');
  assert.equal(api.form.value.memberId, 'member-B');
  assert.equal(api.dialog.visible, true);
});

test('cancelled edit cannot reopen after its detail response arrives', async () => {
  const pending = deferred(); const { api } = setup({ getFn: () => pending.promise });
  const editing = api.handleUpdate({ reservationId: 'A' }); api.cancel();
  pending.resolve({ data: { reservationId: 'A' } }); await editing;
  assert.equal(api.dialog.visible, false); assert.equal(api.form.value.reservationId, undefined);
});

test('opening Add invalidates a previous pending edit', async () => {
  const pending = deferred(); const { api } = setup({ getFn: () => pending.promise });
  const editing = api.handleUpdate({ reservationId: 'A' }); await api.handleAdd();
  pending.resolve({ data: { reservationId: 'A' } }); await editing;
  assert.equal(api.dialog.title, '新增预约'); assert.equal(api.form.value.reservationId, undefined);
});

test('late option hydration from A cannot replace B or reopen the old editor', async () => {
  const optionsA = deferred(), optionsB = deferred();
  const field = { type: 'remoteSelect', prop: 'memberId', remoteMethod: ({ value }) => (value === 'member-A' ? optionsA : optionsB).promise };
  const { api } = setup({ formFields: [field], getFn: async id => ({ data: { reservationId: id, memberId: `member-${id}` } }) });
  const first = api.handleUpdate({ reservationId: 'A' }); await Promise.resolve();
  const second = api.handleUpdate({ reservationId: 'B' }); await Promise.resolve();
  optionsB.resolve([{ value: 'member-B', label: '会员B' }]); await second;
  optionsA.resolve([{ value: 'member-A', label: '会员A' }]); await first;
  assert.equal(api.form.value.memberId, 'member-B');
  assert.deepEqual(copy(api.remoteOptionsMap.form_memberId), [{ value: 'member-B', label: '会员B' }]);
});

test('route member change closes the editor and invalidates pending form options', async () => {
  const options = deferred();
  const field = { type: 'remoteSelect', prop: 'memberId', remoteMethod: () => options.promise };
  const run = setup({ formFields: [field] }, { memberId: 'member-A' });
  const adding = run.api.handleAdd();
  assert.equal(run.api.dialog.visible, true);
  run.changeRoute({ memberId: 'member-B' });
  options.resolve(Object.assign([{ value: 'member-A', label: '会员A' }], { hasMore: true })); await adding;
  assert.equal(run.api.dialog.visible, false); assert.equal(run.api.form.value.memberId, 'member-B');
  assert.equal(run.api.remoteOptionsMap.form_memberId, undefined);
  assert.equal(run.api.remoteLoadingMap.form_memberId, false);
  assert.equal(run.api.remoteMoreMap.form_memberId, undefined);
});

test('route path change invalidates an edit detail response even if query is unchanged', async () => {
  const pending = deferred(); const run = setup({ getFn: () => pending.promise });
  const editing = run.api.handleUpdate({ reservationId: 'A' });
  run.changeRoute(run.route.query, '/fitness-member/member');
  pending.resolve({ data: { reservationId: 'A' } }); await editing;
  assert.equal(run.api.dialog.visible, false); assert.equal(run.api.form.value.reservationId, undefined);
});

test('dialog close control resets pending options and cannot restore a dismissed form', async () => {
  const pending = deferred(); const field = { type: 'remoteSelect', prop: 'memberId', remoteMethod: () => pending.promise };
  const { api } = setup({ formFields: [field] }, { memberId: 'A' });
  const adding = api.handleAdd(); let closed = false;
  api.closeDialog(() => { closed = true; });
  pending.resolve([{ value: 'A', label: '会员A' }]); await adding;
  assert.equal(closed, true); assert.equal(api.dialog.visible, false);
  assert.equal(api.remoteOptionsMap.form_memberId, undefined);
  const descriptor = sfc.parse(fs.readFileSync(path.join(root, crudFile), 'utf8')).descriptor;
  assert.match(descriptor.template.content, /:before-close="closeDialog"/);
});

test('old save completion cannot close a newly opened editor', async () => {
  const saved = deferred(); let saving;
  const { api } = setup({ updateFn: () => saved.promise });
  await api.handleUpdate({ reservationId: 'A' });
  api.formRef.value = { resetFields() {}, validate: callback => { saving = callback(true); } };
  api.submitForm(); api.cancel(); await api.handleUpdate({ reservationId: 'B' });
  saved.resolve(); await saving;
  assert.equal(api.form.value.reservationId, 'B'); assert.equal(api.dialog.visible, true);
});

test('unmounted page ignores both detail and form-option responses', async () => {
  const detail = deferred(), options = deferred();
  const field = { type: 'remoteSelect', prop: 'memberId', remoteMethod: () => options.promise };
  const run = setup({ getFn: () => detail.promise, formFields: [field] });
  const editing = run.api.handleUpdate({ reservationId: 'A' });
  const loadingOptions = run.api.loadRemoteOptions(field, 'form'); run.unmount();
  detail.resolve({ data: { reservationId: 'A' } }); options.resolve([{ value: 'A', label: '会员A' }]);
  await Promise.all([editing, loadingOptions]);
  assert.equal(run.api.dialog.visible, false); assert.equal(run.api.remoteOptionsMap.form_memberId, undefined);
});

(async () => {
  for (const { name, run } of tests) { await run(); console.log(`PASS ${name}`); }
  console.log(`RESULT ${tests.length}/${tests.length} passed. Offline only.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
