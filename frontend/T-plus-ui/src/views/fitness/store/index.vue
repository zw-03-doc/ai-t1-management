<template>
  <FitnessCrudPage v-bind="pageConfig as any" />
</template>

<script setup name="FitnessStore" lang="ts">
import FitnessCrudPage from '../components/FitnessCrudPage.vue';
import { addStore, delStore, getStore, listStore, updateStore } from '@/api/fitness/store';

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
