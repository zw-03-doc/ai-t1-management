<template>
  <div class="p-2" v-loading="loading">
    <el-row :gutter="16">
      <!-- 左侧会员信息 -->
      <el-col :span="7" :xs="24">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center justify-between">
              <span>会员信息</span>
              <el-button text @click="goBack">返回列表</el-button>
            </div>
          </template>
          <div class="member-info">
            <el-avatar :size="72" :src="detail.member?.avatarUrl" class="member-avatar">
              {{ detail.member?.realName?.charAt(0) || '?' }}
            </el-avatar>
            <div class="member-name">{{ detail.member?.realName || detail.member?.nickname || '-' }}</div>
            <div class="member-no">编号: {{ detail.member?.memberNo }}</div>
            <el-divider />
            <ul class="info-list">
              <li><span class="label">手机号</span><span class="value">{{ detail.member?.mobile || '-' }}</span></li>
              <li><span class="label">性别</span><span class="value">{{ findDictLabel(genderOptions, detail.member?.gender) || '-' }}</span></li>
              <li><span class="label">归属门店</span><span class="value">{{ detail.member?.homeStoreName || '-' }}</span></li>
              <li><span class="label">会员状态</span>
                <el-tag :type="memberStatusTag(detail.member?.memberStatus)" size="small">
                  {{ findDictLabel(memberStatusOptions, detail.member?.memberStatus) }}
                </el-tag>
              </li>
              <li><span class="label">注册来源</span><span class="value">{{ detail.member?.registerSource || '-' }}</span></li>
              <li><span class="label">最近到店</span><span class="value">{{ detail.member?.lastVisitTime || '-' }}</span></li>
              <li><span class="label">创建时间</span><span class="value">{{ detail.member?.createTime || '-' }}</span></li>
            </ul>
          </div>
        </el-card>

        <!-- 数据概览 -->
        <el-card shadow="hover" class="mt-[10px]">
          <template #header><span>数据概览</span></template>
          <el-row :gutter="12">
            <el-col :span="12" class="mb-[12px]" v-for="stat in stats" :key="stat.label">
              <div class="stat-item">
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 右侧 Tab 内容 -->
      <el-col :span="17" :xs="24">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="持卡权益" name="cards">
              <el-table border :data="detail.cardList" v-if="activeTab === 'cards'" style="width: 100%">
                <el-table-column label="卡号" prop="cardNo" min-width="140" />
                <el-table-column label="卡项名称" prop="cardName" min-width="120" />
                <el-table-column label="卡项类型" prop="planType" min-width="90">
                  <template #default="{ row }">{{ findDictLabel(planTypeOptions, row.planType) }}</template>
                </el-table-column>
                <el-table-column label="总次数" prop="totalTimes" width="70" align="center" />
                <el-table-column label="剩余" prop="remainTimes" width="70" align="center" />
                <el-table-column label="余额" prop="balanceAmount" width="100" align="right">
                  <template #default="{ row }">{{ row.balanceAmount != null ? '¥' + row.balanceAmount : '-' }}</template>
                </el-table-column>
                <el-table-column label="有效期" min-width="170">
                  <template #default="{ row }">{{ row.activeTime }} ~ {{ row.expireTime }}</template>
                </el-table-column>
                <el-table-column label="状态" prop="cardStatus" width="80">
                  <template #default="{ row }">
                    <el-tag :type="cardStatusTag(row.cardStatus)" size="small">
                      {{ findDictLabel(cardStatusOptions, row.cardStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.cardList?.length" description="暂无持卡权益" />
            </el-tab-pane>

            <el-tab-pane label="订单" name="orders">
              <el-table border :data="detail.orderList" v-if="activeTab === 'orders'" style="width: 100%">
                <el-table-column label="订单号" prop="orderNo" min-width="160" />
                <el-table-column label="订单类型" prop="orderType" min-width="90">
                  <template #default="{ row }">{{ findDictLabel(orderTypeOptions, row.orderType) }}</template>
                </el-table-column>
                <el-table-column label="金额" prop="payableAmount" width="100" align="right">
                  <template #default="{ row }">¥{{ row.payableAmount }}</template>
                </el-table-column>
                <el-table-column label="实付" prop="paidAmount" width="100" align="right">
                  <template #default="{ row }">¥{{ row.paidAmount || 0 }}</template>
                </el-table-column>
                <el-table-column label="状态" prop="orderStatus" width="80">
                  <template #default="{ row }">
                    <el-tag :type="orderStatusTag(row.orderStatus)" size="small">
                      {{ findDictLabel(orderStatusOptions, row.orderStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="支付方式" prop="payMethod" width="90">
                  <template #default="{ row }">{{ findDictLabel(payMethodOptions, row.payMethod) || '-' }}</template>
                </el-table-column>
                <el-table-column label="支付时间" prop="paidTime" min-width="140" />
                <el-table-column label="门店" prop="storeName" min-width="100" />
              </el-table>
              <el-empty v-if="!detail.orderList?.length" description="暂无订单记录" />
            </el-tab-pane>

            <el-tab-pane label="预约" name="reservations">
              <el-table border :data="detail.reservationList" v-if="activeTab === 'reservations'" style="width: 100%">
                <el-table-column label="预约编号" prop="reservationNo" min-width="150" />
                <el-table-column label="课程" prop="courseName" min-width="120" />
                <el-table-column label="教练" prop="coachName" min-width="80" />
                <el-table-column label="排课日期" prop="scheduleDate" width="100" />
                <el-table-column label="时段" min-width="110">
                  <template #default="{ row }">{{ row.startTime }}~{{ row.endTime }}</template>
                </el-table-column>
                <el-table-column label="预约类型" prop="reservationType" width="90">
                  <template #default="{ row }">{{ findDictLabel(reservationTypeOptions, row.reservationType) || row.reservationType }}</template>
                </el-table-column>
                <el-table-column label="状态" prop="reservationStatus" width="80">
                  <template #default="{ row }">
                    <el-tag :type="reservationStatusTag(row.reservationStatus)" size="small">
                      {{ findDictLabel(reservationStatusOptions, row.reservationStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="扣费模式" prop="consumeMode" width="80">
                  <template #default="{ row }">{{ findDictLabel(consumeModeOptions, row.consumeMode) || '-' }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.reservationList?.length" description="暂无预约记录" />
            </el-tab-pane>

            <el-tab-pane label="签到核销" name="checkins">
              <el-table border :data="detail.checkinList" v-if="activeTab === 'checkins'" style="width: 100%">
                <el-table-column label="签到编号" prop="checkinNo" min-width="150" />
                <el-table-column label="课程" prop="courseName" min-width="120" />
                <el-table-column label="教练" prop="coachName" min-width="80" />
                <el-table-column label="签到时间" prop="checkinTime" min-width="140" />
                <el-table-column label="核销人" prop="operatorUserName" width="80" />
                <el-table-column label="消耗次数" prop="consumeTimes" width="80" align="center" />
                <el-table-column label="消耗金额" prop="consumeAmount" width="90" align="right">
                  <template #default="{ row }">{{ row.consumeAmount ? '¥' + row.consumeAmount : '-' }}</template>
                </el-table-column>
                <el-table-column label="状态" prop="checkinStatus" width="80">
                  <template #default="{ row }">
                    <el-tag :type="checkinStatusTag(row.checkinStatus)" size="small">
                      {{ findDictLabel(checkinStatusOptions, row.checkinStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.checkinList?.length" description="暂无签到记录" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="FitnessMemberDetail" lang="ts">
import { getMemberDetail } from '@/api/fitness/member';
import { useRoute, useRouter } from 'vue-router';
import { useFitnessDictOptions, findDictLabel } from '../../components/useFitnessDictOptions';

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const activeTab = ref('cards');
const detail = ref<any>({});

const {
  fit_member_status: memberStatusOptions,
  fit_plan_type: planTypeOptions,
  fit_card_status: cardStatusOptions,
  fit_order_type: orderTypeOptions,
  fit_order_status: orderStatusOptions,
  fit_pay_method: payMethodOptions,
  fit_reservation_status: reservationStatusOptions,
  fit_reservation_source: reservationTypeOptions,
  fit_consume_mode: consumeModeOptions,
  fit_checkin_status: checkinStatusOptions,
  sys_user_sex: genderOptions
} = useFitnessDictOptions();

const stats = computed(() => [
  { label: '持卡数', value: detail.value.cardCount ?? 0 },
  { label: '有效卡', value: detail.value.activeCardCount ?? 0 },
  { label: '订单数', value: detail.value.orderCount ?? 0 },
  { label: '预约数', value: detail.value.reservationCount ?? 0 },
]);

function memberStatusTag(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { normal: 'success', frozen: 'warning', lost: 'danger' };
  return map[status || ''] || 'info';
}
function cardStatusTag(status?: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = { active: 'success', expired: 'danger', frozen: 'warning', used_up: 'info' };
  return map[status || ''] || 'info';
}
function orderStatusTag(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = { paid: 'success', pending: 'warning', cancelled: 'info', refunded: 'danger' };
  return map[status || ''] || 'info';
}
function reservationStatusTag(status?: string) {
  const map: Record<string, 'primary' | 'success' | 'info' | 'danger' | 'warning'> = { reserved: 'primary', signed: 'success', cancelled: 'info', absent: 'danger', leave: 'warning' };
  return map[status || ''] || 'info';
}
function checkinStatusTag(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { confirmed: 'success', pending_confirm: 'warning', exception: 'danger', not_checkin: 'info' };
  return map[status || ''] || 'info';
}

async function fetchDetail() {
  const memberId = route.query.memberId as string;
  if (!memberId) return;
  loading.value = true;
  try {
    const res = await getMemberDetail(memberId);
    detail.value = res.data;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.back();
}

fetchDetail();
</script>

<style scoped>
.member-info {
  text-align: center;
}
.member-avatar {
  margin: 0 auto 12px;
}
.member-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}
.member-no {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
}
.info-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.info-list li:last-child {
  border-bottom: none;
}
.info-list .label {
  color: var(--el-text-color-secondary);
}
.info-list .value {
  color: var(--el-text-color-primary);
  text-align: right;
}
.stat-item {
  text-align: center;
  padding: 8px 0;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
</style>
