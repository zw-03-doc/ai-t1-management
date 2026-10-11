<template>
  <div class="p-2">
    <FitnessCrudPage v-bind="pageConfig as any">
      <template #row-actions="{ row, refresh }">
        <el-tooltip content="状态管理" placement="top">
          <el-button v-hasPermi="['fitness:memberCard:manage']" link type="primary" icon="Switch" @click="openStatusDialog(row, refresh)" />
        </el-tooltip>
        <el-tooltip content="权益调整" placement="top">
          <el-button v-hasPermi="['fitness:memberCard:manage']" link type="warning" icon="EditPen" @click="openAdjustDialog(row, refresh)" />
        </el-tooltip>
      </template>
    </FitnessCrudPage>

    <el-dialog v-model="statusDialog.visible" :title="statusDialog.title" width="420px" append-to-body>
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="90px">
        <el-form-item label="卡状态" prop="cardStatus">
          <el-select v-model="statusForm.cardStatus" placeholder="请选择卡状态">
            <el-option v-for="option in cardStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitStatus">确 定</el-button>
          <el-button @click="statusDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustDialog.visible" :title="adjustDialog.title" width="620px" append-to-body>
      <el-form ref="adjustFormRef" :model="adjustForm" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="总次数调整">
              <el-input-number v-model="adjustForm.totalTimesAdjust" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="剩余次数调整">
              <el-input-number v-model="adjustForm.remainTimesAdjust" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="余额调整">
              <el-input-number v-model="adjustForm.balanceAmountAdjust" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期时间">
              <el-date-picker v-model="adjustForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="adjustForm.remark" type="textarea" :rows="3" placeholder="请输入调整说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAdjust">确 定</el-button>
          <el-button @click="adjustDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FitnessMemberCard" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { adjustMemberCard, changeMemberCardStatus, getMemberCard, listMemberCard } from '@/api/fitness/memberCard';

const cardStatusOptions = [
  { label: '未生效', value: 'unused' },
  { label: '已激活', value: 'active' },
  { label: '已冻结', value: 'frozen' },
  { label: '已过期', value: 'expired' },
  { label: '已耗尽', value: 'exhausted' }
];
const planTypeOptions = [
  { label: '时长卡', value: 'time_card' },
  { label: '次卡', value: 'count_card' },
  { label: '储值卡', value: 'stored_card' },
  { label: '私教课包', value: 'private_package' },
  { label: '体验卡', value: 'experience' }
];

const noop = async () => ({ code: 200, data: {} });
const refreshAction = ref<null | (() => Promise<void>)>(null);
const statusFormRef = ref<ElFormInstance>();
const adjustFormRef = ref<ElFormInstance>();

const pageConfig = reactive({
  title: '会员持卡',
  permPrefix: 'fitness:memberCard',
  idKey: 'cardId',
  exportUrl: 'fitness/memberCard/export',
  allowCreate: false,
  allowEdit: false,
  allowDelete: false,
  queryFields: [
    { label: '关键字', prop: 'keyword' },
    { label: '卡项类型', prop: 'planType', type: 'select', options: planTypeOptions },
    { label: '卡状态', prop: 'cardStatus', type: 'select', options: cardStatusOptions }
  ],
  formFields: [],
  tableColumns: [
    { label: '门店', prop: 'storeName' },
    { label: '会员', prop: 'memberName' },
    { label: '手机号', prop: 'memberMobile' },
    { label: '方案名称', prop: 'planName' },
    { label: '卡号', prop: 'cardNo' },
    { label: '卡项类型', prop: 'planType', type: 'tag', options: planTypeOptions },
    { label: '剩余次数', prop: 'remainTimes' },
    { label: '剩余余额', prop: 'balanceAmount' },
    { label: '到期时间', prop: 'expireTime', type: 'date' },
    { label: '卡状态', prop: 'cardStatus', type: 'tag', options: cardStatusOptions }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    planType: '',
    cardStatus: ''
  },
  initialForm: {},
  listFn: listMemberCard,
  getFn: getMemberCard,
  addFn: noop,
  updateFn: noop,
  delFn: noop
});

const statusDialog = reactive<DialogOption>({
  visible: false,
  title: '会员卡状态管理'
});
const adjustDialog = reactive<DialogOption>({
  visible: false,
  title: '会员卡权益调整'
});

const statusForm = ref({
  cardId: undefined as number | string | undefined,
  cardStatus: 'active'
});
const adjustForm = ref({
  cardId: undefined as number | string | undefined,
  totalTimesAdjust: 0,
  remainTimesAdjust: 0,
  balanceAmountAdjust: 0,
  expireTime: '',
  remark: ''
});

const statusRules = {
  cardStatus: [{ required: true, message: '卡状态不能为空', trigger: 'change' }]
};

const openStatusDialog = (row: any, refresh: () => Promise<void>) => {
  refreshAction.value = refresh;
  statusForm.value = {
    cardId: row.cardId,
    cardStatus: row.cardStatus || 'active'
  };
  statusDialog.visible = true;
};

const openAdjustDialog = (row: any, refresh: () => Promise<void>) => {
  refreshAction.value = refresh;
  adjustForm.value = {
    cardId: row.cardId,
    totalTimesAdjust: 0,
    remainTimesAdjust: 0,
    balanceAmountAdjust: 0,
    expireTime: row.expireTime || '',
    remark: ''
  };
  adjustDialog.visible = true;
};

const submitStatus = () => {
  statusFormRef.value?.validate(async (valid: boolean) => {
    if (!valid || !statusForm.value.cardId) {
      return;
    }
    await changeMemberCardStatus(statusForm.value.cardId, statusForm.value.cardStatus);
    statusDialog.visible = false;
    ElMessage.success('卡状态更新成功');
    await refreshAction.value?.();
  });
};

const submitAdjust = async () => {
  if (!adjustForm.value.cardId) {
    return;
  }
  await adjustMemberCard(adjustForm.value.cardId, adjustForm.value);
  adjustDialog.visible = false;
  ElMessage.success('会员卡权益调整成功');
  await refreshAction.value?.();
};
</script>
