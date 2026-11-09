<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" @submit.prevent="save">
    <el-form-item label="显示名称" prop="nickName"><el-input v-model.trim="form.nickName" maxlength="30" /></el-form-item>
    <el-form-item label="手机号" prop="phonenumber"><el-input v-model.trim="form.phonenumber" maxlength="11" type="tel" /></el-form-item>
    <el-form-item label="邮箱" prop="email"><el-input v-model.trim="form.email" maxlength="50" type="email" /></el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="form.sex"><el-radio value="2">未设置</el-radio><el-radio value="0">男</el-radio><el-radio value="1">女</el-radio></el-radio-group>
    </el-form-item>
    <el-form-item><el-button type="primary" native-type="submit" :loading="saving">保存</el-button></el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { updateUserProfile } from '@/api/system/user';
import type { UserVO } from '@/api/system/user/types';
import { useUserStore } from '@/store/modules/user';

const props = defineProps<{ user: UserVO }>();
const emit = defineEmits<{ saved: [] }>();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const formRef = ref<ElFormInstance>();
const saving = ref(false);
const form = reactive({ nickName: props.user.nickName, phonenumber: props.user.phonenumber || '', email: props.user.email || '', sex: props.user.sex || '2' });
const rules: ElFormRules = {
  nickName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式有误', trigger: 'blur' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号须为 11 位', trigger: 'blur' }]
};

async function save() {
  if (saving.value || !await formRef.value?.validate()) return;
  saving.value = true;
  try {
    await updateUserProfile({ ...form, phonenumber: form.phonenumber || undefined });
    await useUserStore().getInfo();
    emit('saved');
    proxy?.$modal.msgSuccess('资料已保存');
  } finally {
    saving.value = false;
  }
}
</script>
