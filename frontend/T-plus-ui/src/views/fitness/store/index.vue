<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row }">
      <el-button v-hasPermi="['fitness:store:query']" link type="primary" @click="openDetail(row)">门店详情</el-button>
    </template>
  </FitnessCrudPage>
  <el-drawer v-model="detailVisible" :title="selectedStore?.storeName || '门店详情'" size="min(900px, 95vw)">
    <template v-if="selectedStore">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="门店编码">{{ selectedStore.storeCode }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ selectedStore.businessHours || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ selectedStore.contactName || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ selectedStore.contactPhone || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ selectedStore.address || '未设置' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert class="mt-[12px]" type="info" :closable="false" title="从这里进入管理，会自动带入当前门店。会员按归属门店统计，课程模板与实际排课分别管理。" />
      <el-space wrap class="my-[16px]">
        <el-button v-hasPermi="['fitness:coach:list']" @click="manage('coach')">教练管理</el-button>
        <el-button v-hasPermi="['fitness:member:list']" @click="manage('member')">会员管理</el-button>
        <el-button v-hasPermi="['fitness:course:list']" @click="manage('course')">课程模板</el-button>
        <el-button v-hasPermi="['fitness:schedule:list']" type="primary" @click="manage('schedule')">查看与安排课表</el-button>
      </el-space>
      <el-tabs v-model="detailTab" @tab-change="changeDetailTab">
        <el-tab-pane v-for="tab in availableTabs" :key="tab.key" :name="tab.key" :label="tab.label" />
      </el-tabs>
      <el-table v-loading="detailLoading" :data="detailRows" border empty-text="该门店暂无相关资料">
        <el-table-column v-for="column in detailColumns" :key="column.prop" :label="column.label" :prop="column.prop" min-width="120" />
      </el-table>
      <pagination v-show="detailTotal > 0" v-model:page="detailPage" v-model:limit="detailPageSize" :total="detailTotal" @pagination="loadStoreRows" />
    </template>
  </el-drawer>
</template>

<script setup name="FitnessStore" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addStore, delStore, getStore, listStore, updateStore } from '@/api/fitness/store';
import { listCoach } from '@/api/fitness/coach';
import { listMember } from '@/api/fitness/member';
import { listCourse } from '@/api/fitness/course';
import { useRouter } from 'vue-router';
import auth from '@/plugins/auth';
import { openFitnessPage } from '../components/navigation';

const router = useRouter();
const detailVisible = ref(false);
const selectedStore = ref<any>();
const detailTab = ref('coach');
const detailRows = ref<any[]>([]);
const detailTotal = ref(0);
const detailPage = ref(1);
const detailPageSize = ref(10);
const detailLoading = ref(false);
let detailRequest = 0;
const availableTabs = computed(() => [
  { key: 'coach', label: '本店教练' }, { key: 'member', label: '归属会员' }, { key: 'course', label: '课程模板' }
].filter((tab) => auth.hasPermi(`fitness:${tab.key}:list`)));
const detailColumns = computed(() => ({
  coach: [{ prop: 'coachName', label: '教练姓名' }, { prop: 'mobile', label: '手机号' }, { prop: 'skillTags', label: '擅长方向' }],
  member: [{ prop: 'realName', label: '会员姓名' }, { prop: 'memberNo', label: '会员编号' }, { prop: 'mobile', label: '手机号' }],
  course: [{ prop: 'courseName', label: '课程名称' }, { prop: 'durationMinutes', label: '时长（分钟）' }, { prop: 'capacity', label: '人数上限' }, { prop: 'displayPrice', label: '展示价格（元）' }]
}[detailTab.value] || []));
async function openDetail(row: any) {
  selectedStore.value = row;
  detailVisible.value = true;
  detailPage.value = 1;
  detailTab.value = availableTabs.value[0]?.key || '';
  await loadStoreRows();
}
async function loadStoreRows() {
  const requestId = ++detailRequest;
  detailRows.value = [];
  detailTotal.value = 0;
  if (!selectedStore.value || !availableTabs.value.some((tab) => tab.key === detailTab.value)) return;
  detailLoading.value = true;
  const query = { pageNum: detailPage.value, pageSize: detailPageSize.value };
  try {
    const result: any = detailTab.value === 'member'
      ? await listMember({ ...query, homeStoreId: selectedStore.value.storeId })
      : detailTab.value === 'coach' ? await listCoach({ ...query, storeId: selectedStore.value.storeId })
        : await listCourse({ ...query, storeId: selectedStore.value.storeId });
    if (requestId !== detailRequest) return;
    detailRows.value = result.rows || [];
    detailTotal.value = result.total || 0;
  } finally { if (requestId === detailRequest) detailLoading.value = false; }
}
function changeDetailTab() { detailPage.value = 1; loadStoreRows(); }
function manage(page: string) {
  const storeId = String(selectedStore.value.storeId);
  openFitnessPage(router, page, page === 'member' ? { homeStoreId: storeId } : { storeId });
}

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' }
];

const pageConfig = reactive({
  title: '门店',
  permPrefix: 'fitness:store',
  idKey: 'storeId',
  exportUrl: 'fitness/store/export',
  queryFields: [
    { label: '门店名称', prop: 'storeName' },
    { label: '联系电话', prop: 'contactPhone' },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions }
  ],
  formFields: [
    { label: '门店编码', prop: 'storeCode' },
    { label: '门店名称', prop: 'storeName' },
    { label: '门店简称', prop: 'storeShortName' },
    { label: '联系人', prop: 'contactName' },
    { label: '联系电话', prop: 'contactPhone' },
    { label: '营业时间', prop: 'businessHours' },
    { label: '详细地址', prop: 'address', span: 24 },
    { label: '状态', prop: 'status', type: 'select', options: statusOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '门店编码', prop: 'storeCode' },
    { label: '门店名称', prop: 'storeName' },
    { label: '联系人', prop: 'contactName' },
    { label: '联系电话', prop: 'contactPhone' },
    { label: '营业时间', prop: 'businessHours' },
    { label: '状态', prop: 'status', type: 'tag', options: statusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    storeName: '',
    contactPhone: '',
    status: ''
  },
  initialForm: {
    storeId: undefined,
    storeCode: '',
    storeName: '',
    storeShortName: '',
    contactName: '',
    contactPhone: '',
    businessHours: '',
    address: '',
    status: '0',
    remark: ''
  },
  rules: {
    storeCode: [{ required: true, message: '门店编码不能为空', trigger: 'blur' }],
    storeName: [{ required: true, message: '门店名称不能为空', trigger: 'blur' }]
  },
  listFn: listStore,
  getFn: getStore,
  addFn: addStore,
  updateFn: updateStore,
  delFn: delStore
});
</script>
