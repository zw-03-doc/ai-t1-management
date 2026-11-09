<template>
  <div class="p-2">
    <el-card shadow="never">
      <el-form inline @submit.prevent="search">
        <el-form-item label="角色名称"><el-input v-model="query.roleName" clearable @clear="search" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 108px" @change="search">
            <el-option label="启用" value="0" /><el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button native-type="submit">查询</el-button></el-form-item>
        <el-form-item><el-button v-hasPermi="['system:role:add']" type="primary" @click="openEditor()">新增角色</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="roles" row-key="roleId" empty-text="没有匹配的角色">
        <el-table-column prop="roleName" label="角色名称" min-width="135" />
        <el-table-column label="数据范围" min-width="138"><template #default="{ row }">{{ scopeName(row.dataScope) }}</template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{ row }">{{ row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
        <el-table-column label="操作" min-width="175">
          <template #default="{ row }">
            <template v-if="String(row.roleId) !== '1'">
              <el-button v-hasPermi="['system:role:edit']" link type="primary" @click="openEditor(row)">编辑</el-button>
              <el-button v-hasPermi="['system:role:edit']" link type="primary" @click="openMembers(row)">角色成员</el-button>
              <el-button v-hasPermi="['system:role:remove']" link type="danger" @click="remove(row)">删除</el-button>
            </template>
            <span v-else>系统内置</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > query.pageSize" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadRoles" />
    </el-card>

    <el-dialog v-model="open" :title="form.roleId ? '编辑角色' : '新增角色'" width="min(550px, 94vw)" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model.trim="form.roleName" maxlength="30" /></el-form-item>
        <el-form-item label="角色标识" prop="roleKey"><el-input v-model.trim="form.roleKey" :disabled="!!form.roleId" maxlength="100" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-if="!form.roleId && String(user.userId) === '1'" v-model="form.dataScope">
            <el-option label="本人记录" value="5" /><el-option label="全部记录" value="1" />
          </el-select>
          <span v-else>{{ scopeName(form.dataScope) }}</span>
        </el-form-item>
        <el-form-item label="功能权限">
          <el-tree ref="tree" class="role-tree" :data="menus" node-key="id" check-strictly show-checkbox
            :props="{ label: 'label', children: 'children' }" :default-checked-keys="original" empty-text="暂无可分配的权限" @check="onCheck" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Role">
import { addRole, delRole, listRole, projectMenus, updateRole } from '@/api/system/role';
import type { MenuTreeOption } from '@/api/system/menu/types';
import type { RoleVO } from '@/api/system/role/types';
import { useUserStore } from '@/store/modules/user';

const user = useUserStore();
const router = useRouter();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const query = reactive({ pageNum: 1, pageSize: 10, roleName: '', roleKey: '', status: '' });
const roles = ref<RoleVO[]>([]);
const total = ref(0);
const loading = ref(false);
const open = ref(false);
const saving = ref(false);
const tree = ref<ElTreeInstance>();
const formRef = ref<ElFormInstance>();
const menus = ref<MenuTreeOption[]>([]);
const original = ref<(string | number)[]>([]);
const emptyForm = () => ({ roleId: undefined as string | number | undefined, roleName: '', roleKey: '', roleSort: 1, status: '0', dataScope: '5', remark: '' });
const form = ref(emptyForm());
const rules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleKey: [{ required: true, message: '请输入角色标识' }]
};
const scopes: Record<string, string> = { '1': '全部记录', '2': '指定组织', '3': '所属组织', '4': '所属组织及下级', '5': '本人记录', '6': '所属组织、下级及本人' };
const scopeName = (scope: string) => scopes[scope] || '沿用已有范围';

async function loadRoles() {
  loading.value = true;
  try {
    const result = await listRole(query);
    roles.value = result.rows;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  loadRoles();
}

async function openEditor(row?: RoleVO) {
  const { data } = await projectMenus(row?.roleId);
  form.value = row ? { roleId: row.roleId, roleName: row.roleName, roleKey: row.roleKey, roleSort: row.roleSort,
    status: row.status, dataScope: row.dataScope, remark: row.remark || '' } : emptyForm();
  menus.value = data.menus;
  original.value = [...data.checkedKeys];
  open.value = true;
  await nextTick();
  tree.value?.setCheckedKeys(original.value);
}

function onCheck(menu: MenuTreeOption) {
  const node = tree.value?.getNode(menu.id);
  if (!node) return;
  if (node.checked) {
    let parent = node.parent;
    while (parent?.level) {
      tree.value?.setChecked(parent.key, true, false);
      parent = parent.parent;
    }
  } else {
    const clearChildren = (children: MenuTreeOption[] = []) => children.forEach(child => {
      tree.value?.setChecked(child.id, false, false);
      clearChildren(child.children);
    });
    clearChildren(menu.children);
  }
}

async function save() {
  if (saving.value || !tree.value || !await formRef.value?.validate()) return;
  saving.value = true;
  try {
    const data = { ...form.value, menuIds: tree.value.getCheckedKeys(), originalMenuIds: original.value };
    await (form.value.roleId ? updateRole(data) : addRole(data));
    open.value = false;
    proxy?.$modal.msgSuccess('角色已保存');
    await loadRoles();
  } finally {
    saving.value = false;
  }
}

function openMembers(row: RoleVO) {
  router.push({ path: '/system/role-auth/user/' + row.roleId, query: { roleName: row.roleName } });
}

async function remove(row: RoleVO) {
  await proxy?.$modal.confirm('删除角色“' + row.roleName + '”？');
  await delRole(row.roleId);
  if (roles.value.length === 1 && query.pageNum > 1) query.pageNum--;
  await loadRoles();
  proxy?.$modal.msgSuccess('角色已删除');
}

onMounted(loadRoles);
</script>

<style scoped>
.role-tree { width: 100%; max-height: 300px; overflow: auto; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 6px; }
</style>
