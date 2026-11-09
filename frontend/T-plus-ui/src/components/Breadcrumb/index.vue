<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="String(item.redirect).toLowerCase() === 'noredirect' || index === levelList.length - 1" class="no-redirect">{{ item.meta?.title }}</span>
        <router-link v-else :to="item.path">{{ item.meta?.title }}</router-link>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
const route = useRoute();

const levelList = computed(() => {
  const items = route.matched
    .filter(item => item.meta.title && item.meta.breadcrumb !== false)
    .map(item => ({ path: item.path, meta: item.meta, redirect: item.redirect }));
  const home = { path: '/index', meta: { title: '首页' }, redirect: undefined };

  if (route.path.startsWith('/system/user-auth/')) {
    return [home, { path: '/system/user', meta: { title: '员工账号' }, redirect: undefined }, ...items.slice(-1)];
  }
  if (route.path.startsWith('/system/role-auth/')) {
    return [home, { path: '/system/role', meta: { title: '角色权限' }, redirect: undefined }, ...items.slice(-1)];
  }
  if (['/system/user', '/system/role'].includes(route.path)) return [home, ...items.slice(-1)];
  return route.path === '/index' ? [home] : [home, ...items];
});
</script>

<style lang="scss" scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;
  margin-left: 8px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>
