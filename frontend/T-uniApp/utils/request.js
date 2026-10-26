import { API_BASE_URL } from "./config";
import {
  clearCoachSession,
  clearMemberSession,
  getCoachSession,
  getMemberSession,
} from "./auth";

let redirecting = false;

// 内存级防重复提交
const submitCache = new Map();
const REPEAT_INTERVAL = 1000;

function isRepeatSubmit(url, data) {
  const key = url + ":" + JSON.stringify(data || {});
  const now = Date.now();
  const last = submitCache.get(key);
  if (last && now - last < REPEAT_INTERVAL) {
    return true;
  }
  submitCache.set(key, now);
  if (submitCache.size > 100) {
    const expire = now - REPEAT_INTERVAL * 2;
    for (const [k, t] of submitCache) {
      if (t < expire) submitCache.delete(k);
    }
  }
  return false;
}

function formatAuthorizationToken(token) {
  if (!token) {
    return "";
  }
  return /^Bearer\s+/i.test(token) ? token : `Bearer ${token}`;
}

function joinUrl(path) {
  if (!path) {
    return API_BASE_URL;
  }
  return `${API_BASE_URL.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
}

function getAuthSession(auth) {
  if (auth === "coach") {
    return getCoachSession();
  }
  if (auth === "member") {
    return getMemberSession();
  }
  return {};
}

function clearAuthSession(auth) {
  if (auth === "coach") {
    clearCoachSession();
    return;
  }
  if (auth === "member") {
    clearMemberSession();
  }
}

function redirectToPortal(auth, message) {
  if (redirecting) {
    return;
  }
  redirecting = true;
  uni.showToast({
    title: message || "登录已失效，请重新登录",
    icon: "none",
  });
  setTimeout(() => {
    uni.reLaunch({
      url: `/pages/portal/index?role=${auth === "coach" ? "coach" : "member"}`,
    });
    setTimeout(() => {
      redirecting = false;
    }, 400);
  }, 120);
}

function resolveSuccessPayload(result) {
  if (result && Object.prototype.hasOwnProperty.call(result, "data")) {
    return result.data;
  }
  return result;
}

function normalizeErrorMessage(message, fallback) {
  const text = String(message || "").trim();
  if (!text) {
    return fallback || "请求失败，请稍后重试";
  }
  if (/timeout|timed out/i.test(text)) {
    return "请求超时，请检查网络后重试";
  }
  if (
    /AppSecret|AppID|openid|code2session|SQL|Exception|tenant_id|Redisson/i.test(
      text,
    )
  ) {
    return "服务暂时不可用，请稍后重试或联系门店";
  }
  if (/fail/i.test(text) && /request/i.test(text)) {
    return "网络请求失败，请检查网络后重试";
  }
  return text;
}

function sanitizeQueryData(method, data) {
  if (
    String(method || "").toUpperCase() !== "GET" ||
    !data ||
    typeof data !== "object" ||
    Array.isArray(data)
  ) {
    return data;
  }
  const cleaned = {};
  Object.keys(data).forEach((key) => {
    const value = data[key];
    if (value !== undefined && value !== null && value !== "") {
      cleaned[key] = value;
    }
  });
  return cleaned;
}

export function request(options = {}) {
  const {
    url,
    method = "GET",
    data,
    auth = "none",
    header = {},
    timeout = 15000,
    noRepeat = false,
  } = options;

  // POST/PUT 防重复提交（默认开启）
  if (
    !noRepeat &&
    (method === "POST" || method === "PUT") &&
    isRepeatSubmit(url, data)
  ) {
    uni.showToast({ title: "请求正在处理，请勿重复提交", icon: "none" });
    return Promise.reject(new Error("请勿重复提交"));
  }

  const authSession = getAuthSession(auth);
  const requestData = sanitizeQueryData(method, data);
  const requestHeader = {
    "content-type": "application/json",
    ...header,
  };

  if (authSession.accessToken) {
    requestHeader.Authorization = formatAuthorizationToken(
      authSession.accessToken,
    );
  }
  if (authSession.clientId) {
    requestHeader.clientid = authSession.clientId;
  }

  return new Promise((resolve, reject) => {
    let retryCount = 0;
    let weakNetworkTimer = null;

    function doRequest() {
      weakNetworkTimer = setTimeout(() => {
        uni.showToast({ title: "网络较慢，请稍候", icon: "none" });
      }, 5000);

      uni.request({
        url: joinUrl(url),
        method,
        data: requestData,
        header: requestHeader,

        timeout,
        success: (response) => {
          clearTimeout(weakNetworkTimer);
          const result = response.data || {};
          const isOk =
            response.statusCode >= 200 &&
            response.statusCode < 300 &&
            Number(result.code) === 200;
          if (isOk) {
            resolve(resolveSuccessPayload(result));
            return;
          }
          if (
            method === "GET" &&
            response.statusCode === 503 &&
            retryCount < 1
          ) {
            retryCount++;
            doRequest();
            return;
          }
          const message = normalizeErrorMessage(
            result.msg,
            "请求失败，请稍后重试",
          );
          if (
            auth !== "none" &&
            (response.statusCode === 401 ||
              Number(result.code) === 401 ||
              /认证|登录状态/.test(message))
          ) {
            clearAuthSession(auth);
            redirectToPortal(auth, message);
          }
          reject(new Error(message));
        },
        fail: (error) => {
          clearTimeout(weakNetworkTimer);
          const errMsg = normalizeErrorMessage(
            error && error.errMsg,
            "网络请求失败",
          );
          if (
            method === "GET" &&
            retryCount < 1 &&
            /timeout/i.test(String((error && error.errMsg) || ""))
          ) {
            retryCount++;
            doRequest();
            return;
          }
          uni.showToast({ title: errMsg, icon: "none" });
          reject(new Error(errMsg));
        },
      });
    }

    doRequest();
  });
}
