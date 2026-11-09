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
          label="来源" /><el-table-column prop="reservationId" label="预约编号" min-width="180" /><el-table-column
          prop="remark"
          label="说明 / 原因"
          min-width="200" /></el-table
      ><pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load"
    /></el-card>
  </div>
</template>
<script setup lang="ts" name="FitnessBehavior">
import request from '@/utils/request';
const actionLabels: Record<string, string> = {
  coach_create_schedule: '教练排课',
  admin_finish_schedule: '管理员结束场次',
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
