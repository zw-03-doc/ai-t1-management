<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row, refresh }">
      <el-tooltip v-if="row.checkinStatus !== 'reversed' && !row.operatorUserId" content="确认核销" placement="top">
        <el-button v-hasPermi="['fitness:checkin:edit']" link type="success" icon="CircleCheck" @click="handleConfirm(row, refresh)" />
      </el-tooltip>
      <el-tooltip v-if="row.checkinStatus !== 'reversed' && !row.operatorUserId" content="标记异常" placement="top">
        <el-button v-hasPermi="['fitness:checkin:edit']" link type="danger" icon="Warning" @click="handleException(row, refresh)" />
      </el-tooltip>
      <el-button
        v-if="row.checkinStatus === 'reversed'"
        v-hasPermi="['fitness:checkin:edit']"
        link
        type="primary"
        @click="handleRestore(row, refresh)"
        >恢复待核销</el-button
      >
    </template>
  </FitnessCrudPage>
  <el-dialog v-model="confirmDialog.visible" title="确认到课与权益核销" width="480px" append-to-body>
    <p>{{ confirmDialog.row.memberName }} · {{ confirmDialog.row.courseName }}</p>
    <p>{{ confirmDialog.row.cardName || '无卡体验' }}</p>
    <el-form label-width="110px">
      <el-form-item v-if="consumptionMode === 'amount'" label="本次扣款(元)"
        ><el-input-number v-model="confirmDialog.amount" :min="0.01" :max="Number(confirmDialog.row.balanceAmount || 0)" :precision="2"
      /></el-form-item>
      <el-form-item v-if="consumptionMode === 'times'" label="本次扣课次"
        ><el-input-number v-model="confirmDialog.times" :min="1" :max="Number(confirmDialog.row.remainTimes || 0)" :precision="0"
      /></el-form-item>
      <el-form-item label="核销后权益"
        ><span>{{ remainingText }}</span></el-form-item
      >
      <el-form-item label="备注"><el-input v-model="confirmDialog.remark" maxlength="500" /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="confirmDialog.visible = false">取消</el-button
      ><el-button type="primary" :loading="confirmDialog.saving" @click="submitConfirm">确认核销</el-button></template
    >
  </el-dialog>
</template>

<script setup name="FitnessCheckin" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { ElMessageBox } from 'element-plus';
import { confirmCheckin, getCheckin, listCheckin, markCheckinException, restoreCheckin } from '@/api/fitness/checkin';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const statusOptions = [
  { label: '已签到', value: 'success' },
  { label: '已异常', value: 'reversed' }
];
const progressOptions = [
  { label: '待核销', value: 'pending', elTagType: 'warning' },
  { label: '已核销', value: 'confirmed', elTagType: 'success' },
  { label: '异常待处理', value: 'exception', elTagType: 'danger' }
];

const noop = async () => ({ code: 200, data: {} });

const pageConfig = reactive({
  title: '签到核销',
  permPrefix: 'fitness:checkin',
  idKey: 'checkinId',
  exportUrl: 'fitness/checkin/export',
  allowCreate: false,
  allowEdit: false,
  allowDelete: false,
  queryFields: [
    { label: '签到编号', prop: 'checkinNo' },
    { label: '关键字', prop: 'keyword' },
    { label: '签到类型', prop: 'checkinType' },
    { label: '签到状态', prop: 'checkinStatus', type: 'select', options: statusOptions }
  ],
  formFields: [],
  tableColumns: [
    { label: '签到编号', prop: 'checkinNo' },
    { label: '预约编号', prop: 'reservationNo' },
    { label: '门店', prop: 'storeName' },
    { label: '课程', prop: 'courseName' },
    { label: '教练', prop: 'coachName' },
    { label: '会员', prop: 'memberName' },
    { label: '扣减卡项', prop: 'cardName' },
    { label: '签到时间', prop: 'checkinTime', type: 'date' },
    { label: '消耗次数', prop: 'consumeTimes' },
    { label: '消耗金额', prop: 'consumeAmount' },
    { label: '核销人', prop: 'operatorUserName' },
    { label: '核销进度', prop: 'redemptionState', type: 'tag', options: progressOptions },
    { label: '签到状态', prop: 'checkinStatus', type: 'tag', options: statusOptions }
  ],
  initialQuery: {
    memberId: '',
    storeId: '',
    scheduleId: '',
    pageNum: 1,
    pageSize: 10,
    checkinNo: '',
    keyword: '',
    checkinType: '',
    checkinStatus: ''
  },
  initialForm: {},
  listFn: async (query: any) => {
    const response: any = await listCheckin(query);
    return {
      ...response,
      rows: (response.rows || []).map((row: any) => ({
        ...row,
        redemptionState: row.checkinStatus === 'reversed' ? 'exception' : row.operatorUserId ? 'confirmed' : 'pending'
      }))
    };
  },
  getFn: getCheckin,
  addFn: noop,
  updateFn: noop,
  delFn: noop
});

