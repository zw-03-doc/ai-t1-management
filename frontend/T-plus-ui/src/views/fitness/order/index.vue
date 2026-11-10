<template>
  <div>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #field-quotedPlanPrice="{ model }">
      <el-form-item label="应付金额" prop="quotedPlanPrice">
        <span>{{ model.quotedPlanPrice == null ? '请先选择卡项方案' : '¥' + Number(model.quotedPlanPrice).toFixed(2) }}</span>
      </el-form-item>
    </template>
    <template #field-settlementAmount="{ model }">
      <el-form-item label="实付金额" prop="settlementAmount">
        <el-input-number v-if="model.quotedPlanPrice != null" :key="model.planId" v-model="model.settlementAmount"
          :precision="2" :min="0" :max="model.quotedPlanPrice" :controls="false" aria-label="实付金额" style="width:100%" />
        <el-input v-else disabled placeholder="请先选择卡项方案" />
      </el-form-item>
    </template>
    <template #row-actions="{ row, refresh }">
      <el-tooltip v-if="row.orderStatus === 'pending'" content="确认线下收款" placement="top">
        <el-button v-hasPermi="['fitness:order:edit']" link type="success" icon="CircleCheck" @click="handlePay(row, refresh)" />
      </el-tooltip>
      <el-tooltip v-if="row.orderStatus === 'pending'" content="取消订单" placement="top">
        <el-button v-hasPermi="['fitness:order:edit']" link type="danger" icon="CircleClose" @click="handleCancel(row, refresh)" />
      </el-tooltip>
    </template>
  </FitnessCrudPage>
  <el-dialog v-model="payDialog.visible" title="确认线下收款并发卡" width="560px" :close-on-click-modal="!paying" :close-on-press-escape="!paying" :show-close="!paying">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="会员">{{ payDialog.order.memberName }} {{ payDialog.order.memberMobile }}</el-descriptions-item>
      <el-descriptions-item label="门店">{{ payDialog.order.storeName }}</el-descriptions-item>
      <el-descriptions-item label="卡项方案">{{ payDialog.order.planName || '请核对订单明细' }}</el-descriptions-item>
      <el-descriptions-item label="应付金额">¥{{ Number(payDialog.order.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
      <el-descriptions-item label="本次实收">¥{{ Number(payDialog.order.payableAmount || 0).toFixed(2) }}</el-descriptions-item>
    </el-descriptions>
    <el-form label-width="100px" class="mt-4">
      <el-form-item label="绑定教练">
        <el-select v-model="payDialog.bindCoachId" filterable remote :remote-method="searchPayCoaches" :loading="coachLoading" :disabled="paying" placeholder="私教卡必选；普通卡可不绑定" @visible-change="visible => visible && searchPayCoaches('')">
          <el-option v-for="coach in payCoaches" :key="String(coach.value)" :label="coach.label" :value="coach.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="收款方式">
        <el-select v-model="payDialog.payMethod" :disabled="paying">
          <el-option v-for="method in payMethodOptions" :key="method.value" :label="method.label" :value="method.value" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="paying" @click="payDialog.visible=false">取消</el-button>
      <el-button type="primary" :loading="paying" @click="confirmPayment">已收款，确认发卡</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup name="FitnessOrder" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addOrder, cancelOrder, delOrder, getOrder, listOrder, payOrder, refundOrder, updateOrder } from '@/api/fitness/order';
import { loadCardPlanOptions, loadCoachOptions, loadMemberOptions, loadStoreOptions, loadSystemUserOptions } from '../components/remoteOptions';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const orderTypeOptions = [
  { label: '购卡', value: 'buy_card' },
  { label: '充值', value: 'recharge' },
  { label: '购课', value: 'buy_course' },
  { label: '体验单', value: 'experience' }
];
const orderSourceOptions = [
  { label: '后台', value: 'admin' },
  { label: '小程序', value: 'mini' },
  { label: '教练端', value: 'coach_app' }
];
const orderStatusOptions = [
  { label: '待收款', value: 'pending' },
  { label: '已收款并发卡', value: 'paid' },
  { label: '已取消', value: 'cancelled' },
  { label: '已退款', value: 'refunded' },
  { label: '已关闭', value: 'closed' }
];
const payMethodOptions = [
  { label: '线下微信收款', value: 'wechat' },
  { label: '现金', value: 'cash' },
  { label: '转账', value: 'transfer' },
  { label: '会员卡抵扣', value: 'card' },
  { label: '组合支付', value: 'mix' }
];
const payNowOptions = [
  { label: '否', value: false },
  { label: '是', value: true }
];

const pageConfig = reactive({
  title: '订单',
  permPrefix: 'fitness:order',
  idKey: 'orderId',
  exportUrl: 'fitness/order/export',
  queryFields: [
    { label: '订单号', prop: 'orderNo' },
    { label: '会员', prop: 'memberId', type: 'remoteSelect', remoteMethod: loadMemberOptions },
    { label: '订单类型', prop: 'orderType', type: 'select', options: orderTypeOptions },
    { label: '订单状态', prop: 'orderStatus', type: 'select', options: orderStatusOptions }
  ],
  formFields: [
    { label: '订单号', prop: 'orderNo', placeholder: '可手动填写，不填则自动生成' },
    { label: '门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '会员', prop: 'memberId', type: 'remoteSelect', remoteMethod: loadMemberOptions },
    { label: '卡项方案', prop: 'planId', type: 'remoteSelect', remoteMethod: loadCardPlanOptions, lockOnEdit: true,
      onSelect: (model: any, plan?: { salePrice?: number }) => {
        model.quotedPlanPrice = plan ? Number(plan.salePrice ?? 0) : undefined;
        model.settlementAmount = model.quotedPlanPrice;
      } },
    { label: '应付金额', prop: 'quotedPlanPrice', type: 'custom' },
    { label: '实付金额', prop: 'settlementAmount', type: 'custom' },
    { label: '绑定教练', prop: 'bindCoachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '订单类型', prop: 'orderType', type: 'select', options: orderTypeOptions },
    { label: '订单来源', prop: 'orderSource', type: 'select', options: orderSourceOptions },
    { label: '支付方式', prop: 'payMethod', type: 'select', options: payMethodOptions },
    { label: '收银员', prop: 'cashierUserId', type: 'remoteSelect', remoteMethod: loadSystemUserOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '会员', prop: 'memberName', minWidth: 164 },
    { label: '卡项方案', prop: 'planName', minWidth: 136 },
    { label: '门店', prop: 'storeName', minWidth: 126 },
    { label: '应付金额', prop: 'totalAmount' },
    { label: '实付金额', prop: 'receiptText', minWidth: 136 },
    { label: '订单状态', prop: 'orderStatus', type: 'tag', options: orderStatusOptions },
    { label: '创建时间', prop: 'createTime', type: 'date', minWidth: 154 }
  ],
  detailColumns: [
    { label: '订单号', prop: 'orderNo' },
    { label: '绑定教练', prop: 'coachName' },
    { label: '约定收款金额', prop: 'payableAmount' },
    { label: '订单类型', prop: 'orderType', type: 'tag', options: orderTypeOptions },
    { label: '订单来源', prop: 'orderSource', type: 'tag', options: orderSourceOptions },
    { label: '支付方式', prop: 'payMethod', type: 'tag', options: payMethodOptions }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    memberId: '',
    orderType: '',
    orderStatus: ''
  },
  initialForm: {
    orderId: undefined,
    orderNo: '',
    storeId: undefined,
    memberId: undefined,
    planId: undefined,
    bindCoachId: undefined,
    orderType: 'buy_card',
    orderSource: 'admin',
    orderStatus: 'pending',
    payMethod: 'cash',
    payNow: false,
    quotedPlanPrice: undefined,
    settlementAmount: undefined,
    cashierUserId: undefined,
    remark: ''
  },
  rules: {
    settlementAmount: [{ required: true, message: '请输入实付金额', trigger: 'blur' }],
    memberId: [{ required: true, message: '会员不能为空', trigger: 'change' }],
    planId: [{ required: true, message: '卡项方案不能为空', trigger: 'change' }]
  },
  listFn: async (query: any) => {
    const result: any = await listOrder(query);
    return { ...result, rows: (result.rows || []).map((row: any) => ({
      ...row,
      receiptText: row.orderStatus === 'pending'
        ? '¥' + Number(row.payableAmount ?? 0).toFixed(2) + '（待确认）'
        : '¥' + Number(row.paidAmount ?? 0).toFixed(2)
    })) };
  },
  getFn: async (id: string | number) => {
    const result = await getOrder(id);
    return { ...result, data: { ...result.data, quotedPlanPrice: result.data.totalAmount, settlementAmount: result.data.payableAmount } };
  },
  addFn: addOrder,
  updateFn: updateOrder,
  delFn: delOrder
});

const paying = ref(false);
const openingPay = ref(false);
const coachLoading = ref(false);
const payCoaches = ref<any[]>([]);
const payDialog = reactive({ visible: false, order: {} as any, bindCoachId: undefined as string | number | undefined, payMethod: 'cash' });
let payRefresh: (() => Promise<void>) | undefined;
let coachRequest = 0;
async function searchPayCoaches(keyword: string) {
  const requestId = ++coachRequest;
  coachLoading.value = true;
  try {
    const options = await loadCoachOptions({ keyword, value: payDialog.bindCoachId, model: { storeId: payDialog.order.storeId, hireStatus: '0' } });
    if (requestId === coachRequest) payCoaches.value = options;
  } finally { if (requestId === coachRequest) coachLoading.value = false; }
}
const handlePay = async (row: any, refresh: () => Promise<void>) => {
  if (openingPay.value || paying.value) return;
  openingPay.value = true;
  try {
    const { data } = await getOrder(row.orderId);
    payDialog.order = data;
    payDialog.bindCoachId = data.bindCoachId;
    payDialog.payMethod = payMethodOptions.some(option => option.value === data.payMethod) ? data.payMethod! : 'cash';
    payCoaches.value = data.bindCoachId ? [{ value: data.bindCoachId, label: data.coachName || '原绑定教练（请核对）' }] : [];
    payRefresh = refresh;
    payDialog.visible = true;
    await searchPayCoaches('');
  } finally { openingPay.value = false; }
};
async function confirmPayment() {
  if (paying.value) return;
  paying.value = true;
  try {
    await payOrder(payDialog.order.orderId, { payMethod: payDialog.payMethod, bindCoachId: payDialog.bindCoachId, payableAmount: payDialog.order.payableAmount, payNow: true });
    payDialog.visible = false;
    proxy?.$modal.msgSuccess('线下收款已确认，会员卡已发放');
    await payRefresh?.();
  } finally { paying.value = false; }
}

const handleCancel = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('请输入取消原因，可留空')!;
    await cancelOrder(row.orderId, value);
    proxy?.$modal.msgSuccess('订单已取消');
    await refresh();
  } catch {
    // 用户取消
  }
};

const handleRefund = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('当前仅支持全额退款。请输入退款原因，可留空')!;
    await refundOrder(row.orderId, { remark: value || '', refundAmount: row.paidAmount });
    proxy?.$modal.msgSuccess('订单已退款并回滚权益');
    await refresh();
  } catch {
    // 用户取消
  }
};
</script>

