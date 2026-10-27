<template>
  <div>
    <el-alert
      title="排课时间均为北京时间；人数和价格为本场次设置，修改课程默认值不会改动既有场次。预约开始、截止留空时按课程规则计算。"
      type="info"
      :closable="false"
      class="mb-4"
    />
    <FitnessCrudPage v-bind="pageConfig as any">
      <template #header-actions="{ refresh }">
        <el-col :span="2">
          <el-button v-hasPermi="['fitness:schedule:add']" type="success" plain icon="Calendar" @click="openBatchDialog(refresh)">
            批量日期行
          </el-button>
        </el-col>
      </template>

      <template #row-actions="{ row, refresh }">
        <el-tooltip v-if="row.scheduleStatus !== 'cancelled'" content="取消排课" placement="top">
          <el-button v-hasPermi="['fitness:schedule:edit']" link type="danger" icon="CircleClose" @click="handleCancel(row, refresh)" />
        </el-tooltip>
      </template>
    </FitnessCrudPage>

    <el-dialog v-model="batchDialog.visible" title="批量日期行排课" width="980px" append-to-body>
      <el-form label-width="110px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="门店">
              <el-select
                v-model="batchForm.storeId"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="loadBatchStoreOptions"
                @visible-change="onBatchStoreVisibleChange"
              >
                <el-option v-for="opt in batchStoreOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程">
              <el-select
                v-model="batchForm.courseId"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="loadBatchCourseOptions"
                @visible-change="onBatchCourseVisibleChange"
              >
                <el-option v-for="opt in batchCourseOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="教练">
              <el-select
                v-model="batchForm.coachId"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="loadBatchCoachOptions"
                @visible-change="onBatchCoachVisibleChange"
              >
                <el-option v-for="opt in batchCoachOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="排课模式">
              <el-select v-model="batchForm.scheduleMode" clearable>
                <el-option v-for="opt in modeOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="可预约人数">
              <el-input-number v-model="batchForm.capacity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="预约截止">
              <el-date-picker v-model="batchForm.reservationDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="取消截止">
              <el-date-picker v-model="batchForm.cancelDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排课状态">
              <el-select v-model="batchForm.scheduleStatus" clearable>
                <el-option v-for="opt in statusOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="batchForm.remark" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="mb-3 flex items-center justify-between">
        <div class="text-gray-500">按行编辑多个日期与时间段，统一创建排课。</div>
        <el-button type="primary" plain icon="Plus" @click="addBatchRow">新增日期行</el-button>
      </div>

      <el-table :data="batchForm.rows" border>
        <el-table-column label="序号" type="index" width="70" align="center" />
        <el-table-column label="排课日期" min-width="180">
          <template #default="scope">
            <el-date-picker v-model="scope.row.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="180">
          <template #default="scope">
            <el-time-picker v-model="scope.row.startClock" value-format="HH:mm:ss" format="HH:mm:ss" placeholder="开始时间" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="scope">
            <el-time-picker v-model="scope.row.endClock" value-format="HH:mm:ss" format="HH:mm:ss" placeholder="结束时间" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column v-if="batchForm.scheduleMode !== 'normal'" label="本行学员" min-width="210"
          ><template #default="scope"
            ><el-select v-model="scope.row.studentMemberIds" multiple filterable remote :remote-method="loadBatchMemberOptions"
              ><el-option v-for="opt in batchMemberOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" /></el-select></template
        ></el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="scope">
            <el-button type="danger" link :disabled="batchForm.rows.length === 1" @click="removeBatchRow(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="batchDialog.submitting" type="primary" @click="submitBatchCreate">批量创建</el-button>
          <el-button :disabled="batchDialog.submitting" @click="batchDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FitnessSchedule" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addSchedule, cancelSchedule, delSchedule, getSchedule, listSchedule, updateSchedule } from '@/api/fitness/schedule';
import { loadCoachOptions, loadCourseOptions, loadMemberOptions, loadStoreOptions } from '../components/remoteOptions';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

type OptionItem = { label: string; value: string | number | boolean };
type BatchRefreshFn = (() => Promise<void>) | null;

type BatchRow = {
  scheduleDate: string;
  startClock: string;
  endClock: string;
  studentMemberIds: (string | number)[];
};

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已开放', value: 'open' },
  { label: '已结束', value: 'finished' },
  { label: '已关闭', value: 'closed' },
  { label: '已取消', value: 'cancelled' }
];

const modeOptions = [
  { label: '普通排课', value: 'normal' },
  { label: '私教定向', value: 'private' },
  { label: '小班定向', value: 'small_group' }
];

