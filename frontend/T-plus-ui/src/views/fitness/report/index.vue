<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="门店">
          <el-select
            v-model="queryParams.storeId"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="storeLoading"
            placeholder="请选择门店"
            :remote-method="loadStoreSelect"
            @visible-change="handleStoreVisibleChange"
          >
            <el-option v-for="option in storeOptions" :key="String(option.value)" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="queryParams.startDate" type="date" value-format="YYYY-MM-DD" clearable placeholder="请选择开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="queryParams.endDate" type="date" value-format="YYYY-MM-DD" clearable placeholder="请选择结束日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['fitness:report:export']" type="warning" plain icon="Download" @click="handleExport">导出当前报表</el-button>
          </el-col>
          <right-toolbar :show-search="false" @query-table="getList" />
        </el-row>
      </template>

      <el-tabs v-model="activeName" @tab-change="getList">
        <el-tab-pane label="会员报表" name="member">
          <el-table v-loading="loading" border :data="reportList.member">
            <el-table-column label="门店" prop="storeName" align="center" />
            <el-table-column label="新增会员" prop="newMemberCount" align="center" />
            <el-table-column label="活跃会员" prop="activeMemberCount" align="center" />
            <el-table-column label="付费会员" prop="paidMemberCount" align="center" />
            <el-table-column label="有效会员卡" prop="activeCardCount" align="center" />
            <el-table-column label="7天内到期卡" prop="expiringCardCount" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="订单报表" name="order">
          <el-table v-loading="loading" border :data="reportList.order">
            <el-table-column label="门店" prop="storeName" align="center" />
            <el-table-column label="订单类型" prop="orderType" align="center" />
            <el-table-column label="订单数" prop="orderCount" align="center" />
            <el-table-column label="已支付数" prop="paidOrderCount" align="center" />
            <el-table-column label="退款数" prop="refundOrderCount" align="center" />
            <el-table-column label="应收金额" prop="payableAmount" align="center" />
            <el-table-column label="实收金额" prop="paidAmount" align="center" />
            <el-table-column label="退款金额" prop="refundAmount" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="课程报表" name="course">
          <el-table v-loading="loading" border :data="reportList.course">
            <el-table-column label="门店" prop="storeName" align="center" />
            <el-table-column label="课程" prop="courseName" align="center" />
            <el-table-column label="课程类型" prop="courseType" align="center" />
            <el-table-column label="排课数" prop="scheduleCount" align="center" />
            <el-table-column label="预约数" prop="reservationCount" align="center" />
            <el-table-column label="核销数" prop="checkinCount" align="center" />
            <el-table-column label="到课率(%)" prop="attendanceRate" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="教练/门店经营" name="coachStore">
          <el-table v-loading="loading" border :data="reportList.coachStore">
            <el-table-column label="门店" prop="storeName" align="center" />
            <el-table-column label="教练" prop="coachName" align="center" />
            <el-table-column label="排课数" prop="scheduleCount" align="center" />
            <el-table-column label="预约数" prop="reservationCount" align="center" />
            <el-table-column label="核销数" prop="checkinCount" align="center" />
            <el-table-column label="私教销售额" prop="privateCourseSalesAmount" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="FitnessReport" lang="ts">
import { listCoachStoreReport, listCourseReport, listMemberReport, listOrderReport } from '@/api/fitness/report';
import { FitnessReportQuery } from '@/api/fitness/report/types';
import { loadStoreOptions } from '../components/remoteOptions';

type ReportType = 'member' | 'order' | 'course' | 'coachStore';

interface OptionItem {
  label: string;
  value: string | number | boolean;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const activeName = ref<ReportType>('member');
const loading = ref(false);
const storeLoading = ref(false);
const storeOptions = ref<OptionItem[]>([]);

const queryParams = reactive<FitnessReportQuery>({
  storeId: undefined,
  startDate: '',
  endDate: ''
});

const reportList = reactive<Record<ReportType, any[]>>({
  member: [],
  order: [],
  course: [],
  coachStore: []
});

const reportLoaders: Record<ReportType, (query: FitnessReportQuery) => Promise<any>> = {
  member: listMemberReport,
  order: listOrderReport,
  course: listCourseReport,
  coachStore: listCoachStoreReport
};

const exportUrls: Record<ReportType, string> = {
  member: 'fitness/report/member/export',
  order: 'fitness/report/order/export',
  course: 'fitness/report/course/export',
  coachStore: 'fitness/report/coachStore/export'
};

function extractRows(res: any) {
  if (Array.isArray(res?.data)) {
    return res.data;
  }
  if (Array.isArray(res?.rows)) {
    return res.rows;
  }
  return [];
}

const getList = async () => {
  loading.value = true;
  try {
    const res = await reportLoaders[activeName.value]({ ...queryParams });
    reportList[activeName.value] = extractRows(res);
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  getList();
};

const resetQuery = () => {
  queryParams.storeId = undefined;
  queryParams.startDate = '';
  queryParams.endDate = '';
  getList();
};

const loadStoreSelect = async (keyword = '') => {
  storeLoading.value = true;
  try {
    storeOptions.value = await loadStoreOptions({ keyword, value: queryParams.storeId });
  } finally {
    storeLoading.value = false;
  }
};

const handleStoreVisibleChange = (visible: boolean) => {
  if (visible) {
    loadStoreSelect();
  }
};

const handleExport = () => {
  proxy?.download(exportUrls[activeName.value], { ...queryParams }, `fitness_report_${activeName.value}_${new Date().getTime()}.xlsx`);
};

onMounted(() => {
  loadStoreSelect();
  getList();
});
</script>
