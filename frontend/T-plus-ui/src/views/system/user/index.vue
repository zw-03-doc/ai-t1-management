<template>
  <main class="accounts-page">
    <header class="accounts-header">
      <h1>员工账号</h1>
      <el-button v-if="canQuery && checkPermi(['system:user:add'])" type="primary" @click="openEditor()">新增账号</el-button>
    </header>

    <section class="accounts-panel">
      <form class="accounts-filters" aria-label="筛选员工账号" @submit.prevent="search">
        <el-select v-model="searchBy" aria-label="查询字段" class="filter-field">
          <el-option v-for="(label, field) in searchFields" :key="field" :label="label" :value="field" />
        </el-select>
        <el-input
          v-model="keyword"
          :placeholder="'搜索' + searchFields[searchBy]"
          :aria-label="searchFields[searchBy]"
          clearable
          class="filter-keyword"
          @clear="search"
        />
        <el-select v-model="status" aria-label="账号状态" placeholder="全部状态" class="filter-status" @change="search">
          <el-option label="全部状态" value="" />
          <el-option label="已启用" value="0" />
          <el-option label="已停用" value="1" />
        </el-select>
        <el-button native-type="submit">查询</el-button>
      </form>

      <el-alert v-if="loadFailed" title="账号列表加载失败" type="error" :closable="false">
        <el-button link @click="loadAccounts">重新加载</el-button>
      </el-alert>
      <el-table v-else v-loading="loading" :data="accounts" row-key="userId" empty-text="没有符合条件的账号">
        <el-table-column label="登录账号" prop="userName" min-width="130" show-overflow-tooltip />
        <el-table-column label="姓名" prop="nickName" min-width="130" show-overflow-tooltip />
        <el-table-column label="手机号" min-width="135">
          <template #default="{ row }">{{ row.phonenumber || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <span class="account-status" :class="{ inactive: row.status !== '0' }">{{ row.status === '0' ? '已启用' : '已停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="155" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isSelf(row)" link @click="router.push('/user/profile')">个人资料</el-button>
            <div v-else-if="String(row.userId) !== '1' && (canEdit || canReset || canRemove)" class="account-actions">
              <el-button v-if="canQuery && canEdit" link type="primary" @click="openEditor(row)">编辑</el-button>
              <el-dropdown trigger="click">
                <el-button link aria-label="更多账号操作">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canQuery && canEdit" @click="router.push('/system/user-auth/role/' + row.userId)"
                      >分配角色</el-dropdown-item
                    >
                    <el-dropdown-item v-if="canReset" @click="resetPassword(row)">重置密码</el-dropdown-item>
                    <el-dropdown-item v-if="canEdit" @click="changeStatus(row)">{{ row.status === '0' ? '停用账号' : '启用账号' }}</el-dropdown-item>
                    <el-dropdown-item v-if="canRemove" @click="removeAccount(row)">删除账号</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-if="!loadFailed && total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadAccounts"
      />
    </section>

    <el-dialog
      v-model="editorOpen"
      :title="editing ? '编辑员工账号' : '新增员工账号'"
      width="min(440px, 94vw)"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      @closed="clearEditor"
    >
      <el-form v-if="editorReady" ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save">
        <el-form-item label="登录账号" prop="userName">
          <el-input v-model.trim="form.userName" :disabled="editing" maxlength="30" autocomplete="off" />
        </el-form-item>
        <el-form-item v-if="!editing" label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" autocomplete="new-password" show-password maxlength="20" placeholder="5 至 20 个字符" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickName">
          <el-input v-model.trim="form.nickName" maxlength="30" />
        </el-form-item>
        <el-form-item label="手机号" prop="phonenumber">
          <el-input v-model.trim="form.phonenumber" inputmode="tel" maxlength="11" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="!editing" label="账号角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" no-data-text="没有可分配的角色">
            <el-option v-for="role in roles" :key="role.roleId" :label="role.roleName" :value="String(role.roleId)" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :rows="4" animated />
      <template #footer>
        <el-button :disabled="saving" @click="editorOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!editorReady" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup name="User" lang="ts">
import type { FormRules } from 'element-plus';
import * as api from '@/api/system/user';
import type { UserForm, UserQuery, UserVO } from '@/api/system/user/types';
import type { RoleVO } from '@/api/system/role/types';
import { useUserStore } from '@/store/modules/user';
import { checkPermi } from '@/utils/permission';

const router = useRouter();
const userStore = useUserStore();
const canQuery = computed(() => checkPermi(['system:user:query']));
const canEdit = computed(() => checkPermi(['system:user:edit']));
const canReset = computed(() => checkPermi(['system:user:resetPwd']));
const canRemove = computed(() => checkPermi(['system:user:remove']));
const isSelf = (row: UserVO) => String(row.userId) === String(userStore.userId);

const searchFields = { userName: '登录账号', nickName: '姓名', phonenumber: '手机号' };
const searchBy = ref<keyof typeof searchFields>('userName');
const keyword = ref('');
const status = ref('');
const query = reactive<UserQuery>({ pageNum: 1, pageSize: 10 });
const accounts = ref<UserVO[]>([]);
const total = ref(0);
const loading = ref(false);
const loadFailed = ref(false);
let listRequest = 0;

