<template>
  <div class="p-4">
    <el-card
      ><template #header
        ><div class="flex justify-between"><span>行为记录</span><el-button :loading="loading" @click="load">刷新</el-button></div></template
      ><el-table v-loading="loading" :data="rows"
        ><el-table-column prop="createdAt" label="时间（北京时间）" min-width="170" /><el-table-column
          prop="actorName"
          label="操作人" /><el-table-column prop="actorType" label="身份" /><el-table-column
          prop="action"
          label="行为"
          min-width="150" /><el-table-column prop="memberName" label="会员" /><el-table-column prop="storeName" label="门店" /><el-table-column
          prop="source"
          label="来源" />
        <el-table-column label="操作" width="76" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="selected = row">详情</el-button></template>
        </el-table-column></el-table
      ><pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load"
    /></el-card>
    <FitnessRecordDetails v-if="selected" title="行为详情" :fields="detailFields" :row="selected" @close="selected = undefined" />
  </div>
</template>
<script setup lang="ts" name="FitnessBehavior">
import request from '@/utils/request';
import FitnessRecordDetails from '../components/FitnessRecordDetails.vue';
const selected = ref<Record<string, any>>();
const detailFields = [
  { label: '时间（北京时间）', prop: 'createdAt', type: 'date' },
  { label: '行为', prop: 'action' }, { label: '操作人', prop: 'actorName' }, { label: '身份', prop: 'actorType' },
  { label: '会员', prop: 'memberName' }, { label: '门店', prop: 'storeName' }, { label: '来源', prop: 'source' },
  { label: '预约记录 ID', prop: 'reservationId' }, { label: '说明 / 原因', prop: 'remark' },
];
const actionLabels: Record<string, string> = {
  admin_bind_member_card: '管理员手动绑卡',
  coach_create_schedule: '教练排课',
  admin_finish_schedule: '管理员结束场次',
  admin_schedule_status: '管理员变更排课状态',
  member_reservation: '会员预约',
  admin_reservation: '管理员预约',
  member_cancel_reservation: '会员取消预约',
  admin_cancel_reservation: '管理员取消预约',
  member_checkin: '会员签到',
  coach_proxy_checkin: '教练代签到',
  coach_confirm_checkin: '教练核销',
  admin_confirm_checkin: '管理员核销',
  coach_checkin_exception: '教练标记异常',
  admin_checkin_exception: '管理员标记异常',
  admin_restore_checkin: '管理员恢复签到',
  coach_restore_checkin: '教练恢复签到',
  admin_resolve_reservation: '管理员处理缺席或请假',
  coach_resolve_reservation: '教练处理缺席或请假'
};
const rows = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const query = reactive({ pageNum: 1, pageSize: 20 });
async function load() {
  loading.value = true;
  try {
    const res: any = await request({ url: '/fitness/behavior/list', params: query });
    rows.value = (res.rows || []).map((row: any) => ({
      ...row,
      action: actionLabels[row.action] || row.action,
      actorType: ({ member: '会员', coach: '教练', admin: '管理员' } as Record<string, string>)[row.actorType] || row.actorType,
      source: ({ web: '网页', mini: '微信小程序' } as Record<string, string>)[row.source] || row.source
    }));
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
