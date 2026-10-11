<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
</template>

<script setup name="FitnessActivity" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addActivity, delActivity, getActivity, listActivity, updateActivity } from '@/api/fitness/activity';
import { loadCardPlanOptions, loadStoreOptions } from '../components/remoteOptions';

const statusOptions = [
  { label: '上架', value: '0' },
  { label: '下架', value: '1' }
];

const featuredOptions = [
  { label: '否', value: '0' },
  { label: '是', value: '1' }
];

const activityTypeOptions = [
  { label: '优惠卡项活动', value: 'card_discount' },
  { label: '定制推送活动', value: 'custom_push' }
];

const actionTypeOptions = [
  { label: '查看详情', value: 'detail' },
  { label: '联系门店', value: 'contact' }
];

const pageConfig = reactive({
  title: '活动管理',
  permPrefix: 'fitness:activity',
  idKey: 'activityId',
  exportUrl: 'fitness/activity/export',
  queryFields: [
    { label: '活动编号', prop: 'activityNo' },
    { label: '活动标题', prop: 'title' },
    { label: '活动类型', prop: 'activityType', type: 'select', options: activityTypeOptions },
    { label: '动作类型', prop: 'actionType', type: 'select', options: actionTypeOptions },
    { label: '适用门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '本周主推', prop: 'weeklyFeatured', type: 'select', options: featuredOptions },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions }
  ],
  formFields: [
    { label: '活动编号', prop: 'activityNo', placeholder: '可手填，不填自动生成' },
    { label: '适用门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions, placeholder: '不选则全门店生效' },
    { label: '活动标题', prop: 'title' },
    { label: '活动副标题', prop: 'subTitle' },
    { label: '活动类型', prop: 'activityType', type: 'select', options: activityTypeOptions },
    { label: '动作类型', prop: 'actionType', type: 'select', options: actionTypeOptions },
    { label: '绑定卡项', prop: 'planIds', type: 'remoteSelectMultiple', remoteMethod: loadCardPlanOptions, placeholder: '优惠卡项活动请选择绑定卡项' },
    { label: '折扣标签', prop: 'discountLabel', placeholder: '示例：7折、立减100、买一赠一' },
    { label: '规则备注', prop: 'ruleNote', placeholder: '示例：体验卡仅限新客、不可叠加其他优惠' },
    { label: '价格亮点', prop: 'priceHighlight', placeholder: '示例：次卡低至 ¥99' },
    { label: '适用人群', prop: 'targetAudience', placeholder: '示例：新客 / 上班族 / 会员续费' },
    { label: '活动封面图', prop: 'coverUrl', type: 'imageUpload', placeholder: '上传后自动回填地址' },
    { label: '活动详情图', prop: 'detailImageUrl', type: 'imageUpload', placeholder: '上传后自动回填地址' },
    { label: '开始时间', prop: 'startTime', type: 'datetime' },
    { label: '结束时间', prop: 'endTime', type: 'datetime' },
    { label: '倒计时截止', prop: 'countdownEndTime', type: 'datetime' },
    { label: '本周主推', prop: 'weeklyFeatured', type: 'select', options: featuredOptions },
    { label: '排序号', prop: 'sortNo', type: 'number' },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions },
    { label: '详情文案', prop: 'detailContent', type: 'textarea', span: 24 },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '活动编号', prop: 'activityNo' },
    { label: '活动标题', prop: 'title' },
    { label: '门店', prop: 'storeName' },
    { label: '活动类型', prop: 'activityType', type: 'tag', options: activityTypeOptions },
    { label: '动作类型', prop: 'actionType', type: 'tag', options: actionTypeOptions },
    { label: '价格亮点', prop: 'priceHighlight' },
    { label: '本周主推', prop: 'weeklyFeatured', type: 'tag', options: featuredOptions },
    { label: '状态', prop: 'status', type: 'tag', options: statusOptions },
    { label: '开始时间', prop: 'startTime', type: 'date' },
    { label: '结束时间', prop: 'endTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    activityNo: '',
    title: '',
    activityType: '',
    actionType: '',
    storeId: undefined,
    weeklyFeatured: '',
    status: ''
  },
  initialForm: {
    activityId: undefined,
    activityNo: '',
    storeId: undefined,
    title: '',
    subTitle: '',
    activityType: 'card_discount',
    actionType: 'detail',
    planIds: [],
    discountLabel: '',
    ruleNote: '',
    priceHighlight: '',
    targetAudience: '',
    coverUrl: '',
    detailImageUrl: '',
    detailContent: '',
    startTime: '',
    endTime: '',
    countdownEndTime: '',
    weeklyFeatured: '0',
    sortNo: 0,
    status: '0',
    remark: ''
  },
  rules: {
    title: [{ required: true, message: '活动标题不能为空', trigger: 'blur' }],
    activityType: [{ required: true, message: '活动类型不能为空', trigger: 'change' }],
    actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }],
    coverUrl: [{ required: true, message: '活动封面图不能为空', trigger: 'change' }],
    startTime: [{ required: true, message: '开始时间不能为空', trigger: 'change' }],
    endTime: [{ required: true, message: '结束时间不能为空', trigger: 'change' }]
  },
  listFn: listActivity,
  getFn: getActivity,
  addFn: addActivity,
  updateFn: updateActivity,
  delFn: delActivity
});
</script>
