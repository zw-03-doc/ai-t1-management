<template>
  <div class="p-4">
    <el-alert
      title="预约、签到、代签到和核销记录实际操作者、对象、时间与来源。课程、教练和排课配置变更可在系统操作日志查看。"
      type="info"
      :closable="false"
      class="mb-4"
    /><el-card
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
const rows = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const query = reactive({ pageNum: 1, pageSize: 20 });
async function load() {
  loading.value = true;
  try {
    const res: any = await request({ url: '/fitness/behavior/list', params: query });
    rows.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
