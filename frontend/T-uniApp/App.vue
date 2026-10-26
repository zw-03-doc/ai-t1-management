<script>
import {
  COACH_ROUTE_PREFIX,
  MEMBER_ROUTES,
  PUBLIC_ROUTES,
} from "./utils/config";
import { hasCoachSession, hasMemberSession } from "./utils/auth";

let interceptorInstalled = false;

function normalizeUrl(url = "") {
  return (url.split("?")[0] || "").trim();
}

function openPortal(role, target) {
  const query = [`role=${role}`];
  if (target) {
    query.push(`target=${encodeURIComponent(target)}`);
  }
  setTimeout(() => {
    uni.reLaunch({
      url: `/pages/portal/index?${query.join("&")}`,
    });
  }, 0);
}

function handleRouteGuard(args) {
  const target = normalizeUrl((args && args.url) || "");
  if (!target || PUBLIC_ROUTES.includes(target)) {
    return args;
  }
  const isTraining = target === "/pages/training/index";
  const coachTraining =
    isTraining && /[?&]role=coach(?:&|$)/.test(args.url || "");
  if (target.startsWith(COACH_ROUTE_PREFIX) || coachTraining) {
    if (hasCoachSession()) {
      return args;
    }
    uni.showToast({ title: "请先完成教练登录", icon: "none" });
    openPortal("coach", args.url || target);
    return false;
  }
  if (MEMBER_ROUTES.includes(target) || isTraining) {
    if (hasMemberSession()) {
      return args;
    }
    uni.showToast({ title: "请先登录会员账号", icon: "none" });
    openPortal("member", args.url || target);
    return false;
  }
  return args;
}

function installRouteInterceptor() {
  if (interceptorInstalled) {
    return;
  }
  ["navigateTo", "redirectTo", "reLaunch", "switchTab"].forEach((name) => {
    uni.addInterceptor(name, {
      invoke(args) {
        return handleRouteGuard(args);
      },
    });
  });
  interceptorInstalled = true;
}

export default {
  onLaunch() {
    installRouteInterceptor();
    console.log("T1 uni-app Launch");
  },
  onShow() {
    console.log("T1 uni-app Show");
  },
  onHide() {
    console.log("T1 uni-app Hide");
  },
};
</script>

<style lang="scss">
@import "./styles/theme.scss";
</style>
