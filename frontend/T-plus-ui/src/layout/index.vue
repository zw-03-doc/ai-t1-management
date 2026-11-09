<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme }">
    <button v-if="drawerOpen" class="drawer-bg" type="button" aria-label="关闭导航菜单" @click="appStore.closeMobileDrawer" />
    <side-bar v-if="!sidebar.hide" id="app-navigation" class="sidebar-container" :inert="device === 'mobile' && !sidebar.opened" @click="onSidebarClick" />
    <div :class="{ sidebarHide: sidebar.hide }" class="main-container">
      <div :class="{ 'fixed-header': fixedHeader }">
        <navbar ref="navbarRef" />
      </div>
      <app-main />
      <settings />
    </div>
  </div>
</template>

<script setup lang="ts">
import SideBar from './components/Sidebar/index.vue';
import { AppMain, Navbar, Settings } from './components';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { useEventListener, useMediaQuery } from '@vueuse/core';

const settingsStore = useSettingsStore();
const appStore = useAppStore();
const route = useRoute();
const theme = computed(() => settingsStore.theme);
const sidebar = computed(() => appStore.sidebar);
const device = computed(() => appStore.device);
const fixedHeader = computed(() => settingsStore.fixedHeader);
const drawerOpen = computed(() => device.value === 'mobile' && !sidebar.value.hide && sidebar.value.opened);

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}));

const isNarrow = useMediaQuery('(max-width: 992px)');
watch(isNarrow, (narrow) => appStore.setDevice(narrow ? 'mobile' : 'desktop'), { immediate: true });
watch(() => route.fullPath, () => appStore.closeMobileDrawer());
useEventListener('keydown', (event) => {
  if (drawerOpen.value && event.key === 'Escape') appStore.closeMobileDrawer();
});

function onSidebarClick(event: MouseEvent) {
  // Re-selecting the current page produces no route change. A group title should stay open.
  if (drawerOpen.value && event.target instanceof Element && event.target.closest('a[href]')) {
    appStore.closeMobileDrawer();
  }
}

const navbarRef = ref<InstanceType<typeof Navbar>>();

onMounted(() => {
  nextTick(() => {
    navbarRef.value?.initTenantList();
  });
});



</script>

<style lang="scss" scoped>
@use '@/assets/styles/mixin.scss';
@use '@/assets/styles/variables.module.scss' as *;

.app-wrapper {
  @include mixin.clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: rgb(0 0 0 / 30%);
  border: 0;
  padding: 0;
  inset: 0;
  width: 100%;
  height: 100%;
  position: fixed;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{$base-sidebar-width});
  transition: width 0.28s;
  background: $fixed-header-bg;
}

.hideSidebar .fixed-header {
  width: calc(100% - 54px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}
</style>
