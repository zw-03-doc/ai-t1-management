<template>
  <div>
    <el-alert
      title="课程上架与排课分别设置：上架天数控制展示；提前预约天数控制可约范围。所有时间均为北京时间。价格仅用于线下咨询。"
      type="info"
      :closable="false"
      class="mb-4"
    /><FitnessCrudPage v-bind="pageConfig as any" />
  </div>
</template>

<script setup name="FitnessCourse" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addCourse, delCourse, getCourse, listCourse, updateCourse } from '@/api/fitness/course';
import { loadCoachOptions, loadStoreOptions } from '../components/remoteOptions';

const statusOptions = [
  { label: '上架', value: '0' },
  { label: '下架', value: '1' }
];
const courseTypeOptions = [
  { label: '团课', value: 'group' },
  { label: '私教', value: 'private' },
  { label: '小班课', value: 'small' },
  { label: '体验课', value: 'experience' }
];

const pageConfig = reactive({
  title: '课程',
  permPrefix: 'fitness:course',
  idKey: 'courseId',
  exportUrl: 'fitness/course/export',
  queryFields: [
    { label: '课程编号', prop: 'courseNo' },
    { label: '课程名称', prop: 'courseName' },
    { label: '课程类型', prop: 'courseType', type: 'select', options: courseTypeOptions },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions }
  ],
  formFields: [
    { label: '课程编号', prop: 'courseNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '课程名称', prop: 'courseName' },
    { label: '课程类型', prop: 'courseType', type: 'select', options: courseTypeOptions },
    { label: '归属门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '默认教练', prop: 'defaultCoachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '时长(分钟)', prop: 'durationMinutes', type: 'number' },
    { label: '人数上限', prop: 'capacity', type: 'number', min: 1 },
    { label: '展示价格(元)', prop: 'displayPrice', type: 'number', precision: 2 },
    { label: '上架开始', prop: 'publishedAt', type: 'datetime' },
    { label: '上架天数', prop: 'listingDays', type: 'number', min: 1, max: 365 },
    { label: '提前预约天数', prop: 'bookingAdvanceDays', type: 'number', max: 365 },
    { label: '开课前停约(分)', prop: 'bookingCloseMinutes', type: 'number' },
    { label: '开课前停取消(分)', prop: 'cancelCloseMinutes', type: 'number' },
    { label: '提前签到(分)', prop: 'checkinBeforeMinutes', type: 'number' },
    { label: '课后签到(分)', prop: 'checkinAfterMinutes', type: 'number' },
    { label: '支持卡项类型', prop: 'supportCardType' },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions },
    { label: '课程介绍', prop: 'description', type: 'textarea', span: 24 },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '课程编号', prop: 'courseNo' },
    { label: '课程名称', prop: 'courseName' },
    { label: '课程类型', prop: 'courseType', type: 'tag', options: courseTypeOptions },
    { label: '归属门店', prop: 'storeName' },
    { label: '默认教练', prop: 'coachName' },
    { label: '时长(分钟)', prop: 'durationMinutes' },
    { label: '人数上限', prop: 'capacity' },
    { label: '展示价格(元)', prop: 'displayPrice' },
    { label: '上架截止', prop: 'listingUntil', type: 'date' },
    { label: '提前可约(天)', prop: 'bookingAdvanceDays' },
    { label: '状态', prop: 'status', type: 'tag', options: statusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    courseNo: '',
    courseName: '',
    courseType: '',
    status: ''
  },
  initialForm: {
    courseId: undefined,
    courseNo: '',
    courseName: '',
    courseType: 'group',
    storeId: undefined,
    defaultCoachId: undefined,
    durationMinutes: 60,
    capacity: 20,
    displayPrice: 0,
    listingDays: 30,
    publishedAt: undefined,
    bookingAdvanceDays: 7,
    bookingCloseMinutes: 30,
    cancelCloseMinutes: 120,
    checkinBeforeMinutes: 30,
    checkinAfterMinutes: 30,
    supportCardType: '',
    status: '0',
    description: '',
    remark: ''
  },

  rules: {
    courseName: [{ required: true, message: '课程名称不能为空', trigger: 'blur' }]
  },
  listFn: listCourse,
  getFn: getCourse,
  addFn: addCourse,
  updateFn: updateCourse,
  delFn: delCourse
});
</script>
