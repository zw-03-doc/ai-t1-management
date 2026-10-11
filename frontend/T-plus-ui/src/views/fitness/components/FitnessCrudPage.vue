<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item v-for="field in queryFields" :key="field.prop" :label="field.label" :prop="field.prop">
              <el-input
                v-if="field.type === 'input' || !field.type"
                v-model="queryParams[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                clearable
                @keyup.enter="handleQuery"
              />
              <el-select v-else-if="field.type === 'select'" v-model="queryParams[field.prop]" clearable :placeholder="field.placeholder || `请选择${field.label}`">
                <el-option v-for="option in field.options || []" :key="String(option.value)" :label="option.label" :value="option.value" />
              </el-select>
              <el-select
                v-else-if="['remoteSelect', 'remoteSelectMultiple'].includes(field.type || '')"
                v-model="queryParams[field.prop]"
                :multiple="['remoteSelectMultiple'].includes(field.type || '')"
                clearable
                filterable
                remote
                reserve-keyword
                :loading="getRemoteLoading('query', field.prop)"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :remote-method="(keyword) => handleRemoteSearch(field, 'query', keyword)"
                @visible-change="(visible) => handleRemoteVisibleChange(field, 'query', visible)"
              >
                <el-option v-for="option in getFieldOptions(field, 'query')" :key="String(option.value)" :label="option.label" :value="option.value" />
              </el-select>

              <el-date-picker

                v-else-if="field.type === 'date'"
                v-model="queryParams[field.prop]"
                type="date"
                value-format="YYYY-MM-DD"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              />
              <el-date-picker
                v-else-if="field.type === 'datetime'"
                v-model="queryParams[field.prop]"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                clearable
                :placeholder="field.placeholder || `请选择${field.label}`"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col v-if="allowCreate" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:add`]" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col v-if="allowDelete" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:remove`]" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">批量删除</el-button>
          </el-col>
          <el-col v-if="exportUrl" :span="1.5">
            <el-button v-hasPermi="[`${permPrefix}:export`]" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
          </el-col>
          <slot name="header-actions" :refresh="getList" />
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="list" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-for="column in tableColumns" :key="column.prop" :label="column.label" align="center" :prop="column.prop" :width="column.width" :show-overflow-tooltip="column.showOverflow !== false">
          <template #default="scope">
            <span v-if="column.type === 'date'">{{ proxy?.parseTime(scope.row[column.prop], column.format || '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <el-tag
              v-else-if="column.type === 'tag'"
              :type="resolveTagType(column.options, scope.row[column.prop], column.tagType)"
              :class="resolveTagClass(column.options, scope.row[column.prop])"
            >{{ resolveOptionLabel(column.options, scope.row[column.prop]) }}</el-tag>
            <span v-else>{{ scope.row[column.prop] }}</span>

          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
          <template #default="scope">
            <el-tooltip v-if="allowEdit" content="修改" placement="top">
              <el-button v-hasPermi="[`${permPrefix}:edit`]" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip v-if="allowDelete" content="删除" placement="top">
              <el-button v-hasPermi="[`${permPrefix}:remove`]" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
            <slot name="row-actions" :row="scope.row" :refresh="getList"></slot>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="820px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col v-for="field in formFields" :key="field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.prop">
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
              <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" :min="field.min ?? 0" style="width: 100%" />
              <el-select v-else-if="field.type === 'select'" v-model="form[field.prop]" clearable :placeholder="field.placeholder || `请选择${field.label}`">
                <el-option v-for="option in field.options || []" :key="String(option.value)" :label="option.label" :value="option.value" />
              </el-select>
              <el-select
                v-else-if="field.type === 'remoteSelect'"
                v-model="form[field.prop]"
                clearable
                filterable
                remote
                reserve-keyword
                :loading="getRemoteLoading('form', field.prop)"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :remote-method="(keyword) => handleRemoteSearch(field, 'form', keyword)"
                @visible-change="(visible) => handleRemoteVisibleChange(field, 'form', visible)"
              >
                <el-option v-for="option in getFieldOptions(field, 'form')" :key="String(option.value)" :label="option.label" :value="option.value" />
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
              <ImageUpload v-else-if="field.type === 'imageUpload'" v-model="form[field.prop]" :limit="field.limit ?? 1" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FitnessCrudPage" lang="ts">
interface OptionItem {
  label: string;
  value: string | number | boolean;
  elTagType?: string;
  elTagClass?: string;
}


interface RemoteOptionRequest {
  keyword?: string;
  value?: string | number | boolean | null | undefined;
  model?: Record<string, any>;
}

type RemoteMethod = (params: RemoteOptionRequest) => Promise<OptionItem[]>;
type QueryFieldType = 'input' | 'select' | 'remoteSelect' | 'date' | 'datetime';
type FormFieldType = 'input' | 'textarea' | 'number' | 'select' | 'remoteSelect' | 'remoteSelectMultiple' | 'date' | 'datetime' | 'imageUpload';

type FieldScope = 'query' | 'form';

interface QueryField {
  label: string;
  prop: string;
  type?: QueryFieldType;
  placeholder?: string;
  options?: OptionItem[];
  remoteMethod?: RemoteMethod;
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
  limit?: number;
}

interface TableColumn {
  label: string;
  prop: string;
  width?: string | number;
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
    queryFields: QueryField[];
    formFields: FormField[];
    tableColumns: TableColumn[];
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
const loading = ref(false);
const showSearch = ref(true);
const list = ref<any[]>([]);
const total = ref(0);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const queryFormRef = ref<ElFormInstance>();
const formRef = ref<ElFormInstance>();
const remoteOptionsMap = reactive<Record<string, OptionItem[]>>({});
const remoteLoadingMap = reactive<Record<string, boolean>>({});

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const queryParams = ref<Record<string, any>>(cloneValue(props.initialQuery));
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


async function loadRemoteOptions(field: FieldConfig, scope: FieldScope, keyword = '') {
  if (!isRemoteField(field)) {
    return;
  }
  const key = getRemoteKey(scope, field.prop);
  const model = getFieldModel(scope);
  const currentValue = model[field.prop];
  remoteLoadingMap[key] = true;
  try {
    const options = await field.remoteMethod({ keyword, value: currentValue, model });
    const currentOptions = currentValue === undefined || currentValue === null || currentValue === ''
      ? []
      : (remoteOptionsMap[key] || []).filter((item) => item.value === currentValue);
    remoteOptionsMap[key] = mergeOptions(currentOptions, options || []);
  } finally {
    remoteLoadingMap[key] = false;
  }
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
  const key = getRemoteKey(scope, field.prop);
  const options = await field.remoteMethod({ value: currentValue, model });
  remoteOptionsMap[key] = mergeOptions(remoteOptionsMap[key], options || []);
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

const getList = async () => {
  loading.value = true;
  try {
    const res = await props.listFn(queryParams.value);
    list.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  form.value = cloneValue(props.initialForm);
  formRef.value?.resetFields();
};

const cancel = () => {
  dialog.visible = false;
  reset();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.value = cloneValue(props.initialQuery);
  handleQuery();
};

const handleSelectionChange = (selection: any[]) => {
  ids.value = selection.map((item) => item[props.idKey]);
  single.value = selection.length !== 1;
  multiple.value = selection.length === 0;
};

const handleAdd = async () => {
  reset();
  dialog.visible = true;
  dialog.title = `新增${props.title}`;
  await Promise.all(props.formFields.map((field) => ensureRemoteOption(field, 'form')));
};

const handleUpdate = async (row?: any) => {
  reset();
  const id = row?.[props.idKey] ?? ids.value[0];
  const { data } = await props.getFn(id);
  form.value = { ...form.value, ...data };
  await Promise.all(props.formFields.map((field) => ensureRemoteOption(field, 'form')));
  dialog.visible = true;
  dialog.title = `修改${props.title}`;
};

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) {
      return;
    }
    form.value[props.idKey] ? await props.updateFn(form.value) : await props.addFn(form.value);
    proxy?.$modal.msgSuccess('操作成功');
    dialog.visible = false;
    await getList();
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
});
</script>

