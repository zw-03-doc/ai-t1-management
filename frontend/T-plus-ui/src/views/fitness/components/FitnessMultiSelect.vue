<template>
  <el-select
    :model-value="single ? modelValue?.[0] : modelValue || []"
    :multiple="!single" :multiple-limit="max || 0" :loading="loading" :placeholder="placeholder"
    :aria-label="label" :remote-method="search" :reserve-keyword="false"
    filterable remote clearable remote-show-suffix no-data-text="暂无匹配结果"
    popper-class="fitness-choice-menu"
    @visible-change="open => open && search('')" @update:model-value="update"
  >
    <template v-if="!single" #prefix><span class="selection-mode">多选</span></template>
    <template v-if="!single" #header>
      <span class="choice-count" aria-live="polite">已选 {{ modelValue?.length || 0 }} 项{{ max ? '，最多 ' + max + ' 项' : '' }}</span>
    </template>
    <el-option v-for="option in options" :key="String(option.value)" :value="option.value" :label="option.label">
      <span v-if="!single" class="choice-box" aria-hidden="true">{{ modelValue?.some(value => value === option.value) ? '✓' : '' }}</span>
      {{ option.label }}
    </el-option>
    <template v-if="hasMore" #footer><el-button text :loading="loading" @click="emit('more')">加载更多</el-button></template>
  </el-select>
</template>

<script setup lang="ts" generic="T extends string | number | boolean">
import type { RemoteSelectOption } from './remoteOptions';

const props = defineProps<{
  modelValue?: T[]; label: string; placeholder?: string; options: RemoteSelectOption[];
  search: (keyword: string) => void; loading?: boolean; hasMore?: boolean; max?: number;
}>();
const emit = defineEmits<{ 'update:modelValue': [T[]]; more: [] }>();
const single = computed(() => props.max === 1 && (props.modelValue?.length || 0) <= 1);
function update(value: T | T[] | null | undefined) {
  emit('update:modelValue', Array.isArray(value) ? value : value == null || value === '' ? [] : [value]);
}
</script>

<style scoped>
.selection-mode { font-size: 12px; color: var(--el-text-color-regular); padding-right: 7px; border-right: 1px solid var(--el-border-color); white-space: nowrap; }
.choice-count { font-size: 13px; color: var(--el-text-color-regular); }
.choice-box { display: inline-grid; place-items: center; box-sizing: border-box; width: 16px; height: 16px; margin-right: 8px; vertical-align: middle; border: 1px solid var(--el-border-color-darker); border-radius: 4px; line-height: 1; }
.is-selected .choice-box { background: var(--el-color-primary); border-color: var(--el-color-primary); color: white; }
:global(.fitness-choice-menu .el-select-dropdown.is-multiple .el-select-dropdown__item.is-selected::after) { display: none; }
</style>
