<template>
  <main class="access-page">
    <section>
      <a class="brand" href="/site/">T1 健身空间</a>
      <h1>{{ forbidden ? '没有访问权限' : '页面不存在' }}</h1>
      <p v-if="forbidden">如需办理这项业务，请联系管理员开通权限。</p>
      <router-link class="return-link" to="/index">返回工作台</router-link>
    </section>
  </main>
</template>

<script setup lang="ts">
defineProps<{ forbidden?: boolean }>();
</script>

<style scoped>
.access-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
section { width: min(100%, 420px); }
.brand, p { color: var(--el-text-color-secondary); }
h1 { margin: 20px 0 12px; font-size: 30px; }
.return-link { display: inline-block; margin-top: 16px; text-decoration: underline; }
</style>
