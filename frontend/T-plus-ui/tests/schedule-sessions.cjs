const {test}=require('node:test'), assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript');
const vue=require('vue'),{parse}=require('@vue/compiler-sfc');
const root=path.join(__dirname,'../src/views/fitness/schedule');
const output=s=>ts.transpileModule(s,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
const api={};vm.runInNewContext(output(fs.readFileSync(path.join(root,'sessions.ts'),'utf8')),{exports:api});
function editor(model={}){
 const s=parse(fs.readFileSync(path.join(root,'ScheduleTimes.vue'),'utf8')).descriptor.scriptSetup.content.replace(/^import .*;$/gm,'');
 const ctx={...vue,defineProps:()=>({model}),loadMemberOptions:async()=>[]};
 return vm.runInNewContext(output(s)+'\n;({dates,periods,adjust,changes,removed,rows,addPeriod,restoreRemoved,singleDate})',ctx);
}
test('four dates multiplied by two windows retain individual exceptions and audiences',()=>{
 const model={scheduleMode:'directed',studentMemberIds:['A']},e=editor(model);
 e.dates.value=['2026-11-19','2026-11-20','2026-11-21','2026-11-22'];
 Object.assign(e.periods.value[0],{start:'14:00',end:'16:00'}); e.addPeriod(); Object.assign(e.periods.value[1],{start:'17:00',end:'20:00'});
 assert.equal(model.sessions.length,8);assert.equal(api.draftError(model.sessions),'');
 const row=e.rows.value[3];e.adjust(row);e.changes[row.key].start='18:00';e.changes[row.key].studentMemberIds=['B'];
 e.removed.add(e.rows.value[4].key);
 const sent=api.scheduleRequests(model);assert.equal(sent.length,7);assert.equal(sent[3].startTime,'2026-11-20 18:00:00');assert.equal(sent[3].studentMemberIds[0],'B');assert.equal(sent[0].studentMemberIds[0],'A');
 assert.equal(sent.filter(r=>r.startTime.endsWith('17:00:00')).length,3);
 e.restoreRemoved();assert.equal(model.sessions.length,8);e.changes[row.key].start='15:00';assert.match(api.draftError(model.sessions),/重叠/);
});
test('editing an overnight session preserves its dates and seconds',()=>{
 const model={scheduleId:'s',scheduleDate:'2026-11-19',startTime:'2026-11-19 23:10:18',endTime:'2026-11-20 00:20:18',scheduleMode:'normal'};
 const e=editor(model);assert.equal(api.draftError(model.sessions),'');e.singleDate.value='2026-11-21';
 const [r]=api.scheduleRequests(model);assert.equal(r.scheduleId,'s');assert.equal(r.startTime,'2026-11-21 23:10:18');assert.equal(r.endTime,'2026-11-22 00:20:18');
});
test('empty, reversed and overlapping times cannot be submitted; adjacent times can',()=>{
 assert(api.draftError([]));const a={date:'2026-11-19',start:'14:00',end:'16:00'};
 assert(api.draftError([{...a,end:''}]));assert(api.draftError([{...a,start:'17:00'}]));assert(api.draftError([a,{...a}]));assert.equal(api.draftError([a,{...a,start:'16:00',end:'17:00'}]),'');
});
