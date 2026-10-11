<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row, refresh }">
      <el-tooltip v-if="row.checkinStatus !== 'reversed'" content="确认核销" placement="top">
        <el-button v-hasPermi="['fitness:checkin:edit']" link type="success" icon="CircleCheck" @click="handleConfirm(row, refresh)" />
      </el-tooltip>
      <el-tooltip v-if="row.checkinStatus !== 'reversed'" content="标记异常" placement="top">
        <el-button v-hasPermi="['fitness:checkin:edit']" link type="danger" icon="Warning" @click="handleException(row, refresh)" />
      </el-tooltip>
    </template>
  </FitnessCrudPage>
</template>

<script setup name="FitnessCheckin" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { confirmCheckin, getCheckin, listCheckin, markCheckinException } from '@/api/fitness/checkin';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const statusOptions = [
  { label: '已签到', value: 'success' },
  { label: '已异常', value: 'reversed' }
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
    { label: '签到状态', prop: 'checkinStatus', type: 'tag', options: statusOptions }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    checkinNo: '',
    keyword: '',
    checkinType: '',
    checkinStatus: ''
  },
  initialForm: {},
  listFn: listCheckin,
  getFn: getCheckin,
  addFn: noop,
  updateFn: noop,
  delFn: noop
});

const handleConfirm = async (row: any, refresh: () => Promise<void>) => {
  try {
    await proxy?.$modal.confirm(`确认核销会员【${row.memberName || row.memberNo || '该会员'}】的签到记录吗？`);
    await confirmCheckin(row.checkinId, {});
    proxy?.$modal.msgSuccess('核销成功');
    await refresh();
  } catch {
    // 用户取消
  }
};


const handleException = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('请输入异常说明，可留空')!;
    await markCheckinException(row.checkinId, value);
    proxy?.$modal.msgSuccess('异常标记成功');
    await refresh();
  } catch {
    // 用户取消
  }
};
</script>
