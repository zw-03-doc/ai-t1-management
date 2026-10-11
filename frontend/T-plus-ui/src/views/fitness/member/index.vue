<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row }">
      <el-tooltip content="详情" placement="top">
        <el-button v-hasPermi="['fitness:member:query']" link type="primary" icon="View" @click="handleDetail(row)" />
      </el-tooltip>
    </template>
  </FitnessCrudPage>
</template>

<script setup name="FitnessMember" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addMember, delMember, getMember, listMember, updateMember } from '@/api/fitness/member';
import { loadStoreOptions } from '../components/remoteOptions';
import { useRouter } from 'vue-router';

const router = useRouter();

const memberStatusOptions = [
  { label: '正常', value: 'normal' },
  { label: '冻结', value: 'frozen' },
  { label: '流失', value: 'lost' }
];
const genderOptions = [
  { label: '未知', value: '0' },
  { label: '男', value: '1' },
  { label: '女', value: '2' }
];

const pageConfig = reactive({
  title: '会员',
  permPrefix: 'fitness:member',
  idKey: 'memberId',
  exportUrl: 'fitness/member/export',
  queryFields: [
    { label: '会员编号', prop: 'memberNo' },
    { label: '姓名', prop: 'realName' },
    { label: '手机号', prop: 'mobile' },
    { label: '会员状态', prop: 'memberStatus', type: 'select', options: memberStatusOptions }
  ],
  formFields: [
    { label: '会员编号', prop: 'memberNo' },
    { label: '真实姓名', prop: 'realName' },
    { label: '昵称', prop: 'nickname' },
    { label: '手机号', prop: 'mobile' },
    { label: '归属门店', prop: 'homeStoreId', type: 'remoteSelect', remoteMethod: loadStoreOptions },

    { label: '性别', prop: 'gender', type: 'select', options: genderOptions },
    { label: '生日', prop: 'birthday', type: 'date' },
    { label: '注册来源', prop: 'registerSource' },
    { label: '会员状态', prop: 'memberStatus', type: 'select', options: memberStatusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '会员编号', prop: 'memberNo' },
    { label: '姓名', prop: 'realName' },
    { label: '昵称', prop: 'nickname' },
    { label: '手机号', prop: 'mobile' },
    { label: '归属门店', prop: 'homeStoreName' },
    { label: '会员状态', prop: 'memberStatus', type: 'tag', options: memberStatusOptions },
    { label: '最近到店时间', prop: 'lastVisitTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    memberNo: '',
    realName: '',
    mobile: '',
    memberStatus: ''
  },
  initialForm: {
    memberId: undefined,
    memberNo: '',
    homeStoreId: undefined,
    realName: '',
    nickname: '',
    mobile: '',
    gender: '0',
    birthday: '',
    registerSource: 'admin',
    memberStatus: 'normal',
    remark: ''
  },
  rules: {
    memberNo: [{ required: true, message: '会员编号不能为空', trigger: 'blur' }]
  },
  listFn: listMember,
  getFn: getMember,
  addFn: addMember,
  updateFn: updateMember,
  delFn: delMember
});

function handleDetail(row: any) {
  router.push({ path: '/fitness/member/detail', query: { memberId: row.memberId } });
}
</script>
