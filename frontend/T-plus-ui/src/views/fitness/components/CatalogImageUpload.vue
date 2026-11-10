<template>
  <div class="catalog-upload">
    <el-image v-if="modelValue" class="preview" :src="src" :preview-src-list="[src]" fit="contain" preview-teleported />
    <input ref="input" type="file" accept="image/jpeg,image/png" hidden @change="upload" />
    <el-button :loading="busy" @click="input?.click()">{{ modelValue ? '更换图片' : '选择图片' }}</el-button>
    <el-button v-if="modelValue" link :disabled="busy" @click="emit('update:modelValue', '')">移除</el-button>
    <small>JPG / PNG · 5 MB 以内</small>
  </div>
</template>
<script setup lang="ts">
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
const props = defineProps<{ modelValue?: string; scope: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: string]; busy: [value: boolean] }>();
const input = ref<HTMLInputElement>();
const busy = ref(false);
let mounted = true;
const src = computed(() => props.modelValue?.startsWith('/fitness/catalog/images/') ? import.meta.env.VITE_APP_BASE_API + props.modelValue : props.modelValue);
onBeforeUnmount(() => { mounted = false; if (busy.value) emit('busy', false); });
async function upload(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  target.value = '';
  if (!file || busy.value) return;
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    ElMessage.error('请选择 5 MB 以内的 JPG 或 PNG 图片');
    return;
  }
  busy.value = true;
  emit('busy', true);
  try {
    const form = new FormData();
    form.append('file', file);
    const { data } = await request.post('/fitness/catalog/' + props.scope + '/images', form, { headers: { repeatSubmit: false } });
    if (mounted) emit('update:modelValue', data.url);
  } catch {
    // The request client shows the server's error; the previous image stays selected.
  } finally {
    busy.value = false;
    if (mounted) emit('busy', false);
  }
}
</script>
<style scoped>
.catalog-upload { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.preview { width: 90px; height: 64px; border: 1px solid var(--el-border-color); border-radius: 6px; }
small { color: var(--el-text-color-secondary); }
</style>
