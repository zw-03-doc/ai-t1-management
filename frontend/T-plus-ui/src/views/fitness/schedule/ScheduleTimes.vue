<template>
  <section class="session-editor">
    <el-form-item label="排课日期" required>
      <el-date-picker v-if="editing" v-model="singleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      <el-date-picker v-else v-model="dates" type="dates" value-format="YYYY-MM-DD" placeholder="选择一个或多个日期" />
    </el-form-item>
    <el-form-item label="时间段" required>
      <div class="time-templates">
        <div v-for="(period, index) in periods" :key="period.id" class="time-pair">
          <input v-model="period.start" aria-label="开始时间" type="time" />
          <span>至</span>
          <input v-model="period.end" aria-label="结束时间" type="time" />
          <el-checkbox v-model="period.nextDay">次日结束</el-checkbox>
          <el-button v-if="periods.length > 1" link type="danger" @click="periods.splice(index, 1)">移除时段</el-button>
        </div>
        <el-button v-if="!editing" link type="primary" @click="addPeriod">＋ 添加时间段</el-button>
      </div>
    </el-form-item>
    <el-form-item prop="sessions" :label="editing ? '' : '场次预览'" :label-width="editing ? 0 : undefined">
      <div v-if="!editing" class="preview">
        <div class="preview-heading">
          <strong>{{ rows.length }} 场</strong>
          <el-button v-if="removedCount" link @click="restoreRemoved">恢复已移除的 {{ removedCount }} 场</el-button>
        </div>
        <div v-for="row in rows" :key="row.key" class="preview-row">
          <time>{{ row.date }}</time>
          <template v-if="editingKey !== row.key">
            <span class="slot-time">{{ row.start?.slice(0,5) || '待选' }} – {{ row.nextDay ? '次日 ' : '' }}{{ row.end?.slice(0,5) || '待选' }}</span>
            <el-button link type="primary" @click="adjust(row)">调整</el-button>
          </template>
          <template v-else>
            <div class="override">
              <div class="time-pair">
                <input v-model="changes[row.key].start" aria-label="本场开始时间" type="time" />
                <span>至</span>
                <input v-model="changes[row.key].end" aria-label="本场结束时间" type="time" />
                <el-checkbox v-model="changes[row.key].nextDay">次日结束</el-checkbox>
              </div>
              <el-select v-if="directed" v-model="changes[row.key].studentMemberIds" multiple filterable remote
                :remote-method="searchMembers" aria-label="本场学员" placeholder="本场学员" @visible-change="visible => visible && searchMembers('')">
                <el-option v-for="member in memberOptions" :key="String(member.value)" :label="member.label" :value="member.value" />
              </el-select>
            </div>
            <el-button link type="primary" @click="editingKey = ''">完成</el-button>
          </template>
          <el-button link type="danger" @click="removed.add(row.key)">移除</el-button>
        </div>
      </div>
    </el-form-item>
  </section>
</template>

<script setup lang="ts">
import type { SessionTime } from './sessions';
import { loadMemberOptions } from '../components/remoteOptions';
const props = defineProps<{ model: Record<string, any> }>();
const editing = !!props.model.scheduleId;
const singleDate = ref(String(props.model.scheduleDate || '').slice(0, 10));
const dates = ref<string[]>([]);
let nextId = 1;
const periods = ref([{ id: nextId, start: String(props.model.startTime || '').slice(11,19), end: String(props.model.endTime || '').slice(11,19),
  nextDay: !!editing && String(props.model.endTime).slice(0,10) !== singleDate.value }]);
const changes = reactive<Record<string, Partial<SessionTime>>>({});
const removed = reactive(new Set<string>());
const editingKey = ref('');
const directed = computed(() => props.model.scheduleMode !== 'normal');
const memberOptions = ref<{label: string; value: string | number | boolean}[]>([]);
let memberSearch = 0;
async function searchMembers(keyword: string) {
  const request = ++memberSearch;
  const selected = changes[editingKey.value]?.studentMemberIds;
  const options = await loadMemberOptions({ keyword, value: selected });
  if (request === memberSearch) memberOptions.value = options;
}

const bases = computed(() => {
  const days = editing ? [singleDate.value] : [...new Set(dates.value || [])].sort();
  return days.flatMap(date => periods.value.map(({ id, ...slot }) => ({ key: date + '/' + id, date, ...slot })));
});
const rows = computed(() => bases.value.filter(row => !removed.has(row.key)).map(row => ({ ...row, ...changes[row.key] }))
  .sort((a, b) => a.date.localeCompare(b.date) || a.start.localeCompare(b.start)));
const removedCount = computed(() => bases.value.length - rows.value.length);
function restoreRemoved() { removed.clear(); }
function addPeriod() { periods.value.push({ id: ++nextId, start: '', end: '', nextDay: false }); }
function adjust(row: SessionTime & { key: string }) {
  editingKey.value = row.key;
  if (!changes[row.key]) {
    changes[row.key] = { start: row.start, end: row.end, nextDay: row.nextDay };
  }
  if (directed.value) {
    changes[row.key].studentMemberIds ??= [...(props.model.studentMemberIds || [])];
    searchMembers('');
  }
}
watch(bases, list => {
  const keys = new Set(list.map(row => row.key));
  for (const key of removed) if (!keys.has(key)) removed.delete(key);
  for (const key of Object.keys(changes)) if (!keys.has(key)) delete changes[key];
  if (!keys.has(editingKey.value)) editingKey.value = '';
});
watchEffect(() => {
  props.model.sessions = rows.value.map(({ key, ...row }) => row);
}, { flush: 'sync' });
</script>

<style scoped>
.session-editor { margin: 0 0 16px; padding: 16px 0 0; border-block: 1px solid var(--el-border-color-lighter); }
.time-templates, .preview { width: 100%; }
.session-editor :deep(.el-date-editor) { width: 100%; }
.time-pair, .preview-heading, .preview-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px 12px; }
.time-pair { margin-bottom: 6px; }
.time-pair input { box-sizing: border-box; height: 34px; padding: 0 8px; border: 1px solid var(--el-border-color); border-radius: 6px; background: var(--el-fill-color-blank); color: var(--el-text-color-primary); font: inherit; }
.preview { max-height: 280px; overflow: auto; }
.preview-heading { justify-content: space-between; }
.preview-row { padding: 5px 0; border-top: 1px solid var(--el-border-color-lighter); }
.preview-row time { font-variant-numeric: tabular-nums; }
.slot-time, .override { flex: 1; }
.preview-row .el-button { margin-left: 0; }
</style>
