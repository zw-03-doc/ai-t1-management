<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
</template>

<script setup name="FitnessCoach" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addCoach, delCoach, getCoach, listCoach, updateCoach } from '@/api/fitness/coach';
import { loadStoreOptions } from '../components/remoteOptions';

const hireStatusOptions = [
  { label: '在职', value: '0' },
  { label: '停用', value: '1' }
];
const genderOptions = [
  { label: '未知', value: '0' },
  { label: '男', value: '1' },
  { label: '女', value: '2' }
];

const pageConfig = reactive({
  title: '教练',
  permPrefix: 'fitness:coach',
  idKey: 'coachId',
  exportUrl: 'fitness/coach/export',
  queryFields: [
    { label: '教练编号', prop: 'coachNo' },
    { label: '教练姓名', prop: 'coachName' },
    { label: '手机号', prop: 'mobile' },
    { label: '咨询微信号', prop: 'contactWechat' },
    { label: '任职状态', prop: 'hireStatus', type: 'select', options: hireStatusOptions }
  ],
  formFields: [
    { label: '教练编号', prop: 'coachNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '激活码', prop: 'activationCode', placeholder: '可手动填写，不填则自动生成' },
    { label: '所属门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },

    { label: '教练姓名', prop: 'coachName' },
    { label: '手机号', prop: 'mobile' },
    { label: '咨询微信号', prop: 'contactWechat' },
    { label: '性别', prop: 'gender', type: 'select', options: genderOptions },
    { label: '教练等级', prop: 'levelCode' },
    { label: '擅长标签', prop: 'skillTags' },
    { label: '任职状态', prop: 'hireStatus', type: 'select', options: hireStatusOptions },
    { label: '教练简介', prop: 'intro', type: 'textarea', span: 24 },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '教练编号', prop: 'coachNo' },
    { label: '激活码', prop: 'activationCode' },
    { label: '所属门店', prop: 'storeName' },
    { label: '教练姓名', prop: 'coachName' },
    { label: '手机号', prop: 'mobile' },
    { label: '咨询微信号', prop: 'contactWechat' },

    { label: '擅长标签', prop: 'skillTags' },

    { label: '教练等级', prop: 'levelCode' },
    { label: '任职状态', prop: 'hireStatus', type: 'tag', options: hireStatusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    coachNo: '',
    coachName: '',
    mobile: '',
    contactWechat: '',
    hireStatus: ''
  },
  initialForm: {
    coachId: undefined,
    coachNo: '',
    storeId: undefined,
    coachName: '',
    mobile: '',
    contactWechat: '',
    gender: '0',
    levelCode: '',
    skillTags: '',
    hireStatus: '0',
    intro: '',
    remark: ''
  },

  rules: {
    coachName: [{ required: true, message: '教练姓名不能为空', trigger: 'blur' }],
    mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }]
  },

  listFn: listCoach,
  getFn: getCoach,
  addFn: addCoach,
  updateFn: updateCoach,
  delFn: delCoach
});
</script>
