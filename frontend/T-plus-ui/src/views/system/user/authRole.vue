<template>
  <main class="role-page">
    <el-button link @click="back">返回员工账号</el-button>
    <h1>分配角色</h1>
    <section v-loading="loading" class="role-panel">
      <template v-if="account">
        <h2>{{ account.nickName || account.userName }}</h2>
        <p v-if="account.nickName !== account.userName" class="login-name">{{ account.userName }}</p>
        <el-checkbox-group v-model="selected" aria-label="账号角色" class="role-options" :disabled="protectedAccount || saving">
          <el-checkbox v-for="role in options" :key="role.roleId" :value="String(role.roleId)" :disabled="!canSelect(role)">
            {{ role.roleName }}<span v-if="role.status !== '0'">（已停用）</span>
          </el-checkbox>
        </el-checkbox-group>
        <p v-if="protectedAccount" class="role-note">该账号的角色受保护。</p>
        <p v-else-if="!options.length" class="role-note">没有可分配的角色。</p>
        <footer>
          <el-button v-if="!protectedAccount" type="primary" :disabled="!changed || !selected.length" :loading="saving" @click="save"
            >保存角色</el-button
          >
        </footer>
      </template>
      <el-skeleton v-else-if="loading" :rows="3" />
      <el-empty v-else description="账号未能加载"><el-button @click="load(String(route.params.userId))">重新加载</el-button></el-empty>
    </section>
  </main>
</template>

<script setup name="AuthRole" lang="ts">
import type { RoleVO } from '@/api/system/role/types';
import { getAuthRole, updateAuthRole } from '@/api/system/user';
import type { UserVO } from '@/api/system/user/types';
import { useUserStore } from '@/store/modules/user';
import { checkPermi } from '@/utils/permission';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const account = ref<UserVO>();
const options = ref<RoleVO[]>([]);
const editableIds = ref(new Set<string>());
const selected = ref<string[]>([]);
const initial = ref<string[]>([]);
const changed = computed(() => selected.value.length !== initial.value.length || selected.value.some((id) => !initial.value.includes(id)));
const protectedAccount = computed(() => {
  const id = String(account.value?.userId);
  return id === '1' || id === String(userStore.userId) || !checkPermi(['system:user:edit']);
});
const loading = ref(false);
const saving = ref(false);
let request = 0;

function canSelect(role: RoleVO) {
  const id = String(role.roleId);
  return editableIds.value.has(id) && (account.value?.status === '0' || initial.value.includes(id));
}

function back() {
  router.push('/system/user');
}

async function load(userId: string) {
  const current = ++request;
  account.value = undefined;
  options.value = [];
  selected.value = [];
  loading.value = true;
  try {
    const { data } = await getAuthRole(userId);
    if (current !== request) return;
    account.value = data.user;
    const offered = data.roles || [];
    const assigned = data.user.roles || [];
    const byId = new Map(offered.map((role) => [String(role.roleId), role]));
    // Include existing assignments outside the operator's selectable roles.
    for (const role of assigned) {
      if (!byId.has(String(role.roleId))) byId.set(String(role.roleId), role);
    }
    selected.value = [...new Set([...assigned, ...offered.filter((role) => role.flag)].map((role) => String(role.roleId)))];
    initial.value = [...selected.value];
    options.value = [...byId.values()].filter((role) => role.status === '0' || selected.value.includes(String(role.roleId)));
    editableIds.value = new Set(offered.filter((role) => role.status === '0').map((role) => String(role.roleId)));
  } catch {
    // The empty state offers a retry after the request interceptor reports the error.
  } finally {
    if (current === request) loading.value = false;
  }
}

async function save() {
  if (!account.value || protectedAccount.value || saving.value || !changed.value || !selected.value.length) return;
  saving.value = true;
  try {
    const userId = String(account.value.userId);
    await updateAuthRole({ userId, roleIds: selected.value.join(','), originalRoleIds: initial.value.join(',') });
    ElMessage.success('账号角色已更新');
    if (String(route.params.userId) === userId) back();
  } finally {
    saving.value = false;
  }
}

watch(
  () => route.params.userId,
  (id) => {
    if (typeof id === 'string' && id) load(id);
  },
  { immediate: true }
);
</script>

<style scoped>
.role-page {
  padding: clamp(16px, 2.5vw, 32px);
}
h1 {
  margin: 14px 0 20px;
  font-size: 22px;
  font-weight: 600;
}
.role-panel {
  max-width: 600px;
  padding: 24px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}
h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.login-name,
.role-note {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.role-options {
  display: grid;
  margin: 18px 0;
}
footer {
  display: flex;
  gap: 8px;
  margin-top: 24px;
}
footer .el-button + .el-button {
  margin-left: 0;
}
</style>
