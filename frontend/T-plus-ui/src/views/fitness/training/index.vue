<template>
  <div class="p-4">
    <el-alert
      title="会员可在会员网页或小程序自建计划，教练在学员列表中定制。此页供管理员按会员查看计划和实际训练记录。"
      type="info"
      :closable="false"
      class="mb-4"
    /><el-card
      ><template #header
        ><el-select v-model="memberId" filterable remote placeholder="搜索会员" :remote-method="searchMembers" @change="load"
          ><el-option v-for="m in members" :key="String(m.value)" :label="m.label" :value="m.value" /></el-select
        ><el-button class="ml-2" @click="load">查看计划</el-button></template
      ><el-table :data="plans" v-loading="loading"
        ><el-table-column prop="name" label="计划" /><el-table-column prop="cycleDays" label="循环天数" /><el-table-column
          prop="status"
          label="状态"
        /><el-table-column prop="visibility" label="公开范围" /><el-table-column label="详情"
          ><template #default="scope"><el-button link type="primary" @click="detail(scope.row)">查看日程与统计</el-button></template></el-table-column
        ></el-table
      ></el-card
    ><el-dialog v-model="visible" title="训练日程与实际记录" width="85%"
      ><el-table :data="workouts"
        ><el-table-column prop="plannedDate" label="计划日期" /><el-table-column prop="originalDate" label="原定日期" /><el-table-column
          prop="title"
          label="训练内容" /><el-table-column prop="status" label="状态" /><el-table-column prop="performedDate" label="实际训练日期"
      /></el-table>
      <h3>每次训练的重量（kg）与组数</h3>
      <el-table :data="points"
        ><el-table-column prop="date" label="日期" /><el-table-column prop="exerciseName" label="动作" /><el-table-column
          prop="maxWorkingWeightKg"
          label="正式组最大重量" /><el-table-column prop="workingVolumeKg" label="正式组训练量 kg·次" /><el-table-column
          prop="workingSets"
          label="正式组" /><el-table-column prop="warmupSets" label="热身组"
      /></el-table>
      <h3>训练行为记录</h3>
      <el-table :data="audit"
        ><el-table-column prop="createTime" label="时间" /><el-table-column prop="actorId" label="操作者" /><el-table-column
          prop="actorType"
          label="身份" /><el-table-column prop="action" label="动作" /><el-table-column prop="detail" label="变更记录" min-width="320" /></el-table
    ></el-dialog>
  </div>
</template>
<script setup lang="ts" name="FitnessTraining">
import request from '@/utils/request';
import { loadMemberOptions } from '../components/remoteOptions';
const memberId = ref<string | number>();
const members = ref<any[]>([]);
const plans = ref<any[]>([]);
const workouts = ref<any[]>([]);
const points = ref<any[]>([]);
const audit = ref<any[]>([]);
const visible = ref(false);
const loading = ref(false);
async function searchMembers(keyword: string) {
  members.value = await loadMemberOptions({ keyword, value: memberId.value, model: {} });
}
async function load() {
  if (!memberId.value) return;
  loading.value = true;
  try {
    const res: any = await request({ url: '/fitness/training/plans', params: { memberId: memberId.value } });
    plans.value = res.data || [];
  } finally {
    loading.value = false;
  }
}
async function detail(p: any) {
  const [a, b, c]: any[] = await Promise.all([
    request({ url: `/fitness/training/plans/${p.planId}/workouts` }),
    request({ url: `/fitness/training/plans/${p.planId}/stats` }),
    request({ url: '/fitness/training/audit', params: { planId: p.planId } })
  ]);
  workouts.value = a.data || [];
  points.value = b.data?.points || [];
  audit.value = c.data || [];
  visible.value = true;
}
onMounted(() => searchMembers(''));
</script>
