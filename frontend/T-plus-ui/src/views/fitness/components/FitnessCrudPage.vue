<template>
  <div class="p-2">
    <el-button v-if="hasContext" class="mb-[10px]" link type="primary" @click="clearContext">清除筛选并查看全部</el-button>
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="never">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true" @submit.prevent>
            <el-form-item v-for="field in queryFields" :key="field.prop" :label="field.label" :prop="field.prop" :class="{ 'date-range-filter': field.endProp }">
              <el-input
                v-if="field.type === 'input' || !field.type"
                v-model="queryParams[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                clearable
                @input="onTextInput"
                @keyup.enter="!$event.isComposing && handleQuery()"
              />
              <el-select
                v-else-if="field.type === 'select'"
                v-model="queryParams[field.prop]"
                @change="onFilterChange(field)"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              >
                <el-option v-for="option in field.options || []" :key="String(option.value)" :label="option.label" :value="option.value" />
              </el-select>
              <el-select
                v-else-if="['remoteSelect', 'remoteSelectMultiple'].includes(field.type || '')"
                v-model="queryParams[field.prop]"
                @change="onFilterChange(field)"
                :multiple="['remoteSelectMultiple'].includes(field.type || '')"
                clearable
                filterable
                remote
                reserve-keyword
                :loading="getRemoteLoading('query', field.prop)"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :remote-method="(keyword) => handleRemoteSearch(field, 'query', keyword)"
                no-data-text="暂无匹配结果"
                @visible-change="(visible) => handleRemoteVisibleChange(field, 'query', visible)"
              >
                <el-option
                  v-for="option in getFieldOptions(field, 'query')"
                  :key="String(option.value)"
                  :label="option.label"
                  :value="option.value"
                />
                <template #footer>
                  <el-button v-if="remoteMoreMap[getRemoteKey('query', field.prop)]" text :loading="getRemoteLoading('query', field.prop)" @click="loadMore(field, 'query')">加载更多</el-button>
                </template>
              </el-select>

              <div v-else-if="field.type === 'date' && field.endProp" class="query-date-range">
                <el-date-picker
                  :model-value="queryParams[field.prop]"
                  type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :aria-label="field.label"
                  @update:model-value="(day) => setDateStart(field, day)"
                />
                <span>至</span>
                <el-date-picker
                  :model-value="queryParams[field.endProp] === queryParams[field.prop] ? null : queryParams[field.endProp]"
                  type="date" value-format="YYYY-MM-DD" placeholder="结束日期（选填）" aria-label="结束日期"
                  :disabled="!queryParams[field.prop]"
                  :disabled-date="(day: Date) => day < new Date(queryParams[field.prop] + 'T00:00:00')"
                  @update:model-value="(day) => { queryParams[field.endProp!] = day || queryParams[field.prop]; onFilterChange(field); }"
                />
              </div>
              <el-date-picker
                v-else-if="field.type === 'date'"
                v-model="queryParams[field.prop]"
                @change="onFilterChange(field)"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              />
              <el-date-picker
                v-else-if="field.type === 'datetime'"
                v-model="queryParams[field.prop]"
                @change="onFilterChange(field)"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              />
            </el-form-item>
            <el-form-item>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="never">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col v-if="allowCreate" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:add`]" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <slot name="header-actions" :selection="selectedRows" :loading="loading" :refresh="handleQuery" />
          <el-col v-if="allowDelete" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:remove`]" type="danger" plain icon="Delete" :disabled="!selectedRows.length" @click="handleDelete()"
              >批量删除</el-button
            >
          </el-col>
          <el-col v-if="exportUrl" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:export`]" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="list" @selection-change="handleSelectionChange">
        <el-table-column v-if="(allowDelete && checkPermi([`${permPrefix}:remove`])) || (selectionPermission && checkPermi([selectionPermission]))" type="selection" width="44" align="center" />
        <el-table-column
          v-for="column in tableColumns"
          :key="column.prop"
          :label="column.label"
          align="left"
          :prop="column.prop"
          :width="column.width"
          :min-width="column.minWidth"
          :show-overflow-tooltip="column.showOverflow !== false"
        >
          <template #default="scope">
            <span v-if="column.type === 'date'">{{ proxy?.parseTime(scope.row[column.prop], column.format || '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <el-tag
              v-else-if="column.type === 'tag'"
              :type="resolveTagType(column.options, scope.row[column.prop], column.tagType)"
              :class="resolveTagClass(column.options, scope.row[column.prop])"
              >{{ resolveOptionLabel(column.options, scope.row[column.prop]) }}</el-tag
            >
            <span v-else>{{ cellText(scope.row, column) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" align="center" :width="$slots['row-actions'] ? 164 : 120">
          <template #default="scope">
            <div class="record-actions">
              <slot name="detail-action" :row="scope.row">
                <el-button v-hasPermi="[`${permPrefix}:query`]" link type="primary" @click="detailRow = scope.row">详情</el-button>
              </slot>
              <el-tooltip v-if="allowEdit" content="修改" placement="top">
                <el-button v-hasPermi="[`${permPrefix}:edit`]" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
              </el-tooltip>
              <el-tooltip v-if="allowDelete" content="删除" placement="top">
                <el-button v-hasPermi="[`${permPrefix}:remove`]" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
              </el-tooltip>
              <slot name="row-actions" :row="scope.row" :refresh="getList"></slot>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <FitnessRecordDetails v-if="detailRow" :key="detailRow[idKey]" :title="title + '详情'" :row="detailRow"
      :fields="[...tableColumns, ...(detailColumns || [])]" :fetch-record="fetchDetail" @close="detailRow = undefined" />
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="min(900px, 96vw)" append-to-body destroy-on-close :before-close="closeDialog">
      <el-form v-if="dialog.visible" ref="formRef" :model="form" :rules="rules" :disabled="saving" label-width="110px">
        <el-row>
          <el-col v-for="field in formFields.filter(item => !item.visibleWhen || item.visibleWhen(form))" :key="field.prop" :span="field.span || 12" :xs="24">
            <slot v-if="field.type === 'custom'" :name="'field-' + field.prop" :model="form" />
            <el-form-item v-else :label="field.label" :prop="field.prop">
              <el-input
                v-if="field.type === 'input' || !field.type"
                v-model="form[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                v-model="form[field.prop]"
                type="textarea"
                :rows="field.rows || 3"
                :placeholder="field.placeholder || `请输入${field.label}`"
              />
              <el-input-number
                v-else-if="field.type === 'number'"
                v-model="form[field.prop]"
                :min="field.min ?? 0"
                :max="field.max"
                :precision="field.precision"
                style="width: 100%"
              />
              <el-select
                v-else-if="field.type === 'select'"
                v-model="form[field.prop]"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              >
                <el-option v-for="option in field.options || []" :key="String(option.value)" :label="option.label" :value="option.value" />
              </el-select>
              <el-select
                v-else-if="['remoteSelect', 'remoteSelectMultiple'].includes(field.type || '')"
                v-model="form[field.prop]"
                :multiple="field.type === 'remoteSelectMultiple'"
                :disabled="field.lockOnEdit && !!form[idKey]"
                @change="value => field.onSelect?.(form, getFieldOptions(field, 'form').find(option => option.value === value))"
                clearable
                filterable
                remote
                reserve-keyword
                :loading="getRemoteLoading('form', field.prop)"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :remote-method="(keyword) => handleRemoteSearch(field, 'form', keyword)"
                no-data-text="暂无匹配结果"
                @visible-change="(visible) => handleRemoteVisibleChange(field, 'form', visible)"
              >
                <el-option v-for="option in getFieldOptions(field, 'form')" :key="String(option.value)" :label="option.label" :value="option.value" />
                <template #footer>
                  <el-button v-if="remoteMoreMap[getRemoteKey('form', field.prop)]" text :loading="getRemoteLoading('form', field.prop)" @click="loadMore(field, 'form')">加载更多</el-button>
                </template>
              </el-select>
              <el-date-picker
                v-else-if="field.type === 'date'"
                v-model="form[field.prop]"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
                style="width: 100%"
                :placeholder="field.placeholder || `请选择${field.label}`"
              />

              <el-date-picker
                v-else-if="field.type === 'datetime'"
                v-model="form[field.prop]"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                clearable
                style="width: 100%"
                :placeholder="field.placeholder || `请选择${field.label}`"
              />
              <CatalogImageUpload v-else-if="field.type === 'imageUpload'" v-model="form[field.prop]" :scope="permPrefix.split(':')[1]" @busy="value => value ? uploads.add(field.prop) : uploads.delete(field.prop)" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-alert v-if="saveError" :title="saveError" type="error" :closable="false" />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="saving" :disabled="uploads.size > 0" @click="submitForm"><slot name="submit-label" :model="form">确定</slot></el-button>
          <el-button :disabled="saving" @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FitnessCrudPage" lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { checkPermi } from '@/utils/permission';
import CatalogImageUpload from './CatalogImageUpload.vue';
import FitnessRecordDetails from './FitnessRecordDetails.vue';
interface OptionItem {
  salePrice?: number;
  label: string;
  value: string | number | boolean;
  elTagType?: string;
  elTagClass?: string;
}

interface RemoteOptionRequest {
  keyword?: string;
  pageNum?: number;
  value?: string | number | boolean | Array<string | number | boolean> | null | undefined;
  model?: Record<string, any>;
}

type RemoteMethod = (params: RemoteOptionRequest) => Promise<OptionItem[] & { hasMore?: boolean }>;
type QueryFieldType = 'input' | 'select' | 'remoteSelect' | 'remoteSelectMultiple' | 'date' | 'datetime';
type FormFieldType = 'input' | 'textarea' | 'number' | 'select' | 'remoteSelect' | 'remoteSelectMultiple' | 'date' | 'datetime' | 'imageUpload' | 'custom';

type FieldScope = 'query' | 'form';

interface QueryField {
  label: string;
  prop: string;
  type?: QueryFieldType;
  placeholder?: string;
  options?: OptionItem[];
  remoteMethod?: RemoteMethod;
  dependsOn?: string;
  endProp?: string;
}

interface FormField {
  label: string;
  prop: string;
  type?: FormFieldType;
  placeholder?: string;
  options?: OptionItem[];
  remoteMethod?: RemoteMethod;
  span?: number;
  rows?: number;
  min?: number;
  max?: number;
  precision?: number;
  limit?: number;
  visibleWhen?: (model: Record<string, any>) => boolean;
  lockOnEdit?: boolean;
  onSelect?: (model: Record<string, any>, option?: OptionItem) => void;
}

interface TableColumn {
  label: string;
  prop: string;
  width?: string | number;
  minWidth?: number;
  fallbackProp?: string;
  showOverflow?: boolean;
  type?: 'text' | 'date' | 'tag';
  format?: string;
  options?: OptionItem[];
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}

type FieldConfig = QueryField | FormField;

const props = withDefaults(
  defineProps<{
    title: string;
    permPrefix: string;
    idKey: string;
    exportUrl?: string;
    allowCreate?: boolean;
    allowEdit?: boolean;
    allowDelete?: boolean;
    selectionPermission?: string;
    queryFields: QueryField[];
    formFields: FormField[];
    tableColumns: TableColumn[];
    detailColumns?: TableColumn[];
    initialQuery: Record<string, any>;
    initialForm: Record<string, any>;
    rules?: Record<string, any>;
    listFn: (query: any) => Promise<any>;
    getFn: (id: string | number) => Promise<any>;
    addFn: (data: any) => Promise<any>;
    updateFn: (data: any) => Promise<any>;
    delFn: (ids: string | number | Array<string | number>) => Promise<any>;
  }>(),
  {
    allowCreate: true,
    allowEdit: true,
    allowDelete: true,
    exportUrl: ''
  }
);

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();
const contextKeys = ['storeId', 'homeStoreId', 'memberId', 'coachId', 'scheduleId'];
const hasContext = computed(() => contextKeys.some((key) => typeof route.query[key] === 'string'));
function applyContext(model: Record<string, any>) {
  for (const key of contextKeys) {
    if (key !== props.idKey && (key in props.initialQuery || key in props.initialForm)
      && typeof route.query[key] === 'string') model[key] = route.query[key];
  }
  return model;
}
function clearContext() {
  const query = { ...route.query };
  contextKeys.forEach((key) => delete query[key]);
  router.replace({ path: route.path, query });
}
const loading = ref(false);
const detailRow = ref<Record<string, any>>();
const fetchDetail = () => props.getFn(detailRow.value![props.idKey]);
const showSearch = ref(true);
const list = ref<any[]>([]);
const total = ref(0);
const selectedRows = ref<any[]>([]);
const ids = computed<Array<string | number>>(() => selectedRows.value.map((row) => row[props.idKey]));
const queryFormRef = ref<ElFormInstance>();
const formRef = ref<ElFormInstance>();
const remoteOptionsMap = reactive<Record<string, OptionItem[]>>({});
const remoteLoadingMap = reactive<Record<string, boolean>>({});
const remoteMoreMap = reactive<Record<string, boolean>>({});
const remotePageMap: Record<string, number> = {};
const remoteKeywordMap: Record<string, string> = {};
const remoteRequestMap: Record<string, number> = {};
let editRequest = 0;

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const queryParams = ref<Record<string, any>>(applyContext(cloneValue(props.initialQuery)));
const form = ref<Record<string, any>>(cloneValue(props.initialForm));
const rules = computed(() => props.rules || {});

function cloneValue<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

function isRemoteField(field: FieldConfig): field is FieldConfig & { type: 'remoteSelect' | 'remoteSelectMultiple'; remoteMethod: RemoteMethod } {
  return (field.type === 'remoteSelect' || field.type === 'remoteSelectMultiple') && typeof field.remoteMethod === 'function';
}

function getRemoteKey(scope: FieldScope, prop: string) {
  return `${scope}_${prop}`;
}

function invalidateRemoteField(key: string) {
  remoteRequestMap[key] = (remoteRequestMap[key] || 0) + 1;
  delete remoteOptionsMap[key];
  delete remoteMoreMap[key];
  delete remotePageMap[key];
  delete remoteKeywordMap[key];
  remoteLoadingMap[key] = false;
}

function invalidateRemoteOptions(scope: FieldScope) {
  Object.keys(remoteRequestMap).filter(key => key.startsWith(`${scope}_`)).forEach(invalidateRemoteField);
}

function getRemoteLoading(scope: FieldScope, prop: string) {
  return !!remoteLoadingMap[getRemoteKey(scope, prop)];
}

function getFieldModel(scope: FieldScope) {
  return scope === 'form' ? form.value : queryParams.value;
}

function mergeOptions(current: OptionItem[] = [], next: OptionItem[] = []) {
  const map = new Map<string, OptionItem>();
  [...current, ...next].forEach((item) => {
    if (!item) {
      return;
    }
    map.set(String(item.value), item);
  });
  return Array.from(map.values());
}

function getFieldOptions(field: FieldConfig, scope: FieldScope) {
  if (!isRemoteField(field)) {
    return field.options || [];
  }
  return remoteOptionsMap[getRemoteKey(scope, field.prop)] || [];
}

function cellText(row: Record<string, any>, column: TableColumn) {
  const value = row[column.prop];
  if (value !== null && value !== undefined && value !== '') return value;
  return column.fallbackProp && row[column.fallbackProp] || '—';
}

function resolveOptionLabel(options: OptionItem[] | undefined, value: any) {
  const matched = options?.find((item) => String(item.value) === String(value));
  return matched?.label ?? value ?? '';
}

function resolveTagType(options: OptionItem[] | undefined, value: any, fallback?: TableColumn['tagType']) {
  const matched = options?.find((item) => String(item.value) === String(value));
  if (matched?.elTagType && matched.elTagType !== 'default') {
    return matched.elTagType as TableColumn['tagType'];
  }
  return fallback;
}

function resolveTagClass(options: OptionItem[] | undefined, value: any) {
  return options?.find((item) => String(item.value) === String(value))?.elTagClass || '';
}

async function loadRemoteOptions(field: FieldConfig, scope: FieldScope, keyword = '', pageNum = 1) {
  if (!isRemoteField(field)) {
    return;
  }
  const key = getRemoteKey(scope, field.prop);
  const model = getFieldModel(scope);
  const currentValue = model[field.prop];
  const requestId = (remoteRequestMap[key] || 0) + 1;
  remoteRequestMap[key] = requestId;
  remoteKeywordMap[key] = keyword;
  remoteLoadingMap[key] = true;
  try {
    const options = await field.remoteMethod({ keyword, pageNum, value: currentValue, model });
    if (remoteRequestMap[key] !== requestId) return;
    const selected = (Array.isArray(currentValue) ? currentValue : [currentValue]).map(String);
    const currentOptions = pageNum > 1 ? remoteOptionsMap[key] : (remoteOptionsMap[key] || []).filter((item) => selected.includes(String(item.value)));
    remoteOptionsMap[key] = mergeOptions(currentOptions, options || []);
    remoteMoreMap[key] = options.hasMore === true;
    remotePageMap[key] = pageNum;
  } finally {
    if (remoteRequestMap[key] === requestId) remoteLoadingMap[key] = false;
  }
}

function loadMore(field: FieldConfig, scope: FieldScope) {
  const key = getRemoteKey(scope, field.prop);
  if (!remoteLoadingMap[key]) loadRemoteOptions(field, scope, remoteKeywordMap[key], (remotePageMap[key] || 1) + 1);
}

async function ensureRemoteOption(field: FieldConfig, scope: FieldScope) {
  if (!isRemoteField(field)) {
    return;
  }
  const model = getFieldModel(scope);
  const currentValue = model[field.prop];
  const emptyArray = Array.isArray(currentValue) && currentValue.length === 0;
  if (currentValue === undefined || currentValue === null || currentValue === '' || emptyArray) {
    return;
  }
  await loadRemoteOptions(field, scope);
}

function handleRemoteSearch(field: FieldConfig, scope: FieldScope, keyword: string) {
  loadRemoteOptions(field, scope, keyword);
}

function handleRemoteVisibleChange(field: FieldConfig, scope: FieldScope, visible: boolean) {
  if (!visible) {
    return;
  }
  loadRemoteOptions(field, scope);
}

let textSearchTimer: ReturnType<typeof setTimeout> | undefined;
function cancelTextSearch() {
  if (textSearchTimer !== undefined) clearTimeout(textSearchTimer);
  textSearchTimer = undefined;
}
function onTextInput(value: string) {
  cancelTextSearch();
  if (!value) return handleQuery();
  textSearchTimer = setTimeout(handleQuery, 300);
}
function setDateStart(field: QueryField, day: string | null) {
  const query = queryParams.value;
  const previousStart = query[field.prop];
  const endKey = field.endProp!;
  const previousEnd = query[endKey];
  query[field.prop] = day || undefined;
  if (!day || !previousEnd || previousEnd === previousStart || previousEnd < day) {
    query[endKey] = query[field.prop];
  }
  return onFilterChange(field);
}

function onFilterChange(changed: QueryField) {
  for (const field of props.queryFields) {
    if (field.dependsOn !== changed.prop) continue;
    queryParams.value[field.prop] = props.initialQuery[field.prop];
    invalidateRemoteField(getRemoteKey('query', field.prop));
  }
  return handleQuery();
}

let listRequest = 0;
const getList = async () => {
  cancelTextSearch();
  const requestId = ++listRequest;
  loading.value = true;
  try {
    const res = await props.listFn({ ...queryParams.value });
    if (requestId !== listRequest) return;
    list.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    if (requestId === listRequest) loading.value = false;
  }
};

const reset = () => {
  editRequest += 1;
  saveError.value = '';
  invalidateRemoteOptions('form');
  dialog.visible = false;
  formRef.value?.resetFields();
  form.value = applyContext(cloneValue(props.initialForm));
};

const cancel = () => {
  reset();
};

const closeDialog = (done: () => void) => {
  if (saving.value) return;
  cancel();
  done();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  return getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  invalidateRemoteOptions('query');
  queryParams.value = applyContext(cloneValue(props.initialQuery));
  props.queryFields.forEach((field) => ensureRemoteOption(field, 'query'));
  handleQuery();
};

const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection;
};

const handleAdd = async () => {
  reset();
  dialog.visible = true;
  dialog.title = `新增${props.title}`;
  await Promise.all(props.formFields.map((field) => ensureRemoteOption(field, 'form')));
};

const handleUpdate = async (row?: any) => {
  reset();
  const requestId = editRequest;
  const id = row?.[props.idKey] ?? ids.value[0];
  const { data } = await props.getFn(id);
  if (requestId !== editRequest) return;
  form.value = { ...form.value, ...data };
  await Promise.all(props.formFields.map((field) => ensureRemoteOption(field, 'form')));
  if (requestId !== editRequest) return;
  dialog.visible = true;
  dialog.title = `修改${props.title}`;
};

const saving = ref(false);
const uploads = ref(new Set<string>());
const saveError = ref('');
const submitForm = () => {
  if (saving.value || uploads.value.size) return;
  const requestId = editRequest;
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || saving.value || requestId !== editRequest) {
      return;
    }
    saving.value = true;
    saveError.value = '';
    try {
      const data = cloneValue(form.value);
      await (data[props.idKey] ? props.updateFn(data) : props.addFn(data));
      if (requestId !== editRequest) return;
      proxy?.$modal.msgSuccess('操作成功');
      reset();
      await getList();
    } catch (error) {
      if (requestId === editRequest) saveError.value = error instanceof Error ? error.message : '保存失败，请重试';
    } finally {
      saving.value = false;
    }
  });
};

const handleDelete = async (row?: any) => {
  const targetIds = row?.[props.idKey] ?? ids.value;
  const deleteTargetText = Array.isArray(targetIds) ? `选中的 ${targetIds.length} 条` : '当前';
  await proxy?.$modal.confirm(`是否确认删除${deleteTargetText}${props.title}数据？`);
  await props.delFn(targetIds);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

const handleExport = () => {
  if (!props.exportUrl) {
    return;
  }
  proxy?.download(props.exportUrl, { ...queryParams.value }, `${props.permPrefix.replace(/:/g, '_')}_${new Date().getTime()}.xlsx`);
};

onMounted(() => {
  getList();
  props.queryFields.forEach((field) => ensureRemoteOption(field, 'query'));
});

watch(() => [route.path, route.query], () => {
  detailRow.value = undefined;
  reset();
  invalidateRemoteOptions('query');
  queryParams.value = applyContext(cloneValue(props.initialQuery));
  getList();
  props.queryFields.forEach((field) => ensureRemoteOption(field, 'query'));
});

onBeforeUnmount(() => {
  cancelTextSearch();
  editRequest += 1;
  listRequest += 1;
  invalidateRemoteOptions('form');
  invalidateRemoteOptions('query');
});
</script>

<style scoped>
.query-date-range { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); }
.query-date-range :deep(.el-input) { width: 136px; }
@media (max-width: 600px) {
  .date-range-filter { display: flex; flex-direction: column; width: 100%; margin-right: 0; }
  .date-range-filter :deep(.el-form-item__label) { justify-content: flex-start; }
  .query-date-range { flex: 1; min-width: 0; }
  .query-date-range :deep(.el-input) { flex: 1; width: 0; min-width: 0; }
}
.record-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 4px 9px; }
.record-actions :deep(.el-button) { margin: 0; }
</style>
