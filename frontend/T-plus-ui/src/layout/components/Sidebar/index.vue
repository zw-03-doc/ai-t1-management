<template>
  <div :class="{ 'has-logo': showLogo }" :style="{ backgroundColor: bgColor }">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar :class="sideTheme" wrap-class="scrollbar-wrapper">
      <transition :enter-active-class="proxy?.animate.menuSearchAnimate.enter" mode="out-in">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :background-color="bgColor"
          :text-color="textColor"
          :unique-opened="true"
          :active-text-color="theme"
          :collapse-transition="false"
          :popper-offset="12"
          mode="vertical"
        >
          <sidebar-item v-for="r in gymRoutes" :key="r.path" :item="r" :base-path="r.path" />
          <el-sub-menu v-if="otherRoutes.length" index="system-tools">
            <template #title><svg-icon icon-class="system" /><span>系统与工具</span></template>
            <sidebar-item v-for="r in otherRoutes" :key="r.path" :item="r" :base-path="r.path" />
          </el-sub-menu>
        </el-menu>
      </transition>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import Logo from './Logo.vue';
import SidebarItem from './SidebarItem.vue';
import variables from '@/assets/styles/variables.module.scss';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { RouteRecordRaw } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const route = useRoute();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const sidebarRouters = computed<RouteRecordRaw[]>(() => permissionStore.getSidebarRoutes());
const isGymRoute = (r: RouteRecordRaw) => !r.path || r.path === '/' || r.path.startsWith('/fitness') || r.children?.some(child => child.path === '/index');
const gymRoutes = computed(() => sidebarRouters.value.filter(isGymRoute));
const otherRoutes = computed(() => sidebarRouters.value.filter(r => !r.hidden && !isGymRoute(r)));
const showLogo = computed(() => settingsStore.sidebarLogo);
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);

const activeMenu = computed(() => {
  const { meta, path } = route;
  // if set path, the sidebar will highlight the path you set
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const bgColor = computed(() => (sideTheme.value === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground));
const textColor = computed(() => (sideTheme.value === 'theme-dark' ? variables.menuColor : variables.menuLightColor));
</script>
