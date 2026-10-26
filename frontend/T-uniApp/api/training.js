import { request } from "../utils/request";
import { API_BASE_URL } from "../utils/config";
import { getCoachSession, getMemberSession } from "../utils/auth";
export function trainingApi(path, method = "GET", data, auth = "member") {
  return request({ url: `/fitness/training${path}`, method, data, auth });
}

function headers(role) {
  const session = role === "coach" ? getCoachSession() : getMemberSession();
  return {
    Authorization: /^Bearer /i.test(session.accessToken)
      ? session.accessToken
      : `Bearer ${session.accessToken}`,
    clientid: session.clientId,
  };
}
export function uploadTrainingMedia(filePath, planId, role) {
  return new Promise((resolve, reject) =>
    uni.uploadFile({
      url: `${API_BASE_URL}/fitness/training/media`,
      filePath,
      name: "file",
      formData: { planId: String(planId) },
      header: headers(role),
      success(res) {
        try {
          const result = JSON.parse(res.data);
          if (
            res.statusCode >= 200 &&
            res.statusCode < 300 &&
            result.code === 200
          )
            resolve(result.data);
          else reject(new Error(result.msg || "上传失败"));
        } catch (e) {
          reject(e);
        }
      },
      fail: reject,
    }),
  );
}
export async function resolveTrainingMedia(url, role) {
  if (!url?.startsWith("/fitness/training/media/")) return url;
  // Downloads are authenticated. Neither tokens nor private media are put in a URL.
  // #ifdef H5
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: headers(role),
  });
  if (!response.ok) throw new Error("动作资料读取失败");
  return URL.createObjectURL(await response.blob());
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) =>
    uni.downloadFile({
      url: `${API_BASE_URL}${url}`,
      header: headers(role),
      success: (r) =>
        r.statusCode === 200
          ? resolve(r.tempFilePath)
          : reject(new Error("动作资料读取失败")),
      fail: reject,
    }),
  );
  // #endif
}
