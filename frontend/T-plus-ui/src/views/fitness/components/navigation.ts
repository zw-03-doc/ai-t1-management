import type { Router, LocationQueryRaw } from 'vue-router';
import { ElMessage } from 'element-plus';

/** Use the server-authorized menu route, preserving the member/store context. */
export function openFitnessPage(router: Router, page: string, query: LocationQueryRaw = {}) {
  const target = router.getRoutes().find((route) => route.path.includes('fitness') && route.path.endsWith(`/${page}`));
  if (!target) {
    ElMessage.warning('当前账号没有该页面的访问权限，请联系管理员');
    return;
  }
  return router.push({ path: target.path, query });
}