async function loadAccounts() {
  const request = ++listRequest;
  loading.value = true;
  loadFailed.value = false;
  try {
    const response = await api.listUser({ ...query });
    if (request === listRequest) {
      accounts.value = response.rows;
      total.value = response.total;
    }
  } catch {
    if (request === listRequest) loadFailed.value = true;
  } finally {
    if (request === listRequest) loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  query.userName = query.nickName = query.phonenumber = undefined;
  query[searchBy.value] = keyword.value.trim() || undefined;
  query.status = status.value || undefined;
  loadAccounts();
}

const editorOpen = ref(false);
const editorReady = ref(false);
const editing = ref(false);
const saving = ref(false);
const roles = ref<RoleVO[]>([]);
const formRef = ref<ElFormInstance>();
const emptyForm = (): UserForm => ({ userName: '', nickName: '', password: '', phonenumber: '', roleIds: [], postIds: undefined, status: '0' });
const form = ref<UserForm>(emptyForm());
const passwordPattern = /^(?!\s*$)[^<>"'|\\\r\n]{5,20}$/;
const passwordError = '请填写 5 至 20 位密码，避开尖括号、引号、竖线和反斜杠';
const rules: FormRules = {
  userName: [
    { required: true, message: '请填写登录账号', trigger: 'blur' },
    { min: 2, max: 30, message: '登录账号需为 2 至 30 个字符', trigger: 'blur' }
  ],
  nickName: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请填写登录密码', trigger: 'blur' },
    { pattern: passwordPattern, message: passwordError, trigger: 'blur' }
  ],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '请填写 11 位手机号', trigger: 'blur' }],
  roleIds: [{ type: 'array', required: true, min: 1, message: '请选择账号角色', trigger: 'change' }]
};
let editorRequest = 0;

function clearEditor() {
  editorRequest++;
  editorReady.value = false;
  form.value = emptyForm();
  roles.value = [];
}

async function openEditor(row?: UserVO) {
  clearEditor();
  const request = editorRequest;
  editing.value = !!row;
  editorOpen.value = true;
  try {
    const [{ data }, creator] = await Promise.all([
      api.getUser(row?.userId),
      !row && String(userStore.userId) !== '1' ? api.getUserProfile() : undefined
    ]);
    if (request !== editorRequest || !editorOpen.value) return;
    if (row) {
      // Profile edits must not replace roles, posts, department, status or password.
      form.value = {
        ...emptyForm(),
        userId: String(data.user.userId),
        userName: data.user.userName,
        nickName: data.user.nickName,
        phonenumber: data.user.phonenumber || '',
        password: undefined,
        status: undefined,
        roleIds: undefined
      };
    } else {
      // A delegated manager can see accounts created within their own department scope.
      form.value.deptId = creator?.data.user.deptId;
      roles.value = data.roles.filter((role) => role.status === '0');
    }
    editorReady.value = true;
  } catch {
    if (request === editorRequest) editorOpen.value = false;
  }
}

async function save() {
  if (!editorReady.value || saving.value) return;
  saving.value = true;
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return;
    if (editing.value) await api.updateUser(form.value);
    else await api.addUser(form.value);
    editorOpen.value = false;
    ElMessage.success(editing.value ? '员工账号已更新' : '员工账号已新增');
    await loadAccounts();
  } finally {
    saving.value = false;
  }
}

async function resetPassword(row: UserVO) {
  const result = await ElMessageBox.prompt('为 ' + row.userName + ' 设置新的登录密码', '重置密码', {
    inputType: 'password',
    inputPattern: passwordPattern,
    inputErrorMessage: passwordError,
    confirmButtonText: '重置密码',
    cancelButtonText: '取消',
    closeOnClickModal: false
  }).catch(() => null);
  if (!result) return;
  await api.resetUserPwd(row.userId, result.value);
  ElMessage.success('密码已重置');
}

async function changeStatus(row: UserVO) {
  const enable = row.status !== '0';
  const action = enable ? '启用' : '停用';
  const confirmed = await ElMessageBox.confirm('确认' + action + '账号 ' + row.userName + '？', action + '账号', {
    confirmButtonText: action,
    cancelButtonText: '取消'
  }).catch(() => false);
  if (!confirmed) return;
  await api.changeUserStatus(row.userId, enable ? '0' : '1');
  ElMessage.success('账号已' + action);
  await loadAccounts();
}

async function removeAccount(row: UserVO) {
  const confirmed = await ElMessageBox.confirm('确认删除账号 ' + row.userName + '？', '删除员工账号', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => false);
  if (!confirmed) return;
  await api.delUser(row.userId);
  if (accounts.value.length === 1 && query.pageNum > 1) query.pageNum--;
  ElMessage.success('账号已删除');
  await loadAccounts();
}

onMounted(loadAccounts);
</script>

<style scoped>
.accounts-page {
  padding: clamp(14px, 2.5vw, 32px);
}
.accounts-header,
.accounts-filters,
.account-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.accounts-header {
  justify-content: space-between;
  margin-bottom: 20px;
}
.accounts-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}
.accounts-panel {
  padding: 18px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}
.accounts-filters {
  flex-wrap: wrap;
  margin-bottom: 18px;
  gap: 8px;
}
.filter-field {
  width: 108px;
}
.filter-keyword {
  width: min(235px, 100%);
}
.filter-status {
  width: 120px;
}
.account-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}
.account-status::before {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-success);
  content: '';
}
.account-status.inactive::before {
  background: var(--el-text-color-placeholder);
}
@media (max-width: 640px) {
  .accounts-panel {
    padding: 12px;
  }
  .filter-keyword {
    width: auto;
    flex: 1 1 140px;
  }
}
</style>
