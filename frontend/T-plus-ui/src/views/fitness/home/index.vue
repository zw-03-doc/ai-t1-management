<template>
  <div class="fitness-report p-2" v-loading="loading">
    <el-row :gutter="16" class="mb-4">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-card__label">{{ card.label }}</div>
          <div class="summary-card__value">
            {{ card.value }}
            <span v-if="card.unit" class="summary-card__unit">{{ card.unit }}</span>
          </div>
          <div class="summary-card__desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-4">
      <el-col :xs="24" :xl="14">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-title">经营概览</div>
          </template>
          <div class="overview-list">
            <div v-for="item in overviewItems" :key="item.label" class="overview-item">
              <div class="overview-item__top">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="overview-item__hint">{{ item.hint }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="10">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-title">风险提醒</div>
          </template>
          <div class="metric-list">
            <div v-for="item in riskCards" :key="item.label" class="metric-item metric-item--warn">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-title">本月经营指标</div>
          </template>
          <div class="metric-list metric-list--grid">
            <div v-for="item in monthCards" :key="item.label" class="metric-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="FitnessReportOverview" lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getHomeOverview } from '@/api/fitness/home';
import type { FitnessHomeOverviewVO } from '@/api/fitness/home/types';

const loading = ref(false);
const overview = ref<FitnessHomeOverviewVO>({});

const stats = computed(() => overview.value.stats || {});

const moneyText = (value?: number) => Number(value || 0).toFixed(2);
const percentText = (numerator?: number, denominator?: number) => {
  const base = Number(denominator || 0);
  if (!base) return '0.0%';
  return `${((Number(numerator || 0) / base) * 100).toFixed(1)}%`;
};

const summaryCards = computed(() => [
  { label: '今日销售额', value: moneyText(stats.value.todaySalesAmount), unit: '元', desc: '仅统计已支付订单' },
  { label: '今日到店人数', value: stats.value.todayArrivalCount || 0, unit: '人', desc: '按签到会员去重' },
  { label: '本月新增会员', value: stats.value.monthNewMemberCount || 0, unit: '人', desc: '自然月累计新增' },
  { label: '本月私教销售额', value: moneyText(stats.value.monthPrivateCourseSalesAmount), unit: '元', desc: '私教课包已支付金额' }
]);

const overviewItems = computed(() => [
  {
    label: '今日预约 / 签到',
    value: `${stats.value.todayReservationCount || 0} / ${stats.value.todayCheckinCount || 0}`,
    hint: '观察当天课程到店与核销执行进度'
  },
  {
    label: '签到核销完成率',
    value: percentText(stats.value.todayCheckinCount, stats.value.todayReservationCount),
    hint: '已放入报表中心经营概览，避免和首页工作台重复'
  },
  {
    label: '7天内到期卡项',
    value: `${stats.value.expiringCardCount || 0} 张`,
    hint: '建议结合会员权益与订单数据安排续费触达'
  },
  {
    label: '待支付订单',
    value: `${stats.value.pendingOrderCount || 0} 单`,
    hint: '线下录单后需及时完成收款确认'
  }
]);

const riskCards = computed(() => [
  { label: '待签到课程', value: `${stats.value.pendingCheckinCount || 0} 节` },
  { label: '待处理预约', value: `${stats.value.pendingReservationCount || 0} 条` },
  { label: '待支付订单', value: `${stats.value.pendingOrderCount || 0} 单` },
  { label: '即将到期卡项', value: `${stats.value.expiringCardCount || 0} 张` }
]);

const monthCards = computed(() => [
  { label: '本月续费单量', value: `${stats.value.monthRechargeOrderCount || 0} 单` },
  { label: '本月预约数', value: `${stats.value.monthReservationCount || 0} 次` },
  { label: '今日新增会员', value: `${stats.value.todayNewMemberCount || 0} 人` },
  { label: '今日签到数', value: `${stats.value.todayCheckinCount || 0} 次` }
]);

const loadOverview = async () => {
  loading.value = true;
  try {
    const { data } = await getHomeOverview();
    overview.value = data || {};
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadOverview();
});
</script>

<style scoped>
.fitness-report {
  background: var(--el-bg-color-page);
}

.summary-card {
  border: none;
  border-radius: 16px;
}

.summary-card__label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.summary-card__value {
  margin: 12px 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.summary-card__unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 500;
}

.summary-card__desc {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.panel-card {
  height: 100%;
  border-radius: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
}

.metric-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-list--grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
}

.metric-item--warn strong {
  color: var(--el-color-warning);
}

.overview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-item {
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(180deg, var(--el-fill-color-light) 0%, #fff 100%);
  border: 1px solid var(--el-border-color-lighter);
}

.overview-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.overview-item__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
