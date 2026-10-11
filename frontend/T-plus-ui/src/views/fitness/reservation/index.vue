<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row, refresh }">
      <el-tooltip v-if="row.reservationStatus !== 'cancelled'" content="取消预约" placement="top">
        <el-button v-hasPermi="['fitness:reservation:edit']" link type="danger" icon="CircleClose" @click="handleCancel(row, refresh)" />
      </el-tooltip>
    </template>
  </FitnessCrudPage>
</template>

<script setup name="FitnessReservation" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addReservation, cancelReservation, delReservation, getReservation, listReservation, updateReservation } from '@/api/fitness/reservation';
import { loadMemberCardOptions, loadMemberOptions, loadScheduleOptions } from '../components/remoteOptions';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const sourceOptions = [
  { label: '后台', value: 'admin' },
  { label: '小程序', value: 'mini' },
  { label: '教练端', value: 'coach_app' }
];
const statusOptions = [
  { label: '已预约', value: 'reserved' },
  { label: '已签到', value: 'signed' },
  { label: '已取消', value: 'cancelled' },
  { label: '未到场', value: 'absent' },
  { label: '请假', value: 'leave' }
];
const reservationTypeOptions = [
  { label: '到店体验预约', value: 'store_experience' },
  { label: '卡项预约', value: 'card' }
];
const consumeModeOptions = [

  { label: '次卡扣次', value: 'times' },
  { label: '储值扣费', value: 'amount' },
  { label: '免费体验', value: 'free' }
];
const checkinStatusOptions = [
  { label: '已签到', value: 'success' },
  { label: '异常', value: 'reversed' }
];

const pageConfig = reactive({
  title: '预约',
  permPrefix: 'fitness:reservation',
  idKey: 'reservationId',
  exportUrl: 'fitness/reservation/export',
  queryFields: [
    { label: '预约编号', prop: 'reservationNo' },
    { label: '关键字', prop: 'keyword' },
    { label: '预约来源', prop: 'reservationSource', type: 'select', options: sourceOptions },
    { label: '预约类型', prop: 'reservationType', type: 'select', options: reservationTypeOptions },
    { label: '预约状态', prop: 'reservationStatus', type: 'select', options: statusOptions }

  ],
  formFields: [
    { label: '预约编号', prop: 'reservationNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '排课', prop: 'scheduleId', type: 'remoteSelect', remoteMethod: loadScheduleOptions },
    { label: '会员', prop: 'memberId', type: 'remoteSelect', remoteMethod: loadMemberOptions },
    { label: '会员卡', prop: 'cardId', type: 'remoteSelect', remoteMethod: loadMemberCardOptions },
    { label: '预约类型', prop: 'reservationType', type: 'select', options: reservationTypeOptions },
    { label: '预约来源', prop: 'reservationSource', type: 'select', options: sourceOptions },

    { label: '预约状态', prop: 'reservationStatus', type: 'select', options: statusOptions },
    { label: '扣费模式', prop: 'consumeMode', type: 'select', options: consumeModeOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '预约编号', prop: 'reservationNo' },
    { label: '排课编号', prop: 'scheduleNo' },
    { label: '门店', prop: 'storeName' },
    { label: '课程', prop: 'courseName' },
    { label: '教练', prop: 'coachName' },
    { label: '会员', prop: 'memberName' },
    { label: '手机号', prop: 'memberMobile' },
    { label: '预约类型', prop: 'reservationType', type: 'tag', options: reservationTypeOptions },
    { label: '排课日期', prop: 'scheduleDate', type: 'date', format: '{y}-{m}-{d}' },

    { label: '预约来源', prop: 'reservationSource', type: 'tag', options: sourceOptions },
    { label: '预约状态', prop: 'reservationStatus', type: 'tag', options: statusOptions },
    { label: '签到状态', prop: 'checkinStatus', type: 'tag', options: checkinStatusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    reservationNo: '',
    keyword: '',
    reservationSource: '',
    reservationType: '',
    reservationStatus: ''

  },
  initialForm: {
    reservationId: undefined,
    reservationNo: '',
    scheduleId: undefined,
    memberId: undefined,
    cardId: undefined,
    reservationType: 'card',
    reservationSource: 'admin',

    reservationStatus: 'reserved',
    consumeMode: 'times',
    remark: ''
  },
  rules: {
    scheduleId: [{ required: true, message: '排课不能为空', trigger: 'change' }],
    memberId: [{ required: true, message: '会员不能为空', trigger: 'change' }]
  },
  listFn: listReservation,
  getFn: getReservation,
  addFn: addReservation,
  updateFn: updateReservation,
  delFn: delReservation
});

const handleCancel = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('请输入取消原因，可留空')!;
    await cancelReservation(row.reservationId, value);
    proxy?.$modal.msgSuccess('预约已取消');
    await refresh();
  } catch {
    // 用户取消
  }
};
</script>

