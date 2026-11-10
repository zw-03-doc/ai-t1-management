// Executes the real page methods offline. No login, network or production mutations.
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const assert = require('assert/strict');
const base = path.resolve(__dirname, '..');
const { parse } = require(path.join(base, 'node_modules/@vue/compiler-sfc'));
let checks = 0;
function page(relative, answers = [], cards = []) {
  const source = fs.readFileSync(path.join(base, relative), 'utf8');
  const parsed = parse(source);
  assert.equal(parsed.errors.length, 0);
  const script = parsed.descriptor.script.content.replace(/import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?/g, '').replace('export default', 'globalThis.component=');
  const calls = [];
  const ctx = { console, T1Nav: {}, CheckinReasonSheet: {}, Number, String, Math, Date, Set, Promise,
    memberEligibleCards: async id => { calls.push(['eligible', id]); return cards; },
    memberCreateReservation: async payload => calls.push(['reserve', payload]),
    coachConfirmCheckin: async payload => calls.push(['confirm', payload]),
    coachRestoreCheckin: async (id, reason) => calls.push(['restore', id, reason]),
    coachExceptionCheckin: async payload => calls.push(['exception', payload]),
    uni: { showLoading() {}, hideLoading() {}, showToast: value => calls.push(['toast', value.title]),
      showActionSheet: options => { calls.push(['options', options.itemList]); const next = answers.shift(); next == null ? options.fail() : options.success({ tapIndex: next }); },
      showModal: options => { calls.push(['modal', options.title, options.content]); options.success(answers.shift() || {confirm:false}); }
    }
  };
  vm.createContext(ctx);
  vm.runInContext(fs.readFileSync(path.join(base, 'utils/booking-date.js'), 'utf8').replace(/export /g, ''), ctx);
  vm.runInContext(script, ctx);
  const state = ctx.component.data();
  for (const [name, fn] of Object.entries(ctx.component.methods)) state[name] = fn.bind(state);
  state.refreshAll = async () => {}; state.refreshReservations = async () => {}; state.loadCurrentTabData = async () => {};
  return { state, calls };
}
const clone = value => JSON.parse(JSON.stringify(value));
(async () => {
  {
    const detail = page('pages/course/detail.vue').state;
    const list = page('pages/course/index.vue').state;
    assert.equal(detail.dateText('2026-12-09 14:36:51'), '2026 / 12 / 09');
    assert.equal(detail.dateText('2026-12-09T16:05:00Z'), '2026 / 12 / 10');
    assert.equal(list.formatTime('2026-12-09 00:15:00'), '00:15');
    assert.equal(list.formatDateOnly('2026-12-09'), '2026-12-09');
    assert.equal(list.formatDateOnly('invalid'), '');
    checks++;
  }
  for (const courseType of ['group','private','small','experience']) {
    const {state,calls}=page('pages/course/index.vue',[1],[{cardId:'card',planType:'stored_card',balanceAmount:80}]);
    await state.reserveSchedule({scheduleId:'session',courseType});
    assert.deepEqual(clone(calls.find(c=>c[0]==='reserve')[1]),{scheduleId:'session',cardId:'card',experience:false}); checks++;
  }
  {
    const cards=Array.from({length:7},(_,i)=>({cardId:String(i+1),planType:'count_card',remainTimes:3}));
    const {state,calls}=page('pages/course/index.vue',[5,3],cards);
    const chosen=await state.pickReservationCard({scheduleId:'s'});
    assert.equal(chosen,'7'); assert(calls.filter(c=>c[0]==='options').every(c=>c[1].length<=6)); checks++;
  }
  {
    const {state}=page('pages/course/index.vue',[0],[]);
    assert.deepEqual(clone(await state.buildReservationPayload({scheduleId:'s'})),{scheduleId:'s',experience:true}); checks++;
  }
  {
    const {state,calls}=page('pages/course/index.vue',[null],[]);
    await state.reserveSchedule({scheduleId:'s',courseType:'group'});
    assert(!calls.some(c=>c[0]==='reserve')); assert.equal(state.actioningKey,''); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue',[{confirm:true,content:'12.50'},{confirm:true}]);
    await state.confirmItem({reservationId:'r',cardId:'c',planType:'stored_card',balanceAmount:20,remainTimes:0});
    const sent=calls.find(c=>c[0]==='confirm')[1]; assert.equal(sent.consumeTimes,0); assert.equal(sent.consumeAmount,12.5);
    assert(calls.some(c=>c[0]==='modal' && String(c[2]).includes('7.50'))); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue',[{confirm:true,content:'2'},{confirm:true}]);
    await state.confirmItem({reservationId:'r',cardId:'c',planType:'count_card',remainTimes:4});
    const sent=calls.find(c=>c[0]==='confirm')[1]; assert.equal(sent.consumeTimes,2); assert.equal(sent.consumeAmount,0); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue',[{confirm:true}]);
    await state.confirmItem({reservationId:'r',cardId:'c',planType:'time_card',remainTimes:0});
    const sent=calls.find(c=>c[0]==='confirm')[1]; assert.equal(sent.consumeTimes,0); assert.equal(sent.consumeAmount,0); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue',[{confirm:true,content:'21'}]);
    await state.confirmItem({reservationId:'r',cardId:'c',planType:'stored_card',balanceAmount:20});
    assert(!calls.some(c=>c[0]==='confirm')); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue');
    state.openReason('restore',{reservationId:'r',checkinId:'existing'});
    await state.submitReason('  已核实到场  ');
    assert.deepEqual(calls.find(c=>c[0]==='restore'),['restore','existing','已核实到场']); checks++;
  }
  {
    const {state,calls}=page('pages/coach/checkin/index.vue');
    state.openReason('restore',{reservationId:'r',checkinId:'existing'});
    await state.submitReason(' '); assert(!calls.some(c=>c[0]==='restore')); assert(state.reasonDialog);
    state.closeReason(); assert.equal(state.reasonDialog,null); checks++;
  }
  console.log(`booking-ui: ${checks} checks passed`);
})().catch(error => { console.error(error); process.exitCode = 1; });
