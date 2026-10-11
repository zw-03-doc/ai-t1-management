<template>
  <div class="app-container workbench-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-card__content">
        <div class="hero-card__main">
          <div class="hero-card__eyebrow">一期工作台</div>
          <h2 class="hero-card__title">{{ greeting }}，{{ displayName }}</h2>
          <p class="hero-card__desc">
            首页聚焦今日到店、收银、快捷处理和现场节奏，经营分析统一收敛到报表中心，避免和工作台重复。
          </p>
          <div class="hero-card__meta">
            <span>日期：{{ currentDateText }}</span>
            <span>租户：{{ userStore.tenantId || '未识别' }}</span>
            <span>状态：实时数据已接入</span>
          </div>
          <div class="hero-card__tags">
            <el-tag v-for="role in displayRoles" :key="role" round effect="light">{{ role }}</el-tag>
            <el-tag type="success" round effect="light">工作台实时数据</el-tag>
          </div>
        </div>
        <div class="hero-card__side">
          <div class="focus-panel">
            <div class="focus-panel__title">今日关注</div>
            <div v-for="item in focusItems" :key="item.label" class="focus-panel__item">
              <div>
                <div class="focus-panel__label">{{ item.label }}</div>
                <div class="focus-panel__hint">{{ item.hint }}</div>
              </div>
              <div class="focus-panel__value">{{ item.value }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <el-row :gutter="16" class="stats-row">
      <el-col v-for="card in topCards" :key="card.label" :xs="24" :sm="12" :lg="12" :xl="6">
        <div class="metric-card" :class="`metric-card--${card.tone}`">
          <div class="metric-card__badge">{{ card.badge }}</div>
          <div class="metric-card__value">
            {{ card.value }}
            <span v-if="card.unit" class="metric-card__unit">{{ card.unit }}</span>
          </div>
          <div class="metric-card__label">{{ card.label }}</div>
          <div class="metric-card__foot">{{ card.note }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="panel-row">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-card__header">
              <div>
                <div class="panel-card__title">快捷入口</div>
                <div class="panel-card__subtitle">常用业务动作保留在首页，前台与店长可直接进入处理链路</div>
              </div>
            </div>
          </template>
          <div class="quick-grid">
            <button
              v-for="action in shortcutList"
              :key="action.shortcutCode"
              v-hasPermi="[action.permission]"
              class="quick-card"
              type="button"
              @click="openRoute(action.route, action.shortcutName)"
            >
              <div class="quick-card__code">{{ action.icon || action.shortcutCode || '快捷' }}</div>
              <div class="quick-card__title">{{ action.shortcutName }}</div>
              <div class="quick-card__desc">{{ action.route }}</div>
            </button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-card__header">
              <div>
                <div class="panel-card__title">待办中心</div>
                <div class="panel-card__subtitle">按首页设计文档保留待签到、待处理预约、待支付、到期提醒四类事项</div>
              </div>
            </div>
          </template>
          <div class="todo-list">
            <button
              v-for="todo in todoList"
              :key="todo.todoCode"
              class="todo-item"
              type="button"
              @click="openRoute(todo.route, todo.todoName)"
            >
              <div class="todo-item__left">
                <div class="todo-item__title">{{ todo.todoName }}</div>
                <div class="todo-item__desc">{{ todo.desc }}</div>
              </div>
              <el-tag :type="todo.type" effect="light">{{ todo.tag }}</el-tag>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="panel-row">
      <el-col :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-card__header">
              <div>
                <div class="panel-card__title">今日课程与现场节奏</div>
                <div class="panel-card__subtitle">展示待上课课程、签到压力和现场安排</div>
              </div>
            </div>
          </template>
          <div v-if="scheduleItems.length" class="schedule-list">
            <button
              v-for="item in scheduleItems"
              :key="`${item.scheduleId}-${item.time}`"
              class="schedule-item"
              type="button"
              @click="openRoute('/fitness-course/schedule', '排课管理')"
            >
              <div class="schedule-item__time">{{ item.time }}</div>
              <div class="schedule-item__main">
                <div class="schedule-item__title">{{ item.course }}</div>
                <div class="schedule-item__meta">{{ item.store }} · {{ item.coach }} · {{ item.progress }}</div>
              </div>
              <el-tag :type="item.type" effect="light">{{ item.status }}</el-tag>
            </button>
          </div>
          <el-empty v-else description="今天暂无课程安排" :image-size="92" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index" lang="ts">
import { computed, getCurrentInstance, onMounted, ref, type ComponentInternalInstance } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { getHomeOverview } from '@/api/fitness/home';
import type { FitnessHomeOverviewVO, FitnessHomeScheduleVO, FitnessHomeShortcutVO, FitnessHomeTodoVO } from '@/api/fitness/home/types';

interface MetricCard {
  badge: string;
  label: string;
  value: string;
  unit?: string;
  note: string;
  tone: 'primary' | 'success' | 'warning' | 'brand';
}

interface TodoCard extends FitnessHomeTodoVO {
  desc: string;
  tag: string;
  type: 'success' | 'warning' | 'info' | 'danger';
}

interface ScheduleCard {
  scheduleId?: number;
  time: string;
  course: string;
  store: string;
  coach: string;
  progress: string;
  status: string;
  type: 'success' | 'warning' | 'info' | 'danger';
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const userStore = useUserStore();
const loading = ref(false);
const overview = ref<FitnessHomeOverviewVO>({});

const roleLabelMap: Record<string, string> = {
  super_admin: '平台管理员',
  tenant_admin: '商户管理员',
  store_manager: '门店店长',
  front_desk: '前台/会籍',
  coach_user: '教练账号',
  finance_viewer: '财务/运营',
  ROLE_DEFAULT: '当前账号'
};

const now = new Date();
const hour = now.getHours();

const greeting = computed(() => {
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const displayName = computed(() => userStore.nickname || '伙伴');

const currentDateText = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(now);

const displayRoles = computed(() => {
  const roleList = userStore.roles.length ? userStore.roles : ['ROLE_DEFAULT'];
  return [...new Set(roleList.map((role) => roleLabelMap[role] || role))].slice(0, 4);
});

const stats = computed(() => overview.value.stats || {});

const moneyText = (value?: number) => {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value || 0));
};

const formatTime = (value?: string) => {
  if (!value) return '--:--';
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(new Date(value));
};

const topCards = computed<MetricCard[]>(() => [
  { badge: '到店', label: '今日到店人数', value: String(stats.value.todayArrivalCount || 0), unit: '人', note: '按签到会员去重', tone: 'primary' },
  { badge: '新增', label: '今日新增会员', value: String(stats.value.todayNewMemberCount || 0), unit: '人', note: '当天新增入会人数', tone: 'success' },
  { badge: '销售', label: '今日销售额', value: moneyText(stats.value.todaySalesAmount), unit: '元', note: '仅统计已支付订单', tone: 'brand' },
  {
    badge: '进度',
    label: '今日预约 / 签到',
    value: `${stats.value.todayReservationCount || 0} / ${stats.value.todayCheckinCount || 0}`,
    note: '预约与到店核销实时进度',
    tone: 'warning'
  }
]);

const defaultShortcutList: FitnessHomeShortcutVO[] = [
  { shortcutCode: 'memberAdd', shortcutName: '新增会员', route: '/fitness-member/member', permission: 'fitness:member:add', icon: '会员' },
  { shortcutCode: 'orderCreate', shortcutName: '录入订单', route: '/fitness-order/order', permission: 'fitness:order:add', icon: '订单' },
  { shortcutCode: 'scheduleAdd', shortcutName: '快速排课', route: '/fitness-course/schedule', permission: 'fitness:schedule:add', icon: '排课' },
  { shortcutCode: 'checkinHandle', shortcutName: '签到核销', route: '/fitness-course/checkin', permission: 'fitness:checkin:edit', icon: '签到' }
];

const shortcutList = computed(() => {
  const list = overview.value.shortcutList?.length ? overview.value.shortcutList : defaultShortcutList;
  return list.map((item) => ({
    ...item,
    route: item.route || '/fitness-home',
    permission: item.permission || 'fitness:home:view'
  }));
});

const todoList = computed<TodoCard[]>(() => {
  const metaMap: Record<string, (count: number) => Omit<TodoCard, 'todoCode' | 'todoName' | 'todoCount' | 'route'>> = {
    pendingCheckin: (count) => ({
      desc: count > 0 ? `今天还有 ${count} 节课程未完成签到核销` : '今天待签到课程已全部处理完成',
      tag: count > 0 ? '紧急' : '已清空',
      type: count > 0 ? 'danger' : 'success'
    }),
    pendingReservation: (count) => ({
      desc: count > 0 ? `${count} 条预约需要人工确认门店与排课安排` : '当前没有待人工处理的预约记录',
      tag: `${count} 条`,
      type: 'warning'
    }),
    pendingOrder: (count) => ({
      desc: count > 0 ? `${count} 笔线下订单已录入，等待确认收款` : '当前没有待确认收款的订单',
      tag: `${count} 单`,
      type: 'info'
    }),
    expiringCard: (count) => ({
      desc: count > 0 ? `${count} 张卡将在 7 天内到期，建议安排续费触达` : '最近 7 天暂无即将到期卡项',
      tag: `${count} 张`,
      type: 'success'
    })
  };

  return (overview.value.todoList || []).map((item) => {
    const count = Number(item.todoCount || 0);
    const meta = (metaMap[item.todoCode || ''] || metaMap.pendingCheckin)(count);
    return {
      ...item,
      route: item.route || '/fitness-home',
      ...meta
    };
  });
});

const scheduleItems = computed<ScheduleCard[]>(() => {
  return (overview.value.scheduleList || []).map((item: FitnessHomeScheduleVO) => {
    const startTime = item.startTime ? new Date(item.startTime).getTime() : 0;
    const endTime = item.endTime ? new Date(item.endTime).getTime() : 0;
    const current = Date.now();
    const capacity = Number(item.capacity || 0);
    const reservedCount = Number(item.reservedCount || 0);
    const signedCount = Number(item.signedCount || 0);
    const reservedRate = capacity > 0 ? reservedCount / capacity : 0;

    let status = '正常';
    let type: ScheduleCard['type'] = 'info';
    if ((item.scheduleStatus || '').toLowerCase() === 'cancelled') {
      status = '已取消';
      type = 'info';
    } else if (current < startTime) {
      if (capacity > 0 && reservedRate >= 0.9) {
        status = '预约偏满';
        type = 'danger';
      } else {
        status = '待开课';
        type = 'warning';
      }
    } else if (endTime === 0 || current <= endTime) {
      status = '进行中';
      type = 'success';
    }

    const progress = signedCount > 0 ? `已签到 ${signedCount}/${Math.max(reservedCount, signedCount)}` : capacity > 0 ? `已约 ${reservedCount}/${capacity}` : `已约 ${reservedCount}`;

    return {
      scheduleId: item.scheduleId,
      time: formatTime(item.startTime),
      course: item.courseName || '未命名课程',
      store: item.storeName || '未配置门店',
      coach: `教练：${item.coachName || '待排班'}`,
      progress,
      status,
      type
    };
  });
});

const focusItems = computed(() => [
  { label: '待签到课程', value: `${stats.value.pendingCheckinCount || 0} 节`, hint: '建议优先处理现场到店与核销动作' },
  { label: '待支付订单', value: `${stats.value.pendingOrderCount || 0} 单`, hint: '线下录单后需尽快确认收款' },
  { label: '近 7 天到期卡项', value: `${stats.value.expiringCardCount || 0} 张`, hint: '适合安排续费提醒与会员触达' }
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

const openRoute = (route?: string, title?: string) => {
  if (!route) return;
  proxy?.$tab.openPage(route, title);
};

onMounted(() => {
  loadOverview();
});
</script>

<style lang="scss" scoped>
.workbench-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #1f2937;
}

.hero-card {
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(135deg, #111827 0%, #1d4ed8 55%, #7c3aed 100%);
  color: #fff;
  box-shadow: 0 18px 45px rgb(37 99 235 / 18%);

  &__content {
    display: flex;
    gap: 24px;
    justify-content: space-between;
    padding: 28px;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__eyebrow {
    display: inline-flex;
    padding: 6px 12px;
    border-radius: 999px;
    background: rgb(255 255 255 / 14%);
    font-size: 12px;
    letter-spacing: 0.08em;
  }

  &__title {
    margin: 16px 0 12px;
    font-size: 30px;
    line-height: 1.2;
    font-weight: 700;
  }

  &__desc {
    max-width: 760px;
    margin: 0;
    font-size: 14px;
    line-height: 1.8;
    color: rgb(255 255 255 / 82%);
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 18px;
    font-size: 13px;
    color: rgb(255 255 255 / 72%);
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 18px;
  }

  &__side {
    width: 320px;
    flex-shrink: 0;
  }
}

.focus-panel {
  padding: 20px;
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 20px;
  background: rgb(255 255 255 / 10%);
  backdrop-filter: blur(12px);

  &__title {
    margin-bottom: 16px;
    font-size: 15px;
    font-weight: 600;
  }

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 0;
    border-top: 1px solid rgb(255 255 255 / 10%);

    &:first-of-type {
      border-top: none;
      padding-top: 0;
    }
  }

  &__label {
    font-size: 13px;
    color: rgb(255 255 255 / 70%);
  }

  &__hint {
    margin-top: 4px;
    font-size: 12px;
    color: rgb(255 255 255 / 56%);
  }

  &__value {
    font-size: 22px;
    font-weight: 700;
  }
}

.stats-row,
.panel-row {
  margin-top: 0;
}

.metric-card {
  position: relative;
  margin-bottom: 16px;
  padding: 20px;
  border: 1px solid #edf2f7;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 28px rgb(15 23 42 / 4%);

  &__badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 52px;
    height: 28px;
    padding: 0 10px;
    border-radius: 999px;
    background: #eef2ff;
    font-size: 12px;
    font-weight: 600;
  }

  &__value {
    margin-top: 18px;
    font-size: 30px;
    line-height: 1;
    font-weight: 700;
    color: #111827;
  }

  &__unit {
    margin-left: 4px;
    font-size: 13px;
    font-weight: 500;
    color: #6b7280;
  }

  &__label {
    margin-top: 10px;
    font-size: 14px;
    color: #374151;
  }

  &__foot {
    margin-top: 18px;
    font-size: 12px;
    color: #6b7280;
  }

  &--primary .metric-card__badge {
    background: #dbeafe;
    color: #2563eb;
  }

  &--success .metric-card__badge {
    background: #dcfce7;
    color: #16a34a;
  }

  &--warning .metric-card__badge {
    background: #fef3c7;
    color: #d97706;
  }

  &--brand .metric-card__badge {
    background: #ede9fe;
    color: #7c3aed;
  }
}

.panel-card {
  height: 100%;
  border: none;
  border-radius: 22px;
  box-shadow: 0 10px 28px rgb(15 23 42 / 4%);

  :deep(.el-card__body) {
    padding-top: 10px;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  &__title {
    font-size: 16px;
    font-weight: 700;
    color: #111827;
  }

  &__subtitle {
    margin-top: 6px;
    font-size: 12px;
    color: #6b7280;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 10px 20px rgb(99 102 241 / 10%);
    transform: translateY(-2px);
  }

  &__code {
    display: inline-flex;
    width: fit-content;
    padding: 4px 10px;
    border-radius: 999px;
    background: #eef2ff;
    color: #4f46e5;
    font-size: 12px;
    font-weight: 600;
  }

  &__title {
    font-size: 16px;
    font-weight: 700;
    color: #111827;
  }

  &__desc {
    font-size: 13px;
    line-height: 1.7;
    color: #6b7280;
  }
}

.todo-list,
.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.todo-item,
.schedule-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border: none;
  border-bottom: 1px solid #f1f5f9;
  background: transparent;
  text-align: left;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.todo-item {
  &__left {
    min-width: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
  }

  &__desc {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.6;
    color: #6b7280;
  }
}

.schedule-item {
  &__time {
    width: 72px;
    flex-shrink: 0;
    font-size: 15px;
    font-weight: 700;
    color: #1d4ed8;
  }

  &__main {
    min-width: 0;
    flex: 1;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
  }

  &__meta {
    margin-top: 6px;
    font-size: 12px;
    color: #6b7280;
  }
}

@media (max-width: 1200px) {
  .hero-card {
    &__content {
      flex-direction: column;
    }

    &__side {
      width: 100%;
    }
  }
}

@media (max-width: 768px) {
  .hero-card {
    border-radius: 20px;

    &__content {
      padding: 20px;
    }

    &__title {
      font-size: 24px;
    }
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .schedule-item {
    align-items: flex-start;
    flex-wrap: wrap;

    &__time {
      width: auto;
    }
  }
}
</style>
