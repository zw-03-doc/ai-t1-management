<template>
  <main v-loading="loading" class="app-container workbench">
    <header><h1>门店工作台</h1><time>{{ currentDateText }}</time></header>
    <nav aria-label="快捷操作">
      <button v-for="action in shortcutList" :key="action.shortcutCode" v-hasPermi="[action.permission]"
        type="button" @click="openRoute(action.route, action.shortcutName)">{{ action.shortcutName }}</button>
    </nav>
    <dl class="metrics">
      <div v-for="card in topCards" :key="card.label"><dt>{{ card.label }}</dt><dd>{{ card.value }}<small>{{ card.unit }}</small></dd></div>
    </dl>
    <div class="columns">
      <section><h2>今日排课</h2>
        <button v-for="item in scheduleItems" :key="item.scheduleId" class="entry" type="button"
          @click="openRoute('/fitness-course/schedule', '排课管理')">
          <time>{{ item.time }}</time><span class="course"><strong>{{ item.course }}</strong><small>{{ item.store }} · {{ item.coach }} · {{ item.progress }}</small></span>
          <el-tag :type="item.type" effect="plain">{{ item.status }}</el-tag>
        </button>
        <p v-if="!scheduleItems.length && !loading">今天暂无排课</p>
      </section>
      <section><h2>待办事项</h2>
        <button v-for="todo in overview.todoList" :key="todo.todoCode" class="entry" type="button" @click="openRoute(todo.route, todo.todoName)">
          <span>{{ todo.todoName }}</span><strong>{{ todo.todoCount ?? 0 }}</strong>
        </button>
        <p v-if="!overview.todoList?.length && !loading">暂无待办</p>
      </section>
    </div>
  </main>
</template>

<script setup name="Index" lang="ts">
import { computed, getCurrentInstance, onMounted, ref, type ComponentInternalInstance } from 'vue';
import { getHomeOverview } from '@/api/fitness/home';
import type { FitnessHomeOverviewVO, FitnessHomeScheduleVO, FitnessHomeShortcutVO } from '@/api/fitness/home/types';

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
const loading = ref(false);
const overview = ref<FitnessHomeOverviewVO>({});

const now = new Date();
const currentDateText = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(now);

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

const topCards = computed(() => [
  { label: '今日到店', value: stats.value.todayArrivalCount ?? 0, unit: '人' },
  { label: '新增会员', value: stats.value.todayNewMemberCount ?? 0, unit: '人' },
  { label: '已支付销售额', value: moneyText(stats.value.todaySalesAmount), unit: '元' },
  { label: '预约 / 签到', value: (stats.value.todayReservationCount ?? 0) + ' / ' + (stats.value.todayCheckinCount ?? 0), unit: '' }
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

<style scoped>
.workbench { max-width: 1340px; margin: auto; color: var(--el-text-color-primary); }
header,nav,.entry { display: flex; align-items: center; gap: 12px; }
header { justify-content: space-between; flex-wrap: wrap; }
h1 { margin: 8px 0; font-size: clamp(25px, 3vw, 38px); letter-spacing: -.05em; }
h2 { font-size: 18px; margin: 0 0 14px; }
time,small,p,dt { color: var(--el-text-color-secondary); }
nav { flex-wrap: wrap; padding: 20px 0; }
button { font: inherit; color: inherit; cursor: pointer; background: var(--el-bg-color); border: 1px solid var(--el-border-color); border-radius: 7px; }
nav button { padding: 9px 15px; }
button:hover { background: var(--el-fill-color-light); }
button:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.metrics { display: grid; grid-template-columns: repeat(4,1fr); gap: 20px; margin: 0 0 32px; padding: 20px 0 26px; border-block: 1px solid var(--el-border-color); }
dd { margin: 8px 0 0; font-size: clamp(22px,3vw,36px); font-variant-numeric: tabular-nums; }
dd small { font-size: 13px; margin-left: 5px; }
.columns { display: grid; grid-template-columns: 3fr 2fr; gap: 32px; }
.entry { width: 100%; justify-content: space-between; text-align: left; padding: 14px 4px; background: transparent; border: 0; border-bottom: 1px solid var(--el-border-color-lighter); border-radius: 0; }
.course { display: grid; gap: 4px; flex: 1; }
@media(max-width:760px){.columns { grid-template-columns: 1fr; } .metrics { grid-template-columns: 1fr 1fr; }}
</style>
