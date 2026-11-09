<template>
  <el-drawer :model-value="true" :title="title" size="min(540px, 94vw)" append-to-body @close="$emit('close')">
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-result v-else-if="failed" title="详情加载失败">
      <template #extra><el-button @click="load">重试</el-button></template>
    </el-result>
    <dl v-else class="record-details">
      <div v-for="field in fields" :key="field.prop">
        <dt>{{ field.label }}</dt>
        <dd>{{ displayValue(field) }}</dd>
      </div>
    </dl>
  </el-drawer>
</template>

<script setup lang="ts">
import { parseTime } from '@/utils/ruoyi';

interface DetailField {
  label: string;
  prop: string;
  type?: string;
  format?: string;
  options?: { label: string; value: unknown }[];
}
const props = defineProps<{
  title: string;
  row: Record<string, any>;
  fields: DetailField[];
  fetchRecord?: () => Promise<{ data: Record<string, any> }>;
}>();
defineEmits<{ close: [] }>();

const record = ref(props.row);
const loading = ref(!!props.fetchRecord);
const failed = ref(false);
async function load() {
  if (!props.fetchRecord) return;
  loading.value = true;
  failed.value = false;
  try {
    const { data } = await props.fetchRecord();
    if (!data) throw new Error('记录不存在');
    record.value = { ...props.row, ...data };
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
}
function displayValue(field: DetailField) {
  const value = record.value[field.prop];
  if (value == null || value === '') return '—';
  if (field.type === 'date') return parseTime(value, field.format || '{y}-{m}-{d} {h}:{i}:{s}');
  return field.options?.find(option => String(option.value) === String(value))?.label ?? value;
}
onMounted(load);
</script>

<style scoped>
.record-details { margin: 0; }
.record-details > div { display: grid; grid-template-columns: minmax(100px, 34%) 1fr; gap: 12px; padding: 11px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
dt { color: var(--el-text-color-secondary); }
dd { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
