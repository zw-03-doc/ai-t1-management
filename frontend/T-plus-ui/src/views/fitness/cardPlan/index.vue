<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #field-grantedTimes="{ model }">
      <el-form-item label="总次数"><output>{{ Number(model.totalTimes || 0) + Number(model.giftTimes || 0) }} 次</output></el-form-item>
    </template>
    <template #field-legacyCourseType="{ model }">
      <el-form-item label="原课型限制">
        <span>{{ courseTypesLabel(model.supportCourseType) }}</span>
        <el-button link @click="model.supportCourseType = ''">解除限制</el-button>
      </el-form-item>
    </template>
    <template #row-actions="{ row }">
      <el-button v-hasPermi="['fitness:memberCard:manage']" link type="primary" :disabled="row.status !== '0'" @click="bindingPlan = row">绑定会员</el-button>
    </template>
  </FitnessCrudPage>
  <BindMemberCardDialog v-if="bindingPlan" :plan="bindingPlan" @close="bindingPlan = undefined" />
</template>

<script setup name="FitnessCardPlan" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import BindMemberCardDialog from '../components/BindMemberCardDialog.vue';
import type { FitnessCardPlanVO } from '@/api/fitness/cardPlan/types';
const bindingPlan = ref<FitnessCardPlanVO>();
import { addCardPlan, delCardPlan, getCardPlan, listCardPlan, updateCardPlan } from '@/api/fitness/cardPlan';
import { loadCoachOptions, loadCourseOptions, loadMemberOptions, loadStoreOptions } from '../components/remoteOptions';
import type { RemoteSelectParams } from '../components/remoteOptions';
import type { FitnessCardPlanForm } from '@/api/fitness/cardPlan/types';

function coursesForPlan(params: RemoteSelectParams) {
  const kind = params.model?.planType;
  return loadCourseOptions({ ...params, model: { ...params.model, courseTypeFilter: kind === 'private_package' ? 'private' : kind === 'small_group_package' ? 'small' : undefined } });
}

function courseTypesLabel(types: string) {
  const names: Record<string, string> = { private: '私教课', small: '小班课', small_group: '小班课', group: '团课', experience: '体验课', all: '全部课程' };
  return types.split(/[,，;\s]+/).map(type => names[type] || type).join('、');
}

const statusOptions = [
  { label: '上架', value: '0' },
  { label: '下架', value: '1' }
];

const planTypeOptions = [
  { label: '时长卡', value: 'time_card' },
  { label: '次卡', value: 'count_card' },
  { label: '储值卡', value: 'stored_card' },
  { label: '私教课包', value: 'private_package' },
  { label: '小班课包', value: 'small_group_package' },
  { label: '体验卡', value: 'experience' }
];

const bindCoachOptions = [
  { label: '否', value: '0' },
  { label: '是', value: '1' }
];

const pageConfig = reactive({
  title: '卡项方案',
  permPrefix: 'fitness:cardPlan',
  idKey: 'planId',
  exportUrl: 'fitness/cardPlan/export',
  queryFields: [
    { label: '方案编号', prop: 'planNo' },
    { label: '方案名称', prop: 'planName' },
    { label: '卡项类型', prop: 'planType', type: 'select', options: planTypeOptions },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions }
  ],
  formFields: [
    { label: '方案编号', prop: 'planNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '适用门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '方案名称', prop: 'planName' },
    { label: '卡项类型', prop: 'planType', type: 'select', options: planTypeOptions },
    { label: '销售价', prop: 'salePrice', type: 'number' },
    { label: '原价', prop: 'originalPrice', type: 'number' },
    { label: '有效天数', prop: 'validDays', type: 'number' },
    { label: '适用课程', prop: 'allowedCourseIds', type: 'remoteSelectMultiple', span: 24, remoteMethod: coursesForPlan, placeholder: '选择课程，留空表示不限具体课程', onSelect: (model: Record<string, any>) => { model.supportCourseType = ''; } },
    { label: '次数', prop: 'totalTimes', type: 'number', min: 0, precision: 0 },
    { label: '赠送次数', prop: 'giftTimes', type: 'number', min: 0, precision: 0 },
    { label: '总次数', prop: 'grantedTimes', type: 'custom' },
    { label: '储值金额', prop: 'storedAmount', type: 'number' },
    { label: '原课型限制', prop: 'legacyCourseType', type: 'custom', visibleWhen: (model: Record<string, any>) => !!model.supportCourseType && !['private_package', 'small_group_package'].includes(model.planType) },
    { label: '绑定教练', prop: 'isPrivateBindCoach', type: 'select', options: bindCoachOptions },
    { label: '私教/小班教练', prop: 'customCoachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '指定会员(1-5)', prop: 'customMemberIds', type: 'remoteSelectMultiple', remoteMethod: loadMemberOptions, selectionLimit: 5, span: 24 },
    { label: '排序号', prop: 'sortNo', type: 'number' },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '方案名称', prop: 'planName', minWidth: 164 },
    { label: '卡项类型', prop: 'planType', type: 'tag', options: planTypeOptions },
    { label: '适用门店', prop: 'storeName', minWidth: 126 },
    { label: '销售价', prop: 'salePrice' },
    { label: '状态', prop: 'status', type: 'tag', options: statusOptions }
  ],
  detailColumns: [
    { label: '方案编号', prop: 'planNo' },
    { label: '次数', prop: 'totalTimes' },
    { label: '赠送次数', prop: 'giftTimes' },
    { label: '总次数', prop: 'grantedTimes' },
    { label: '绑定教练', prop: 'customCoachName' },
    { label: '指定会员', prop: 'customMemberNames' },
    { label: '适用课程', prop: 'allowedCourseNames' },
    { label: '创建时间', prop: 'createTime', type: 'date' },
    { label: '有效天数', prop: 'validDays' },
    { label: '储值金额', prop: 'storedAmount' },
    { label: '备注', prop: 'remark' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    planNo: '',
    planName: '',
    planType: '',
    status: ''
  },
  initialForm: {
    planId: undefined,
    planNo: '',
    storeId: undefined,
    planName: '',
    planType: 'count_card',
    salePrice: 0,
    originalPrice: 0,
    validDays: 0,
    totalTimes: 0,
    giftTimes: 0,
    storedAmount: 0,
    supportCourseType: '',
    isPrivateBindCoach: '0',
    customCoachId: undefined,
    customMemberIds: [],
    allowedCourseIds: [],
    sortNo: 0,
    status: '0',
    remark: ''
  },
  rules: {
    planName: [{ required: true, message: '卡项名称不能为空', trigger: 'blur' }],
    planType: [{ required: true, message: '卡项类型不能为空', trigger: 'change' }]
  },
  listFn: listCardPlan,
  getFn: getCardPlan,
  addFn: addCardPlan,
  updateFn: (form: FitnessCardPlanForm) => updateCardPlan({ ...form, storeId: form.storeId || null }),
  delFn: delCardPlan
});
</script>
