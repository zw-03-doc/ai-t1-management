<template>
  <div class="p-2">
    <h2 class="text-lg font-semibold mb-3">{{ roleName ? roleName + ' · ' : '' }}角色成员</h2>
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="search">
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
      </div>
    </transition>
    <el-card shadow="never">
      <template #header>
        <el-row :gutter="10">
          <el-col :span="1.5">
            <el-button v-hasPermi="['system:role:edit']" type="primary" plain icon="Plus" @click="openSelectUser">添加员工</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['system:role:edit']" type="danger" plain icon="CircleClose" :disabled="multiple" @click="cancelAuthUserAll">
              移除选中成员
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button plain icon="Back" @click="handleClose">返回角色列表</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" :search="true" @query-table="getList"></right-toolbar>
        </el-row>
      </template>
      <el-table v-loading="loading" border :data="userList" @selection-change="handleSelectionChange">
        <el-table-column :selectable="editable" type="selection" width="55" align="center" />
        <el-table-column label="登录账号" prop="userName" :show-overflow-tooltip="true" />
        <el-table-column label="员工姓名" prop="nickName" :show-overflow-tooltip="true" />
        <el-table-column label="手机" prop="phonenumber" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button v-if="editable(scope.row)" v-hasPermi="['system:role:edit']" link type="danger" @click="cancelAuthUser(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      <select-user ref="selectRef" :role-id="queryParams.roleId" @ok="handleQuery" />
    </el-card>
  </div>
</template>

<script setup name="AuthUser" lang="ts">
import { allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/system/role';
import { UserQuery } from '@/api/system/user/types';
import { UserVO } from '@/api/system/user/types';
import SelectUser from './selectUser.vue';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const user = useUserStore();
const roleName = computed(() => String(route.query.roleName || ''));
const editable = (row: UserVO) => String(row.userId) !== '1' && String(row.userId) !== String(user.userId);
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const userList = ref<UserVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const multiple = ref(true);
const total = ref(0);
const userIds = ref<Array<string | number>>([]);

const queryFormRef = ref<ElFormInstance>();
const selectRef = ref<InstanceType<typeof SelectUser>>();

const queryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  roleId: route.params.roleId as string,
  userName: undefined,
  phonenumber: undefined
});

/** 查询授权用户列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res = await allocatedUserList(queryParams);
    userList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};
// 返回按钮
const handleClose = () => router.push('/system/role');
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};
// 多选框选中数据
const handleSelectionChange = (selection: UserVO[]) => {
  userIds.value = selection.map((item) => item.userId);
  multiple.value = !selection.length;
};
/** 打开授权用户表弹窗 */
const openSelectUser = () => {
  selectRef.value?.show();
};
/** 取消授权按钮操作 */
const cancelAuthUser = async (row: UserVO) => {
  await proxy?.$modal.confirm('确定从该角色移除员工“' + row.userName + '”？');
  await authUserCancel({ userId: row.userId, roleId: queryParams.roleId });
  await getList();
  proxy?.$modal.msgSuccess('已移除');
};
/** 移除选中成员按钮操作 */
const cancelAuthUserAll = async () => {
  const roleId = queryParams.roleId;
  const uIds = userIds.value.join(',');
  await proxy?.$modal.confirm('确定移除选中的角色成员？');
  await authUserCancelAll({ roleId: roleId, userIds: uIds });
  await getList();
  proxy?.$modal.msgSuccess('已移除');
};

watch(() => route.params.roleId, (id) => {
  if (!id) return;
  queryParams.roleId = String(id);
  handleQuery();
}, { immediate: true });
</script>
