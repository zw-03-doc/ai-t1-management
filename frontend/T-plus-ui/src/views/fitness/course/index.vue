<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
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
    { label: '人数上限', prop: 'capacity', type: 'number' },
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
