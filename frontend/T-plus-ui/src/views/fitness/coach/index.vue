<template>
  <div>
    <FitnessCrudPage v-bind="pageConfig as any">
      <template #row-actions="{ row, refresh }">
        <el-button v-hasPermi="['fitness:coach:edit']" link :type="row.hireStatus === '0' ? 'warning' : 'success'" :disabled="statusBusy" @click="toggleStatus(row, refresh)">
          {{ row.hireStatus === '0' ? '停用' : '恢复' }}
        </el-button>
      </template>
    </FitnessCrudPage>
  </div>
</template>

<script setup name="FitnessCoach" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addCoach, delCoach, getCoach, listCoach, updateCoach, changeCoachStatus } from '@/api/fitness/coach';
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
    { label: '教练姓名', prop: 'coachName', minWidth: 164 },
    { label: '所属门店', prop: 'storeName', minWidth: 126 },
    { label: '手机号', prop: 'mobile', minWidth: 120 },
    { label: '擅长标签', prop: 'skillTags' },
    { label: '任职状态', prop: 'hireStatus', type: 'tag', options: hireStatusOptions }
  ],
  detailColumns: [
    { label: '教练编号', prop: 'coachNo' },
    { label: '激活码', prop: 'activationCode' },
    { label: '咨询微信号', prop: 'contactWechat' },
    { label: '教练等级', prop: 'levelCode' },
    { label: '创建时间', prop: 'createTime', type: 'date' },
    { label: '教练简介', prop: 'intro' },
    { label: '备注', prop: 'remark' }
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
    storeId: [{ required: true, message: '请选择所属门店', trigger: 'change' }],
    coachName: [{ required: true, message: '教练姓名不能为空', trigger: 'blur' }],
    mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }]
  },

  listFn: listCoach,
  getFn: getCoach,
  addFn: addCoach,
  updateFn: updateCoach,
  delFn: delCoach
});
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const statusBusy = ref(false);
async function toggleStatus(row: any, refresh: () => Promise<void>) {
  if (statusBusy.value) return;
  const status = row.hireStatus === '0' ? '1' : '0';
  statusBusy.value = true;
  try {
    await proxy?.$modal.confirm(status === '1' ? `停用 ${row.coachName} 后，所有设备将退出登录，不能继续授课操作。保留历史记录，是否继续？` : `恢复 ${row.coachName} 的在职状态？恢复后需要重新登录。`);
    await changeCoachStatus(row.coachId, status);
    proxy?.$modal.msgSuccess(status === '1' ? '教练已停用，所有登录已失效' : '教练已恢复，请重新登录');
    await refresh();
  } catch (error) { /* Request errors are shown by the shared request handler. */ }
  finally { statusBusy.value = false; }
}
</script>
