<template>
  <div>
    <FitnessCrudPage v-bind="pageConfig as any">
      <template #field-sessions="{ model }"><ScheduleTimes :model="model" /></template>
      <template #submit-label="{ model }">{{ model.scheduleId ? '保存场次' : '创建排课' + (model.sessions?.length ? '（' + model.sessions.length + ' 场）' : '') }}</template>
      <template #row-actions="{ row, refresh }">
        <el-button
          v-hasPermi="['fitness:reservation:list']"
          link
          type="primary"
          @click="openFitnessPage(router, 'reservation', { scheduleId: row.scheduleId })"
          >处理预约</el-button
        >
        <el-button
          v-if="['open', 'closed'].includes(row.scheduleStatus)"
          v-hasPermi="['fitness:schedule:edit']"
          link
          type="success"
          @click="handleFinish(row, refresh)"
          >结束场次</el-button
        >
        <el-tooltip v-if="!['cancelled', 'finished'].includes(row.scheduleStatus)" content="取消排课" placement="top">
          <el-button v-hasPermi="['fitness:schedule:edit']" link type="danger" icon="CircleClose" @click="handleCancel(row, refresh)" />
        </el-tooltip>
      </template>
    </FitnessCrudPage>


  </div>
</template>

<script setup name="FitnessSchedule" lang="ts">
import ScheduleTimes from './ScheduleTimes.vue';
import { scheduleRequests, draftError, timeRangeLabel } from './sessions';
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { openFitnessPage } from '../components/navigation';
const router = useRouter();
import { addScheduleBatch, cancelSchedule, delSchedule, getSchedule, listSchedule, updateSchedule, finishSchedule } from '@/api/fitness/schedule';
import { loadCoachOptions, loadCourseOptions, loadMemberOptions, loadStoreOptions } from '../components/remoteOptions';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const handleFinish = async (row: any, refresh: () => Promise<void>) => {
  try {
    await proxy?.$modal.confirm('确认结束本场次？所有预约须已核销或登记缺席/请假，历史记录和人数将保留。');
    await finishSchedule(row.scheduleId);
    proxy?.$modal.msgSuccess('场次已结束');
    await refresh();
  } catch {
    /* 取消或统一请求错误提示 */
  }
};

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已开放', value: 'open' },
  { label: '已结束', value: 'finished' },
  { label: '已关闭', value: 'closed' },
  { label: '已取消', value: 'cancelled' }
];

const modeOptions = [
  { label: '公开预约', value: 'normal' },
  { label: '指定学员', value: 'directed' }
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
    { label: '排课编号', prop: 'scheduleNo', placeholder: '选填，多场次自动编号' },
    { label: '门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '课程', prop: 'courseId', type: 'remoteSelect', remoteMethod: loadCourseOptions },
    { label: '教练', prop: 'coachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '排课模式', prop: 'scheduleMode', type: 'select', options: modeOptions },
    { label: '指定学员名单', prop: 'studentMemberIds', type: 'remoteSelectMultiple', remoteMethod: loadMemberOptions },
    { label: '日期与时间段', prop: 'sessions', type: 'custom', span: 24 },
    { label: '可预约人数', prop: 'capacity', type: 'number', min: 1 },
    { label: '展示价格(元)', prop: 'displayPrice', type: 'number', precision: 2 },
    { label: '预约开始', prop: 'reservationOpenTime', type: 'datetime' },
    { label: '预约截止', prop: 'reservationDeadline', type: 'datetime' },
    { label: '取消截止', prop: 'cancelDeadline', type: 'datetime' },
    { label: '排课状态', prop: 'scheduleStatus', type: 'select', options: statusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '课程', prop: 'courseName', minWidth: 164 },
    { label: '教练', prop: 'coachName' },
    { label: '门店', prop: 'storeName', minWidth: 126 },
    { label: '日期', prop: 'scheduleDate', type: 'date', format: '{y}-{m}-{d}', minWidth: 105 },
    { label: '时间段', prop: 'timeRange', minWidth: 120 },
    { label: '已预约', prop: 'reservedCount' },
    { label: '可预约人数', prop: 'capacity' },
    { label: '状态', prop: 'scheduleStatus', type: 'tag', options: statusOptions }
  ],
  detailColumns: [
    { label: '排课编号', prop: 'scheduleNo' },
    { label: '排课模式', prop: 'scheduleMode', type: 'tag', options: modeOptions },
    { label: '学员', prop: 'studentNames' },
    { label: '开始时间', prop: 'startTime', type: 'date' },
    { label: '结束时间', prop: 'endTime', type: 'date' },
    { label: '已签到', prop: 'signedCount' },
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
    sessions: [{ validator: (_rule: unknown, rows: any[], done: (error?: Error) => void) => {
      const message = draftError(rows);
      done(message ? new Error(message) : undefined);
    } }]
  },
  listFn: async (query: any) => {
    const result: any = await listSchedule(query);
    return { ...result, rows: (result.rows || []).map((row: any) => ({ ...row, timeRange: timeRangeLabel(row) })) };
  },
  getFn: getSchedule,
  addFn: (form: any) => addScheduleBatch(scheduleRequests(form)),
  updateFn: (form: any) => updateSchedule(scheduleRequests(form)[0]),
  delFn: delSchedule
});

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
