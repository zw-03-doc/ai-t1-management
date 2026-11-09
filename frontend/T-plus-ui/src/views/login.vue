<template>
  <main class="login">
    <section class="login-panel" aria-labelledby="login-title">
      <a class="brand-link" href="/site/">
        <img :src="brandLogo" width="28" height="28" alt="" />
        T1 健身空间
      </a>
      <h1 id="login-title">门店管理登录</h1>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" hide-required-asterisk
        class="login-form" @submit.prevent="handleLogin">
        <el-form-item v-if="tenantEnabled && tenantList.length > 1" prop="tenantId" label="所属机构">
          <el-select v-model="loginForm.tenantId" size="large" filterable :placeholder="t('login.selectPlaceholder')">
            <el-option v-for="item in tenantList" :key="item.tenantId" :label="item.companyName" :value="item.tenantId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="username" :label="t('login.username')">
          <el-input v-model="loginForm.username" name="username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password" :label="t('login.password')">
          <el-input v-model="loginForm.password" name="password" type="password" show-password size="large" autocomplete="current-password" />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code" :label="t('login.code')">
          <div class="captcha-row">
            <el-input v-model="loginForm.code" name="code" size="large" :aria-label="t('login.code')" autocomplete="off" />
            <button class="captcha-image" type="button" aria-label="更换验证码" title="更换验证码" @click="getCode">
              <img v-if="codeUrl" :src="codeUrl" alt="图形验证码" />
              <span v-else>获取验证码</span>
            </button>
          </div>
        </el-form-item>
        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">{{ t('login.rememberPassword') }}</el-checkbox>
          <button class="social-login" type="button" @click="doSocialLogin('wechat')">{{ t('login.social.wechat') }}</button>
        </div>
        <el-button class="submit" type="primary" native-type="submit" size="large" :loading="loading">
          {{ loading ? t('login.logging') : t('login.login') }}
        </el-button>
        <router-link v-if="register" class="register-link" to="/register">{{ t('login.switchRegisterPage') }}</router-link>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import brandLogo from '@/assets/logo/logo.png';
import { getCodeImg, getTenantList } from '@/api/login';
import { authRouterUrl } from '@/api/system/social/auth';
import { useUserStore } from '@/store/modules/user';
import { LoginData, TenantVO } from '@/api/types';
import { to } from 'await-to-js';
import { HttpStatus } from '@/enums/RespEnum';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const userStore = useUserStore();
const router = useRouter();
const { t } = useI18n();

const loginForm = ref<LoginData>({
  tenantId: '000000',
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
} as LoginData);

const loginRules: ElFormRules = {
  tenantId: [{ required: true, trigger: 'blur', message: t('login.rule.tenantId.required') }],
  username: [{ required: true, trigger: 'blur', message: t('login.rule.username.required') }],
  password: [{ required: true, trigger: 'blur', message: t('login.rule.password.required') }],
  code: [{ required: true, trigger: 'change', message: t('login.rule.code.required') }]
};

const codeUrl = ref('');
const loading = ref(false);
// 验证码开关
const captchaEnabled = ref(true);
// 租户开关
const tenantEnabled = ref(true);

// 注册开关
const register = ref(false);
const redirect = ref('/');
const loginRef = ref<ElFormInstance>();
// 租户列表
const tenantList = ref<TenantVO[]>([]);

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    redirect.value = newRoute.query && newRoute.query.redirect && decodeURIComponent(newRoute.query.redirect);
  },
  { immediate: true }
);

const handleLogin = () => {
  if (loading.value) return;
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      // 勾选了需要记住密码设置在 localStorage 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        localStorage.setItem('tenantId', String(loginForm.value.tenantId));
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('password', String(loginForm.value.password));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        // 否则移除
        localStorage.removeItem('tenantId');
        localStorage.removeItem('username');
        localStorage.removeItem('password');
        localStorage.removeItem('rememberMe');
      }
      // 调用action的登录方法
      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        const redirectUrl = redirect.value || '/';
        await router.push(redirectUrl);
        loading.value = false;
      } else {
        loading.value = false;
        // 重新获取验证码
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      console.log('error submit!', fields);
    }
  });
};

/**
 * 获取验证码
 */
const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    // 刷新验证码时清空输入框
    loginForm.value.code = '';
    codeUrl.value = 'data:image/gif;base64,' + data.img;
    loginForm.value.uuid = data.uuid;
  }
};

const getLoginData = () => {
  const tenantId = localStorage.getItem('tenantId');
  const username = localStorage.getItem('username');
  const password = localStorage.getItem('password');
  const rememberMe = localStorage.getItem('rememberMe');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? String(loginForm.value.username) : username,
    password: password === null ? String(loginForm.value.password) : String(password),
    rememberMe: rememberMe === null ? false : Boolean(rememberMe)
  } as LoginData;
};

/**
 * 获取租户列表
 */
const initTenantList = async () => {
  const { data } = await getTenantList(false);
  tenantEnabled.value = data.tenantEnabled === undefined ? true : data.tenantEnabled;
  if (tenantEnabled.value) {
    tenantList.value = data.voList ?? [];
    if (tenantList.value.length && !tenantList.value.some((tenant) => tenant.tenantId === loginForm.value.tenantId)) {
      loginForm.value.tenantId = tenantList.value[0].tenantId;
    }
  }
};

/**
 * 第三方登录
 * @param type
 */
const doSocialLogin = (type: string) => {
  authRouterUrl(type, loginForm.value.tenantId).then((res: any) => {
    if (res.code === HttpStatus.SUCCESS) {
      // 获取授权地址跳转
      window.location.href = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

onMounted(() => {
  getCode();
  initTenantList();
  getLoginData();
});
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px 16px;
  background: var(--el-bg-color-page);
}
.login-panel {
  width: min(100%, 384px);
  padding: clamp(24px, 4vw, 32px);
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
}
.brand-link img { border-radius: 6px; object-fit: cover; }
h1 { margin: 20px 0 24px; font-size: 25px; line-height: 1.25; letter-spacing: -.04em; }
.login-form :deep(.el-form-item) { margin-bottom: 20px; }
.login-form :deep(.el-form-item__label) { margin: 0 0 6px; font-size: 14px; line-height: 20px; }
.login-form :deep(.el-input), .login-form :deep(.el-select__wrapper) { height: 42px; }
.login-form :deep(.el-input__wrapper) { border-radius: 7px; }
.captcha-row { display: grid; grid-template-columns: minmax(0,1fr) 106px; gap: 10px; width: 100%; }
.captcha-image {
  display: grid; place-items: center; height: 42px; min-width: 0; padding: 0 3px;
  border: 1px solid var(--el-border-color); border-radius: 7px; background: white; color: #40423f; cursor: pointer;
}
.captcha-image img { display: block; max-width: 100%; max-height: 38px; object-fit: contain; }
.login-options { display: flex; justify-content: space-between; align-items: center; margin: 0 0 12px; }
.social-login { border: 0; padding: 6px 0 6px 8px; color: var(--el-text-color-regular); background: transparent; cursor: pointer; font: inherit; font-size: 14px; }
.submit { width: 100%; min-height: 42px; }
.register-link { display: inline-block; margin-top: 12px; }
a:focus-visible, button:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; }
@media (max-height: 580px) { .login { padding-block: 12px; } }
</style>
