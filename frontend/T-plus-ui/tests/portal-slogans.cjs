// Run the real editor methods with a versioned in-memory server. No external requests.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const sfc = require('@vue/compiler-sfc');
const filename = path.resolve(__dirname, '../src/views/fitness/portal/index.vue');
const { descriptor, errors } = sfc.parse(fs.readFileSync(filename, 'utf8'), { filename });
assert.equal(errors.length, 0);
const template = sfc.compileTemplate({ source: descriptor.template.content, filename, id: 'portal-test' });
assert.deepEqual(template.errors, []);
const source = descriptor.scriptSetup.content.replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '');
const script = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText;
const original = {
  eyebrow: 'YOUR NEXT REP', titleLead: '为下一次', titleAccent: '进步', titleSecondLine: '而练。',
  subtitle: '约一节好课，认真完成每一组。', galleryCaption: '在 T1，找到你的训练节奏', footerCaption: 'T1 · 每一组，都算数', version: 0
};

async function run() {
  let record = { ...original };
  let permitted = true;
  let confirmReload = false;
  let writes = 0;
  const scope = {
    ref: value => ({ value }), reactive: value => value, computed: get => ({ get value() { return get(); } }),
    onMounted() {}, checkPermi: () => permitted, gymOne: '', gymTwo: '', gymThree: '',
    ElMessage: { success() {} }, ElMessageBox: { async confirm() { if (!confirmReload) throw Error('cancel'); } },
    getPortalSlogans: async () => ({ data: { ...record } }),
    updatePortalSlogans: async edits => {
      writes++;
      if (edits.version !== record.version) throw Error('conflict');
      record = { ...edits, version: record.version + 1 };
      return { data: { ...record } };
    }
  };
  vm.runInNewContext(script + '\nglobalThis.page = { draft, errors, dirty, reload, save };', scope);
  const { draft, errors, dirty, reload, save } = scope.page;
  await reload();
  assert.equal(draft.value.titleLead, '为下一次');

  draft.value.eyebrow = '  ';
  await save();
  assert.ok(errors.eyebrow);
  assert.equal(writes, 0);
  draft.value.eyebrow = original.eyebrow;
  draft.value.titleAccent = ' ';
  await save();
  assert.equal(record.titleAccent, '');
  assert.equal(draft.value.version, 1);
  assert.equal(dirty.value, false);

  draft.value.subtitle = '本地修改';
  record = { ...record, subtitle: '另一管理员修改', version: 2 };
  await save();
  assert.equal(draft.value.subtitle, '本地修改');
  assert.equal(draft.value.version, 1);
  assert.equal(record.subtitle, '另一管理员修改');
  await reload();
  assert.equal(draft.value.subtitle, '本地修改');
  confirmReload = true;
  await reload();
  assert.equal(draft.value.subtitle, '另一管理员修改');
  assert.equal(draft.value.version, 2);

  permitted = false;
  draft.value.footerCaption = '只读账号模拟编辑';
  const before = writes;
  await save();
  assert.equal(writes, before);
  console.log('PASS portal template, required fields, optional accent, returned version, conflict draft, reload confirmation and read-only access');
}
run().catch(error => { console.error(error); process.exitCode = 1; });
