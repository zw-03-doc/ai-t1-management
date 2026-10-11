<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
</template>

<script setup name="FitnessCardPlan" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addCardPlan, delCardPlan, getCardPlan, listCardPlan, updateCardPlan } from '@/api/fitness/cardPlan';
import { loadCoachOptions, loadMemberOptions, loadStoreOptions } from '../components/remoteOptions';

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
    { label: '总次数', prop: 'totalTimes', type: 'number' },
    { label: '赠送次数', prop: 'giftTimes', type: 'number' },
    { label: '储值金额', prop: 'storedAmount', type: 'number' },
    { label: '支持课程类型', prop: 'supportCourseType' },
    { label: '绑定教练', prop: 'isPrivateBindCoach', type: 'select', options: bindCoachOptions },
    { label: '私教/小班教练', prop: 'customCoachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '指定会员(1-5)', prop: 'customMemberIds', type: 'remoteSelectMultiple', remoteMethod: loadMemberOptions, span: 24 },
    { label: '自动关联课程ID', prop: 'bindCourseId', type: 'number' },
    { label: '排序号', prop: 'sortNo', type: 'number' },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '方案编号', prop: 'planNo' },
    { label: '适用门店', prop: 'storeName' },
    { label: '方案名称', prop: 'planName' },
    { label: '卡项类型', prop: 'planType', type: 'tag', options: planTypeOptions },
    { label: '销售价', prop: 'salePrice' },
    { label: '总次数', prop: 'totalTimes' },
    { label: '绑定教练', prop: 'customCoachName' },
    { label: '指定会员', prop: 'customMemberNames' },
    { label: '关联课程', prop: 'bindCourseName' },
    { label: '状态', prop: 'status', type: 'tag', options: statusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
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
    bindCourseId: undefined,
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
  updateFn: updateCardPlan,
  delFn: delCardPlan
});
</script>
