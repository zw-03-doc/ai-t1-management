import zhCN from 'element-plus/es/locale/lang/zh-cn';
import enUS from 'element-plus/es/locale/lang/en';
import { defineStore } from 'pinia';
import { useStorage } from '@vueuse/core';
import { ref, reactive, computed } from 'vue';

export const useAppStore = defineStore('app', () => {
  const sidebarStatus = useStorage('sidebarStatus', '1');
  const sidebar = reactive({
    opened: sidebarStatus.value ? !!+sidebarStatus.value : true,
    withoutAnimation: false,
    hide: false
  });
  const device = ref<string>('desktop');
  const size = useStorage<'large' | 'default' | 'small'>('size', 'default');

  // 语言
  const language = useStorage('language', 'zh_CN');
  const languageObj: any = {
    en_US: enUS,
    zh_CN: zhCN
  };
  const locale = computed(() => {
    return languageObj[language.value];
  });

  const toggleSideBar = (withoutAnimation: boolean) => {
    if (sidebar.hide) {
      return false;
    }

    sidebar.opened = !sidebar.opened;
    sidebar.withoutAnimation = withoutAnimation;
    if (device.value === 'desktop') {
      sidebarStatus.value = sidebar.opened ? '1' : '0';
    }
  };

  const closeMobileDrawer = (): void => {
    if (device.value === 'mobile') {
      sidebar.opened = false;
      sidebar.withoutAnimation = false;
    }
  };
  const setDevice = (next: 'mobile' | 'desktop'): void => {
    device.value = next;
    sidebar.opened = next === 'desktop' && sidebarStatus.value === '1';
    sidebar.withoutAnimation = true;
  };
  const setSize = (s: 'large' | 'default' | 'small'): void => {
    size.value = s;
  };
  const toggleSideBarHide = (status: boolean): void => {
    sidebar.hide = status;
  };

  const changeLanguage = (val: string): void => {
    language.value = val;
  };

  return {
    device,
    sidebar,
    language,
    locale,
    size,
    changeLanguage,
    toggleSideBar,
    closeMobileDrawer,
    setDevice,
    setSize,
    toggleSideBarHide
  };
});
