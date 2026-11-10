<template>
  <el-dialog :model-value="true" title="绑定会员卡" width="min(480px, 94vw)" append-to-body
    :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @close="emit('close')">
    <el-form label-position="top" :disabled="saving">
      <el-form-item label="会员">
        <strong v-if="member">{{ member.realName || member.nickname || member.memberNo }} · {{ member.memberNo }}</strong>
        <el-select v-else-if="isPackage" v-model="memberId" filterable clearable placeholder="选择课包指定会员" :loading="loadingRoster" @change="chooseMember">
          <el-option v-for="m in roster" :key="m.memberId" :value="m.memberId" :label="(m.realName || m.nickname || '会员') + ' · ' + m.memberNo" />
        </el-select>
        <CardBindingSelect v-else v-model="memberId" :load="loadMemberOptions" :model="{ memberStatus: 'normal' }" placeholder="搜索会员姓名、手机号或编号" @update:model-value="chooseMember" />
      </el-form-item>
      <el-form-item label="卡项方案">
        <strong v-if="plan">{{ plan.planName }}</strong>
        <CardBindingSelect v-else v-model="planId" :load="loadCardPlanOptions" :model="{ planStatus: '0' }" placeholder="选择卡项方案" @update:model-value="choosePlan" />
      </el-form-item>
      <template v-if="selectedPlan">
        <el-form-item label="发卡门店">
          <span v-if="selectedPlan.storeId">{{ selectedPlan.storeName || '方案所属门店' }}</span>
          <CardBindingSelect v-else v-model="storeId" :load="loadStoreOptions" :model="{ storeStatus: '0' }" placeholder="选择发卡门店" @update:model-value="coachId = undefined" />
        </el-form-item>
        <el-form-item label="绑定教练">
          <span v-if="isPackage">{{ selectedPlan.customCoachName || '课包指定教练' }}</span>
          <CardBindingSelect v-else :key="String(storeId)" v-model="coachId" :load="loadCoachOptions" :disabled="!storeId"
            :model="{ storeId, hireStatus: '0' }" :placeholder="needsCoach ? '请选择教练' : '选填'" />
        </el-form-item>
        <div class="rights"><span>{{ rights }}</span><span>{{ selectedPlan.validDays ? selectedPlan.validDays + ' 天有效' : '不设到期日' }}</span></div>
      </template>
      <el-form-item label="备注"><el-input v-model="remark" maxlength="300" placeholder="选填" /></el-form-item>
    </el-form>
    <el-alert v-if="issue" :title="issue" type="warning" :closable="false" />
    <template #footer><el-button :disabled="saving" @click="emit('close')">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!canBind" @click="submit">确认绑卡</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ElMessage } from 'element-plus';
import CardBindingSelect from './CardBindingSelect.vue';
import { loadCardPlanOptions, loadCoachOptions, loadMemberOptions, loadStoreOptions } from './remoteOptions';
import { getCardPlan } from '@/api/fitness/cardPlan';
import { getMember } from '@/api/fitness/member';
import { bindMemberCard } from '@/api/fitness/memberCard';
import type { FitnessMemberVO } from '@/api/fitness/member/types';
import type { FitnessCardPlanVO } from '@/api/fitness/cardPlan/types';

type Id = string | number;
const props = defineProps<{ member?: FitnessMemberVO; plan?: FitnessCardPlanVO }>();
const emit = defineEmits<{ close: []; bound: [] }>();
const selectedMember = ref(props.member);
const selectedPlan = ref(props.plan);
const memberId = ref<Id | undefined>(props.member?.memberId);
const planId = ref<Id | undefined>(props.plan?.planId);
const storeId = ref<Id>();
const coachId = ref<Id>();
const remark = ref('');
const saving = ref(false);
const roster = ref<FitnessMemberVO[]>([]);
const loadingRoster = ref(false);
let requestId = crypto.randomUUID();
const isPackage = computed(() => ['private_package', 'small_group_package'].includes(selectedPlan.value?.planType || ''));
const needsCoach = computed(() => isPackage.value || selectedPlan.value?.isPrivateBindCoach === '1');
const rights = computed(() => {
  const p = selectedPlan.value!;
  if (p.planType === 'stored_card') return '储值 ¥' + Number(p.storedAmount ?? 0).toFixed(2);
  if (p.planType === 'time_card') return '时长卡';
  return '共 ' + ((p.totalTimes || 0) + (p.giftTimes || 0)) + ' 次' + (p.giftTimes ? '（含赠送 ' + p.giftTimes + ' 次）' : '');
});
const issue = computed(() => {
  if (selectedMember.value && selectedMember.value.memberStatus !== 'normal') return '仅可为正常状态的会员绑卡';
  if (selectedPlan.value && selectedPlan.value.status !== '0') return '该方案已下架';
  if (isPackage.value && selectedMember.value && !selectedPlan.value?.customMemberIds?.map(String).includes(String(memberId.value))) return '该会员不在课包的指定名单中';
  return '';
});
const canBind = computed(() => !!selectedMember.value && !!selectedPlan.value && !!storeId.value && (!needsCoach.value || !!coachId.value) && !issue.value);
async function chooseMember(id?: Id) {
  selectedMember.value = undefined;
  if (!id) return;
  const row = roster.value.find(m => m.memberId === id) || (await getMember(id)).data;
  if (id === memberId.value) selectedMember.value = row;
}
async function choosePlan(id?: Id) {
  selectedPlan.value = undefined;
  if (!id) return;
  const { data } = await getCardPlan(id);
  if (id === planId.value) selectedPlan.value = data;
}
watch([selectedPlan, selectedMember], ([p, m]) => {
  storeId.value = p?.storeId || m?.homeStoreId;
  coachId.value = isPackage.value ? p?.customCoachId : undefined;
}, { immediate: true });
onMounted(async () => {
  if (!props.plan || !isPackage.value) return;
  loadingRoster.value = true;
  try {
    const results = await Promise.allSettled((props.plan.customMemberIds || []).map(id => getMember(id)));
    roster.value = results.flatMap(r => r.status === 'fulfilled' && r.value.data?.memberStatus === 'normal' ? [r.value.data] : []);
  } finally { loadingRoster.value = false; }
});
watch([memberId, planId, storeId, coachId, remark], () => { requestId = crypto.randomUUID(); });
async function submit() {
  if (!canBind.value || saving.value) return;
  saving.value = true;
  try {
    const { data } = await bindMemberCard({ memberId: memberId.value!, planId: planId.value!, storeId: storeId.value!, bindCoachId: coachId.value, requestId, remark: remark.value.trim() });
    ElMessage.success(data.created ? '会员卡已绑定' : '会员已持有此卡');
    emit('bound');
    emit('close');
  } finally { saving.value = false; }
}
</script>
<style scoped>
.rights { display: flex; flex-wrap: wrap; gap: 8px 20px; margin-bottom: 18px; padding: 12px 14px; border-radius: 8px; background: var(--el-fill-color-light); }
</style>
