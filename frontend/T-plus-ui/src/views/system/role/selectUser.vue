<template>
  <el-row>
    <el-dialog v-model="visible" title="选择员工" width="min(800px, 94vw)" top="5vh" append-to-body>
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="登录账号" prop="userName">
          <el-input v-model="queryParams.userName" placeholder="登录账号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phonenumber">
          <el-input v-model="queryParams.phonenumber" placeholder="手机号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-row>
        <el-table ref="tableRef" border :data="userList" height="260px" @row-click="clickRow" @selection-change="handleSelectionChange">
          <el-table-column :selectable="selectable" type="selection" width="55"></el-table-column>
          <el-table-column label="登录账号" prop="userName" :show-overflow-tooltip="true" />
          <el-table-column label="员工姓名" prop="nickName" :show-overflow-tooltip="true" />
          <el-table-column label="手机" prop="phonenumber" :show-overflow-tooltip="true" />
          <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
              <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
          </el-table-column>
        </el-table>
        <pagination v-if="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :disabled="!userIds.length" @click="handleSelectUser">添加</el-button>
          <el-button @click="visible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </el-row>
</template>

<script setup name="SelectUser" lang="ts">
import { authUserSelectAll, unallocatedUserList } from '@/api/system/role';
import { UserVO } from '@/api/system/user/types';
import { UserQuery } from '@/api/system/user/types';

import { useUserStore } from '@/store/modules/user';
const user = useUserStore();
const selectable = (row: UserVO) => row.status === '0' && String(row.userId) !== '1' && String(row.userId) !== String(user.userId);
const props = defineProps({
  roleId: {
    type: [Number, String],
    required: true
  }
});

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const userList = ref<UserVO[]>([]);
const visible = ref(false);
const total = ref(0);
const userIds = ref<Array<string | number>>([]);

const queryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phonenumber: undefined
});

const tableRef = ref<ElTableInstance>();
const queryFormRef = ref<ElFormInstance>();

const show = () => {
  queryParams.roleId = props.roleId;
  queryParams.pageNum = 1;
  queryParams.userName = undefined;
  queryParams.phonenumber = undefined;
  userIds.value = [];
  userList.value = [];
  getList();
  visible.value = true;
};

/**
 * 选择行
 */
const clickRow = (row: any) => {
  if (selectable(row)) tableRef.value?.toggleRowSelection(row);
};
/** 多选框选中数据 */
const handleSelectionChange = (selection: UserVO[]) => {
  userIds.value = selection.map((item: UserVO) => item.userId);
};

/** 查询数据 */
const getList = async () => {
  const res = await unallocatedUserList(queryParams);
  userList.value = res.rows;
  total.value = res.total;
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  getList();
};

const emit = defineEmits(['ok']);
/**选择授权用户操作 */
const handleSelectUser = async () => {
  const roleId = queryParams.roleId;
  const ids = userIds.value.join(',');
  if (ids == '') {
    proxy?.$modal.msgError('请选择员工');
    return;
  }
  await authUserSelectAll({ roleId, userIds: ids });
  proxy?.$modal.msgSuccess('分配成功');
  emit('ok');
  visible.value = false;
};
// 暴露
defineExpose({
  show
});
</script>
