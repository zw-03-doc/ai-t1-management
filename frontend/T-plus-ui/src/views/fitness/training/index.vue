<template>
  <div class="training-archive p-4">
    <el-card class="mt-4">
      <template #header>
        <div class="toolbar">
          <el-select v-model="memberId" clearable filterable remote :remote-method="searchMembers" :loading="searching" placeholder="搜索会员姓名、手机号或编号" aria-label="搜索会员" style="width: min(320px,100%)" @change="load">
            <el-option v-for="member in members" :key="String(member.value)" :label="member.label" :value="member.value" />
          </el-select>
          <el-button :disabled="!memberId" :loading="loading" @click="load">刷新档案</el-button>
          <span v-if="context" class="owner">所属会员：{{ context.displayName }} · {{ context.memberId }}</span>
        </div>
      </template>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-4" />
      <div v-if="context" class="editor-links">
        <el-button type="primary" plain @click="openEditor('coach')">打开教练端定制入口</el-button>
        <el-button plain @click="openEditor('member')">打开会员自己的训练页</el-button>
        <span>两端均需独立登录；教练只可编辑有效绑定学员，会员只能编辑自己的计划。</span>
      </div>
      <el-table :data="plans" v-loading="loading" :empty-text="memberId ? '这位会员还没有训练计划' : '请先选择一位会员'" row-key="planId">
        <el-table-column prop="name" label="计划名称" min-width="200" />
        <el-table-column label="所属会员" min-width="140"><template #default>{{ context?.displayName }}</template></el-table-column>
        <el-table-column prop="cycleDays" label="循环天数" width="100" />
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag>{{ planStatus(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="公开范围" width="100"><template #default="scope">{{ scope.row.visibility === 'public' ? '公开免费' : '私人计划' }}</template></el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column label="操作" fixed="right" width="80"><template #default="scope"><el-button link type="primary" @click="detail(scope.row)">详情</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="detailTitle" width="90%" @closed="closeDetail">
      <div v-loading="detailLoading">
        <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
        <template v-else>
          <el-descriptions v-if="selectedPlan" :column="3" border class="mb-4">
            <el-descriptions-item label="所属会员">{{ detailOwner }}</el-descriptions-item>
            <el-descriptions-item label="计划状态">{{ planStatus(selectedPlan.status) }}</el-descriptions-item>
            <el-descriptions-item label="循环">{{ selectedPlan.cycleDays }} 天</el-descriptions-item>
            <el-descriptions-item label="说明" :span="3">{{ selectedPlan.description || '未填写' }}</el-descriptions-item>
          </el-descriptions>
          <el-tabs v-model="detailTab">
            <el-tab-pane label="日程与逐组记录" name="records">
              <p class="help">展开训练日查看实际重量、次数与强度。暂存进度会保留，完成整次训练后计入进步统计。</p>
              <el-table :data="workouts" row-key="workoutId" empty-text="此计划尚未开启，没有训练日程">
                <el-table-column type="expand"><template #default="scope">
                  <div class="expanded-record">
                    <el-table :data="scope.row.sets || []" empty-text="这一天尚未保存实际训练组" size="small">
                      <el-table-column prop="exerciseName" label="动作" min-width="150" />
                      <el-table-column label="组别" width="100"><template #default="setScope">{{ setScope.row.setType === 'warmup' ? '热身组' : '正式组' }}</template></el-table-column>
                      <el-table-column prop="setNumber" label="记录序号" width="100" />
                      <el-table-column prop="reps" label="实际次数" width="100" />
                      <el-table-column label="实际重量" width="130"><template #default="setScope">{{ setScope.row.weight }} {{ setScope.row.unit === 'lb' ? 'lb（磅）' : 'kg（公斤）' }}</template></el-table-column>
                      <el-table-column label="强度 RPE" width="110"><template #default="setScope">{{ setScope.row.rpe ?? '未填写' }}</template></el-table-column>
                    </el-table>
                  </div>
                </template></el-table-column>
                <el-table-column prop="plannedDate" label="计划日期" width="120" />
                <el-table-column prop="originalDate" label="最初安排日期" width="130" />
                <el-table-column prop="title" label="训练主题" min-width="150" />
                <el-table-column label="状态" width="130"><template #default="scope">{{ workoutStatus(scope.row.status) }}</template></el-table-column>
                <el-table-column prop="performedDate" label="实际训练日期" width="130" />
                <el-table-column label="已记录组数" width="120"><template #default="scope">{{ (scope.row.sets || []).length }} 组</template></el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="计划动作与目标" name="definition">
              <el-collapse><el-collapse-item v-for="day in selectedPlan?.days || []" :key="day.dayNumber" :title="`第 ${day.dayNumber} 天 · ${day.title}${day.restDay ? ' · 休息日' : ''}`" :name="day.dayNumber">
                <div v-for="exercise in day.exercises" :key="exercise.key" class="definition-exercise"><h4>{{ exercise.name }}</h4><p class="instructions">{{ exercise.instructions || '未填写动作说明' }}</p>
                  <el-table :data="exercise.targets" size="small"><el-table-column label="目标组别"><template #default="scope">{{ scope.row.setType === 'warmup' ? '热身组' : '正式组' }}</template></el-table-column><el-table-column prop="reps" label="目标次数" /><el-table-column label="目标重量"><template #default="scope">{{ scope.row.weight == null ? '未指定' : `${scope.row.weight} ${scope.row.unit}` }}</template></el-table-column><el-table-column label="目标强度 RPE"><template #default="scope">{{ scope.row.rpe ?? '未指定' }}</template></el-table-column></el-table>
                </div>
              </el-collapse-item></el-collapse>
            </el-tab-pane>
            <el-tab-pane label="已完成训练统计" name="stats">
              <p class="help">正式组重量统一为 kg；训练量为重量 × 次数，热身组单独计数。</p>
              <el-table :data="points" empty-text="完成训练后会显示重量统计"><el-table-column prop="date" label="日期" /><el-table-column prop="exerciseName" label="动作" /><el-table-column label="正式组最大重量（kg）"><template #default="scope">{{ scope.row.maxWorkingWeightKg ?? '暂无正式组' }}</template></el-table-column><el-table-column prop="workingVolumeKg" label="正式训练量 kg·次" /><el-table-column prop="workingSets" label="正式组数" /><el-table-column prop="warmupSets" label="热身组数" /></el-table>
            </el-tab-pane>
            <el-tab-pane label="操作记录" name="audit">
              <el-table :data="audit" empty-text="暂无操作记录"><el-table-column prop="createTime" label="时间" width="175" /><el-table-column prop="actorId" label="操作者编号" min-width="170" /><el-table-column label="身份" width="100"><template #default="scope">{{ actorLabel(scope.row.actorType) }}</template></el-table-column><el-table-column label="操作" width="120"><template #default="scope">{{ actionLabel(scope.row.action) }}</template></el-table-column><el-table-column label="变更详情" min-width="220"><template #default="scope"><el-popover placement="left" width="560" trigger="click"><template #reference><el-button link type="primary">查看前后记录</el-button></template><pre class="audit-detail">{{ prettyDetail(scope.row.detail) }}</pre></el-popover></template></el-table-column></el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="FitnessTraining">
import request from '@/utils/request';
import { loadMemberOptions } from '../components/remoteOptions';
const route = useRoute();
const memberId = ref<string | number>();
const members = ref<any[]>([]);
const context = ref<any>(null);
const plans = ref<any[]>([]);
const workouts = ref<any[]>([]);
const points = ref<any[]>([]);
const audit = ref<any[]>([]);
const selectedPlan = ref<any>(null);
const visible = ref(false);
const loading = ref(false);
const searching = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const detailOwner = ref('');
const detailTab = ref('records');
let listVersion = 0;
let detailVersion = 0;
let searchVersion = 0;
const detailTitle = computed(() => `${detailOwner.value} · ${selectedPlan.value?.name || '训练档案'}`);
const planStatus = (value: string) => (({  draft: '待开启', active: '进行中', completed: '已完成', archived: '已归档'  } as Record<string, string>)[value] || value);
const workoutStatus = (value: string) => (({  planned: '待训练', in_progress: '已暂存，待完成', completed: '已完成', rest: '休息日', cancelled: '已停止安排'  } as Record<string, string>)[value] || value);
const actorLabel = (value: string) => (({  member: '会员本人', coach: '绑定教练', admin: '管理员'  } as Record<string, string>)[value] || value);
const actionLabel = (value: string) => (({  create: '创建计划', update: '调整计划', copy: '复制私人计划', start: '开启计划', extend: '续排循环', complete: '完成计划', archive: '归档计划', shift: '顺延日程', progress: '暂存训练', log: '完成或修正训练', upload: '上传动作素材'  } as Record<string, string>)[value] || value);
function prettyDetail(value: string) { try { return JSON.stringify(JSON.parse(value), null, 2); } catch { return value; } }
async function searchMembers(keyword: string) {
  const version = ++searchVersion;
  searching.value = true;
  try {
    const result = await loadMemberOptions({ keyword, value: memberId.value, model: {} });
    if (version === searchVersion) members.value = result;
  } finally { if (version === searchVersion) searching.value = false; }
}
function closeDetail() { ++detailVersion; detailLoading.value = false; }
async function load() {
  const version = ++listVersion;
  ++detailVersion;
  const id = memberId.value;
  plans.value = []; context.value = null; workouts.value = []; points.value = []; audit.value = [];
  visible.value = false; selectedPlan.value = null; error.value = '';
  if (!id) { loading.value = false; return; }
  loading.value = true;
  try {
    const [planResult, ownerResult]: any[] = await Promise.all([
      request({ url: '/fitness/training/plans', params: { memberId: id } }),
      request({ url: '/fitness/training/context', params: { memberId: id } })
    ]);
    if (version !== listVersion || String(memberId.value) !== String(id)) return;
    plans.value = planResult.data || []; context.value = ownerResult.data;
  } catch (failure: any) { if (version === listVersion) error.value = failure?.message || '训练档案加载失败，请重试'; }
  finally { if (version === listVersion) loading.value = false; }
}
async function detail(plan: any) {
  const version = ++detailVersion;
  const ownerVersion = listVersion;
  selectedPlan.value = plan; detailOwner.value = `${context.value?.displayName || '会员'} · ${plan.memberId}`;
  workouts.value = []; points.value = []; audit.value = []; detailError.value = ''; detailTab.value = 'records';
  visible.value = true; detailLoading.value = true;
  try {
    const [records, stats, history]: any[] = await Promise.all([
      request({ url: `/fitness/training/plans/${plan.planId}/workouts` }),
      request({ url: `/fitness/training/plans/${plan.planId}/stats` }),
      request({ url: '/fitness/training/audit', params: { planId: plan.planId } })
    ]);
    if (version !== detailVersion || ownerVersion !== listVersion) return;
    workouts.value = records.data || []; points.value = stats.data?.points || []; audit.value = history.data || [];
  } catch (failure: any) { if (version === detailVersion) detailError.value = failure?.message || '记录读取失败，请关闭后重试'; }
  finally { if (version === detailVersion) detailLoading.value = false; }
}
function openEditor(role: 'coach' | 'member') {
  const target = role === 'coach' ? `/pages/training/index?role=coach&memberId=${encodeURIComponent(String(memberId.value))}` : '/pages/training/index';
  const base = import.meta.env.VITE_MEMBER_APP_URL || new URL('/app/', window.location.origin).href;
  window.open(`${base.replace(/\/$/, '')}/#/pages/portal/index?role=${role}&target=${encodeURIComponent(target)}`, '_blank', 'noopener,noreferrer');
}
watch(() => route.query.memberId, async value => {
  const id = Array.isArray(value) ? value[0] : value;
  memberId.value = id && /^\d+$/.test(String(id)) ? String(id) : undefined;
  await Promise.all([searchMembers(''), load()]);
}, { immediate: true });
onBeforeUnmount(() => { ++listVersion; ++detailVersion; ++searchVersion; });
</script>

<style scoped>
.toolbar,.editor-links{display:flex;align-items:center;gap:12px;flex-wrap:wrap}.owner{font-weight:600}.editor-links{margin-bottom:20px}.editor-links span,.help{font-size:13px;color:var(--el-text-color-secondary);line-height:1.7}.expanded-record{padding:16px 32px}.definition-exercise{padding:8px 20px 20px}.instructions{white-space:pre-line;line-height:1.7}.audit-detail{max-height:420px;overflow:auto;white-space:pre-wrap;word-break:break-word;font-size:12px}
</style>
