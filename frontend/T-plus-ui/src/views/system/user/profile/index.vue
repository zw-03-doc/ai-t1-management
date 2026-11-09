<template>
  <div class="p-2 profile">
    <el-card shadow="never">
      <header class="identity">
        <user-avatar />
        <div>
          <h2>{{ user?.nickName || user?.userName }}</h2>
          <p v-if="user">登录账号 {{ user.userName }}<span v-if="roles"> · {{ roles }}</span></p>
        </div>
      </header>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人资料" name="info">
          <user-info v-if="user" :key="revision" :user="user" class="settings" @saved="getUser" />
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="password" lazy><reset-pwd class="settings" /></el-tab-pane>
        <el-tab-pane label="登录设备" name="devices" lazy><online-device v-loading="loadingDevices" :devices="devices" @updated="loadDevices" /></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="Profile" lang="ts">
import UserAvatar from './userAvatar.vue';
import UserInfo from './userInfo.vue';
import ResetPwd from './resetPwd.vue';
import OnlineDevice from './onlineDevice.vue';
import { getUserProfile } from '@/api/system/user';
import { getOnline } from '@/api/monitor/online';
import type { UserVO } from '@/api/system/user/types';

const activeTab = ref('info');
const user = ref<UserVO>();
const roles = ref('');
const revision = ref(0);
const devices = ref([]);
const loadingDevices = ref(false);

async function getUser() {
  const { data } = await getUserProfile();
  user.value = data.user;
  roles.value = data.roleGroup;
  revision.value++;
}

async function loadDevices() {
  loadingDevices.value = true;
  try {
    const result = await getOnline();
    devices.value = result.rows || [];
  } finally {
    loadingDevices.value = false;
  }
}

watch(activeTab, tab => { if (tab === 'devices') loadDevices(); });
onMounted(getUser);
</script>

<style scoped>
.profile { max-width: 1120px; }
.identity { display: flex; align-items: center; gap: 18px; padding: 4px 0 18px; }
.identity h2 { font-size: 20px; margin: 0 0 6px; }
.identity p { margin: 0; font-size: 14px; color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
.settings { max-width: 480px; margin-top: 12px; }
</style>
