<template>
  <FitnessCrudPage v-bind="pageConfig as any">
    <template #row-actions="{ row, refresh }">
      <el-tooltip v-if="row.orderStatus !== 'paid' && row.orderStatus !== 'cancelled'" content="确认支付" placement="top">
        <el-button v-hasPermi="['fitness:order:edit']" link type="success" icon="CircleCheck" @click="handlePay(row, refresh)" />
      </el-tooltip>
      <el-tooltip v-if="row.orderStatus !== 'cancelled'" content="取消订单" placement="top">
        <el-button v-hasPermi="['fitness:order:edit']" link type="danger" icon="CircleClose" @click="handleCancel(row, refresh)" />
      </el-tooltip>
    </template>
  </FitnessCrudPage>
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
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已取消', value: 'cancelled' },
  { label: '已退款', value: 'refunded' },
  { label: '已关闭', value: 'closed' }
];
const payMethodOptions = [
  { label: '微信支付', value: 'wechat' },
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
    { label: '卡项方案', prop: 'planId', type: 'remoteSelect', remoteMethod: loadCardPlanOptions },
    { label: '绑定教练', prop: 'bindCoachId', type: 'remoteSelect', remoteMethod: loadCoachOptions },
    { label: '订单类型', prop: 'orderType', type: 'select', options: orderTypeOptions },
    { label: '订单来源', prop: 'orderSource', type: 'select', options: orderSourceOptions },
    { label: '订单状态', prop: 'orderStatus', type: 'select', options: orderStatusOptions },
    { label: '支付方式', prop: 'payMethod', type: 'select', options: payMethodOptions },
    { label: '立即支付', prop: 'payNow', type: 'select', options: payNowOptions },
    { label: '优惠金额', prop: 'discountAmount', type: 'number' },
    { label: '应付金额', prop: 'payableAmount', type: 'number' },
    { label: '实付金额', prop: 'paidAmount', type: 'number' },
    { label: '收银员', prop: 'cashierUserId', type: 'remoteSelect', remoteMethod: loadSystemUserOptions },
    { label: '备注', prop: 'remark', type: 'textarea', span: 24 }
  ],
  tableColumns: [
    { label: '订单号', prop: 'orderNo' },
    { label: '门店', prop: 'storeName' },
    { label: '会员', prop: 'memberName' },
    { label: '订单类型', prop: 'orderType', type: 'tag', options: orderTypeOptions },
    { label: '订单来源', prop: 'orderSource', type: 'tag', options: orderSourceOptions },
    { label: '订单状态', prop: 'orderStatus', type: 'tag', options: orderStatusOptions },
    { label: '支付方式', prop: 'payMethod', type: 'tag', options: payMethodOptions },
    { label: '应付金额', prop: 'payableAmount' },
    { label: '实付金额', prop: 'paidAmount' },
    { label: '创建时间', prop: 'createTime', type: 'date' }
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
    discountAmount: 0,
    payableAmount: 0,
    paidAmount: 0,
    cashierUserId: undefined,
    remark: ''
  },
  rules: {
    memberId: [{ required: true, message: '会员不能为空', trigger: 'change' }],
    planId: [{ required: true, message: '卡项方案不能为空', trigger: 'change' }]
  },
  listFn: listOrder,
  getFn: getOrder,
  addFn: addOrder,
  updateFn: updateOrder,
  delFn: delOrder
});

const handlePay = async (row: any, refresh: () => Promise<void>) => {
  try {
    const { value } = await proxy?.$modal.prompt('请输入支付方式，如 cash / wechat / transfer / card / mix')!;
    await payOrder(row.orderId, { payMethod: value || 'cash', payNow: true });
    proxy?.$modal.msgSuccess('订单支付成功');
    await refresh();
  } catch {
    // 用户取消
  }
};

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