const handleConfirm = async (row: any, refresh: () => Promise<void>) => {
  const response = await getCheckin(row.checkinId);
  confirmDialog.row = response.data;
  confirmDialog.times = 1;
  confirmDialog.amount = undefined;
  confirmDialog.remark = '';
  confirmRefresh.value = refresh;
  confirmDialog.visible = true;
};
const confirmRefresh = shallowRef<() => Promise<void>>();
const confirmDialog = reactive({ visible: false, saving: false, row: {} as any, times: 1, amount: undefined as number | undefined, remark: '' });
const consumptionMode = computed(() =>
  !confirmDialog.row.cardId || confirmDialog.row.planType === 'time_card' ? 'free' : confirmDialog.row.planType === 'stored_card' ? 'amount' : 'times'
);
const remainingText = computed(() =>
  consumptionMode.value === 'amount'
    ? `¥${(Number(confirmDialog.row.balanceAmount || 0) - Number(confirmDialog.amount || 0)).toFixed(2)}`
    : consumptionMode.value === 'times'
      ? `${Number(confirmDialog.row.remainTimes || 0) - Number(confirmDialog.times || 0)}次`
      : '验证到课，不扣课次或金额'
);
const submitConfirm = async () => {
  if (confirmDialog.saving) return;
  if (consumptionMode.value === 'amount' && (!confirmDialog.amount || confirmDialog.amount <= 0)) {
    proxy?.$modal.msgError('请输入本次实际扣款金额');
    return;
  }
  if (consumptionMode.value === 'amount' && Number(confirmDialog.amount) > Number(confirmDialog.row.balanceAmount || 0)) {
    proxy?.$modal.msgError('扣款金额不能超过卡内余额');
    return;
  }
  if (
    consumptionMode.value === 'times' &&
    (!Number.isInteger(confirmDialog.times) || confirmDialog.times <= 0 || confirmDialog.times > Number(confirmDialog.row.remainTimes || 0))
  ) {
    proxy?.$modal.msgError('请输入有效核销课次');
    return;
  }
  try {
    confirmDialog.saving = true;
    await confirmCheckin(confirmDialog.row.checkinId, {
      consumeTimes: consumptionMode.value === 'times' ? confirmDialog.times : 0,
      consumeAmount: consumptionMode.value === 'amount' ? confirmDialog.amount : 0,
      remark: confirmDialog.remark
    });
    proxy?.$modal.msgSuccess('核销成功');
    confirmDialog.visible = false;
    await confirmRefresh.value?.();
  } finally {
    confirmDialog.saving = false;
  }
};

const handleException = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await ElMessageBox.prompt('请填写异常原因（必填）', '标记异常', {
      inputValidator: (text: string) => !!text?.trim() || '请填写原因'
    })!;
    await markCheckinException(row.checkinId, value);
    proxy?.$modal.msgSuccess('异常标记成功');
    await refresh();
  } catch {
    // 用户取消
  }
};
const handleRestore = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await ElMessageBox.prompt('请填写恢复原因。恢复后需要重新确认核销。', '恢复异常签到', {
      inputValidator: (text: string) => !!text?.trim() || '请填写原因'
    })!;
    await restoreCheckin(row.checkinId, value);
    proxy?.$modal.msgSuccess('已恢复待核销，处理原因已记录');
    await refresh();
  } catch {
    /* 取消或统一请求错误提示 */
  }
};
</script>
