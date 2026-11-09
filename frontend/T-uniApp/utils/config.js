export const DEFAULT_API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "https://t1.wedodiary.top/prod-api";

export const API_BASE_URL =
  (import.meta.env.DEV && uni.getStorageSync("T1_API_BASE_URL")) ||
  DEFAULT_API_BASE_URL;

export const PUBLIC_ROUTES = [
  "/pages/legal/index",
  "/pages/portal/index",
  "/pages/portal/gallery",
  "/pages/guide/index",
];

export const MEMBER_ROUTES = [
  "/pages/index/index",
  "/pages/course/index",
  "/pages/activity/index",
  "/pages/activity/detail",
  "/pages/activity/plan",
  "/pages/mine/index",
  "/pages/mine/profile",
];

export const COACH_ROUTE_PREFIX = "/pages/coach/";