const pageConfig = reactive({
  title: '排课',
  permPrefix: 'fitness:schedule',
  idKey: 'scheduleId',
  exportUrl: 'fitness/schedule/export',
  queryFields: [
    { label: '排课编号', prop: 'scheduleNo' },
    { label: '课程', prop: 'courseId', type: 'remoteSelect', remoteMethod: loadCourseOptions },
    { label: '教练', prop: 'coachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '排课模式', prop: 'scheduleMode', type: 'select', options: modeOptions },
    { label: '状态', prop: 'scheduleStatus', type: 'select', options: statusOptions }
  ],
  formFields: [
    { label: '排课编号', prop: 'scheduleNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '课程', prop: 'courseId', type: 'remoteSelect', remoteMethod: loadCourseOptions },
    { label: '教练', prop: 'coachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '排课模式', prop: 'scheduleMode', type: 'select', options: modeOptions },
    { label: '学员(私教/小班)', prop: 'studentMemberIds', type: 'remoteSelectMultiple', remoteMethod: loadMemberOptions },
    { label: '排课日期', prop: 'scheduleDate', type: 'date' },
    { label: '开始时间', prop: 'startTime', type: 'datetime' },
    { label: '结束时间', prop: 'endTime', type: 'datetime' },
    { label: '可预约人数', prop: 'capacity', type: 'number', min: 1 },
    { label: '展示价格(元)', prop: 'displayPrice', type: 'number', precision: 2 },
    { label: '预约开始', prop: 'reservationOpenTime', type: 'datetime' },
    { label: '预约截止', prop: 'reservationDeadline', type: 'datetime' },
    { label: '取消截止', prop: 'cancelDeadline', type: 'datetime' },
    { label: '排课状态', prop: 'scheduleStatus', type: 'select', options: statusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '排课编号', prop: 'scheduleNo' },
    { label: '门店', prop: 'storeName' },
    { label: '课程', prop: 'courseName' },
    { label: '教练', prop: 'coachName' },
    { label: '排课模式', prop: 'scheduleMode', type: 'tag', options: modeOptions },
    { label: '学员', prop: 'studentNames' },
    { label: '排课日期', prop: 'scheduleDate', type: 'date', format: '{y}-{m}-{d}' },
    { label: '开始时间', prop: 'startTime', type: 'date' },
    { label: '结束时间', prop: 'endTime', type: 'date' },
    { label: '可预约人数', prop: 'capacity' },
    { label: '已预约', prop: 'reservedCount' },
    { label: '已签到', prop: 'signedCount' },
    { label: '状态', prop: 'scheduleStatus', type: 'tag', options: statusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    scheduleNo: '',
    courseId: '',
    coachId: '',
    scheduleMode: '',
    scheduleStatus: ''
  },
  initialForm: {
    scheduleId: undefined,
    scheduleNo: '',
    storeId: undefined,
    courseId: undefined,
    coachId: undefined,
    scheduleMode: 'normal',
    studentMemberIds: [],
    scheduleDate: '',
    startTime: '',
    endTime: '',
    capacity: 20,
    reservationDeadline: '',
    cancelDeadline: '',
    scheduleStatus: 'draft',
    remark: ''
  },
  rules: {
    storeId: [{ required: true, message: '门店不能为空', trigger: 'change' }],
    courseId: [{ required: true, message: '课程不能为空', trigger: 'change' }],
    scheduleDate: [{ required: true, message: '排课日期不能为空', trigger: 'change' }],
    startTime: [{ required: true, message: '开始时间不能为空', trigger: 'change' }],
    endTime: [{ required: true, message: '结束时间不能为空', trigger: 'change' }]
  },
  listFn: listSchedule,
  getFn: getSchedule,
  addFn: addSchedule,
  updateFn: updateSchedule,
  delFn: delSchedule
});

const batchDialog = reactive({
  visible: false,
  submitting: false
});

const batchForm = reactive({
  storeId: undefined as number | string | undefined,
  courseId: undefined as number | string | undefined,
  coachId: undefined as number | string | undefined,
  scheduleMode: 'normal',
  studentMemberIds: [] as Array<number | string>,
  capacity: 20,
  reservationDeadline: '',
  cancelDeadline: '',
  scheduleStatus: 'draft',
  remark: '',
  rows: [{ scheduleDate: '', startClock: '', endClock: '', studentMemberIds: [] }] as BatchRow[]
});

const batchStoreOptions = ref<OptionItem[]>([]);
const batchCourseOptions = ref<OptionItem[]>([]);
const batchCoachOptions = ref<OptionItem[]>([]);
const batchMemberOptions = ref<OptionItem[]>([]);

const batchRefresh = ref<BatchRefreshFn>(null);

const createBatchRow = (): BatchRow => ({
  scheduleDate: '',
  startClock: '',
  endClock: '',
  studentMemberIds: []
});

const resetBatchForm = () => {
  batchForm.storeId = undefined;
  batchForm.courseId = undefined;
  batchForm.coachId = undefined;
  batchForm.scheduleMode = 'normal';
  batchForm.studentMemberIds = [];
  batchForm.capacity = 20;
  batchForm.reservationDeadline = '';
  batchForm.cancelDeadline = '';
  batchForm.scheduleStatus = 'draft';
  batchForm.remark = '';
  batchForm.rows = [createBatchRow()];
};

const openBatchDialog = async (refresh: () => Promise<void>) => {
  resetBatchForm();
  batchRefresh.value = refresh;
  batchDialog.visible = true;
  await Promise.all([loadBatchStoreOptions(''), loadBatchCourseOptions(''), loadBatchCoachOptions(''), loadBatchMemberOptions('')]);
};

const onBatchStoreVisibleChange = (visible: boolean) => {
  if (visible && !batchStoreOptions.value.length) {
    loadBatchStoreOptions('');
  }
};

const onBatchCourseVisibleChange = (visible: boolean) => {
  if (visible && !batchCourseOptions.value.length) {
    loadBatchCourseOptions('');
  }
};

const onBatchCoachVisibleChange = (visible: boolean) => {
  if (visible && !batchCoachOptions.value.length) {
    loadBatchCoachOptions('');
  }
};

const onBatchMemberVisibleChange = (visible: boolean) => {
  if (visible && !batchMemberOptions.value.length) {
    loadBatchMemberOptions('');
  }
};

const loadBatchStoreOptions = async (keyword: string) => {
  batchStoreOptions.value = await loadStoreOptions({ keyword, value: batchForm.storeId, model: batchForm });
};

const loadBatchCourseOptions = async (keyword: string) => {
  batchCourseOptions.value = await loadCourseOptions({ keyword, value: batchForm.courseId, model: batchForm });
};

const loadBatchCoachOptions = async (keyword: string) => {
  batchCoachOptions.value = await loadCoachOptions({ keyword, value: batchForm.coachId, model: batchForm });
};

const loadBatchMemberOptions = async (keyword: string) => {
  batchMemberOptions.value = await loadMemberOptions({ keyword, value: batchForm.studentMemberIds, model: batchForm });
};

const addBatchRow = () => {
  const firstRow = batchForm.rows[0] || createBatchRow();
  batchForm.rows.push({
    scheduleDate: '',
    startClock: firstRow.startClock,
    endClock: firstRow.endClock,
    studentMemberIds: [...firstRow.studentMemberIds]
  });
};

const removeBatchRow = (index: number) => {
  if (batchForm.rows.length <= 1) {
    return;
  }
  batchForm.rows.splice(index, 1);
};

const buildDateTime = (date: string, clock: string) => `${date} ${clock}`;

const validateBatchForm = () => {
  if (!batchForm.storeId) {
    throw new Error('请先选择门店');
  }
  if (!batchForm.courseId) {
    throw new Error('请先选择课程');
  }
  if (!batchForm.rows.length) {
    throw new Error('请至少添加1条日期行');
  }
  const needStudents = batchForm.scheduleMode === 'private' || batchForm.scheduleMode === 'small_group';
  batchForm.rows.forEach((row, idx) => {
    if (!row.scheduleDate || !row.startClock || !row.endClock) {
      throw new Error(`第${idx + 1}行日期或时间未填写完整`);
    }
    const start = new Date(buildDateTime(row.scheduleDate, row.startClock)).getTime();
    const end = new Date(buildDateTime(row.scheduleDate, row.endClock)).getTime();
    if (!start || !end || Number.isNaN(start) || Number.isNaN(end)) {
      throw new Error(`第${idx + 1}行日期时间格式不正确`);
    }
    if (end <= start) {
      throw new Error(`第${idx + 1}行结束时间必须晚于开始时间`);
    }
    if (needStudents) {
      const count = row.studentMemberIds?.length || 0;
      if (count < 1 || count > batchForm.capacity) {
        throw new Error(`第${idx + 1}行学员需选择1-${batchForm.capacity}名`);
      }
    }
  });
};

const submitBatchCreate = async () => {
  if (batchDialog.submitting) {
    return;
  }
  try {
    validateBatchForm();
    batchDialog.submitting = true;
    let successCount = 0;
    for (let i = 0; i < batchForm.rows.length; i++) {
      const row = batchForm.rows[i];
      await addSchedule({
        scheduleNo: '',
        storeId: batchForm.storeId,
        courseId: batchForm.courseId,
        coachId: batchForm.coachId,
        scheduleMode: batchForm.scheduleMode,
        studentMemberIds: row.studentMemberIds,
        scheduleDate: row.scheduleDate,
        startTime: buildDateTime(row.scheduleDate, row.startClock),
        endTime: buildDateTime(row.scheduleDate, row.endClock),
        capacity: batchForm.capacity,
        reservationDeadline: batchForm.reservationDeadline || undefined,
        cancelDeadline: batchForm.cancelDeadline || undefined,
        scheduleStatus: batchForm.scheduleStatus,
        remark: batchForm.remark
      });
      successCount += 1;
    }
    proxy?.$modal.msgSuccess(`批量创建成功，共${successCount}条`);
    batchDialog.visible = false;
    if (batchRefresh.value) {
      await batchRefresh.value();
    }
  } catch (error: any) {
    proxy?.$modal.msgError(error?.message || '批量创建失败');
  } finally {
    batchDialog.submitting = false;
  }
};

const handleCancel = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('请输入取消原因，可留空')!;
    await cancelSchedule(row.scheduleId, value);
    proxy?.$modal.msgSuccess('排课已取消');
    await refresh();
  } catch {
    // 用户取消
  }
};
</script>
