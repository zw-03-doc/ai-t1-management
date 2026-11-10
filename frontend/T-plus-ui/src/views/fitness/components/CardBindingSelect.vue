<template>
  <el-select :model-value="modelValue" :placeholder="placeholder" filterable remote clearable :remote-method="search" :loading="loading"
    @visible-change="open => open && search('')" @update:model-value="value => emit('update:modelValue', value || undefined)">
    <el-option v-for="option in options" :key="String(option.value)" :label="option.label" :value="option.value" />
    <template #footer><el-button v-if="more" text :loading="loading" @click="loadPage(page + 1)">加载更多</el-button></template>
  </el-select>
</template>
<script setup lang="ts">
import type { RemoteSelectOption, RemoteSelectParams } from './remoteOptions';
const props = defineProps<{
  modelValue?: string | number; placeholder: string; model?: Record<string, any>;
  load: (params: RemoteSelectParams) => Promise<RemoteSelectOption[] & { hasMore?: boolean }>;
}>();
const emit = defineEmits<{ 'update:modelValue': [id: string | number | undefined] }>();
const options = ref<RemoteSelectOption[]>([]);
const loading = ref(false);
const more = ref(false);
const page = ref(1);
let keyword = '', sequence = 0;
async function loadPage(num: number) {
  const seq = ++sequence;
  loading.value = true;
  try {
    const rows = await props.load({ keyword, value: props.modelValue, model: props.model, pageNum: num });
    if (seq !== sequence) return;
    const all = num === 1 ? rows : [...options.value, ...rows];
    options.value = [...new Map(all.map(item => [String(item.value), item])).values()];
    more.value = !!rows.hasMore;
    page.value = num;
  } finally { if (seq === sequence) loading.value = false; }
}
function search(value: string) { keyword = value; return loadPage(1); }
watch(() => props.modelValue, id => { if (id && !options.value.some(o => o.value === id)) search(''); }, { immediate: true });
onBeforeUnmount(() => { sequence++; });
</script>
