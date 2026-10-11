<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
</template>

<script setup name="FitnessOrderItem" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { getOrderItem, listOrderItem } from '@/api/fitness/orderItem';
import { loadMemberOptions, loadStoreOptions } from '../components/remoteOptions';

const noop = async () => ({ code: 200, data: {} });

const itemTypeOptions = [{ label: '卡项方案', value: 'card_plan' }];
const orderStatusOptions = [
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已取消', value: 'cancelled' },
  { label: '已退款', value: 'refunded' },
  { label: '已关闭', value: 'closed' }
];

const pageConfig = reactive({
  title: '订单明细',
  permPrefix: 'fitness:orderItem',
  idKey: 'itemId',
  exportUrl: 'fitness/orderItem/export',
  allowCreate: false,
  allowEdit: false,
  allowDelete: false,
  queryFields: [
    { label: '订单号', prop: 'orderNo' },
    { label: '门店', prop: 'storeId', type: 'remoteSelect', remoteMethod: loadStoreOptions },
    { label: '会员', prop: 'memberId', type: 'remoteSelect', remoteMethod: loadMemberOptions },
    { label: '明细类型', prop: 'itemType', type: 'select', options: itemTypeOptions },
    { label: '明细名称', prop: 'itemName' },
    { label: '下单开始', prop: 'orderCreateTimeStart', type: 'date' },
    { label: '下单结束', prop: 'orderCreateTimeEnd', type: 'date' }
  ],
  formFields: [],
  tableColumns: [
    { label: '明细编号', prop: 'itemNo' },
    { label: '订单号', prop: 'orderNo' },
    { label: '订单状态', prop: 'orderStatus', type: 'tag', options: orderStatusOptions },
    { label: '门店', prop: 'storeName' },
    { label: '会员', prop: 'memberName' },
    { label: '手机号', prop: 'memberMobile' },
    { label: '明细类型', prop: 'itemType', type: 'tag', options: itemTypeOptions },
    { label: '明细名称', prop: 'itemName' },
    { label: '数量', prop: 'quantity' },
    { label: '单价', prop: 'unitPrice' },
    { label: '小计金额', prop: 'totalPrice' },
    { label: '优惠金额', prop: 'discountAmount' },
    { label: '实付金额', prop: 'realAmount' },
    { label: '发放会员卡', prop: 'grantCardId' },
    { label: '创建时间', prop: 'createTime', type: 'date' }
  ],
  initialQuery: {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    storeId: undefined,
    memberId: undefined,
    itemType: '',
    itemName: '',
    orderCreateTimeStart: '',
    orderCreateTimeEnd: ''
  },
  initialForm: {},
  listFn: listOrderItem,
  getFn: getOrderItem,
  addFn: noop,
  updateFn: noop,
  delFn: noop
});
</script>
