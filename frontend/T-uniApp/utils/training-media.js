const MIB = 1024 * 1024;

export function selectTrainingMedia(type) {
  const video = type === "video";
  const label = video ? "视频" : "图片";
  const limit = video ? 50 : 10;
  return new Promise((resolve, reject) => {
    const options = {
      count: 1,
      success(result) {
        const file = result.tempFiles?.[0];
        if (!Number.isSafeInteger(file?.size) || file.size <= 0) {
          reject(new Error(`无法读取${label}，请重新选择`));
        } else if (file.size > limit * MIB) {
          reject(new Error(`请选择 ${limit} MB 以内的${label}`));
        } else {
          // #ifdef H5
          resolve(file);
          // #endif
          // #ifdef MP-WEIXIN
          const path = file.tempFilePath || file.path || result.tempFilePaths?.[0];
          if (path) resolve(path);
          else reject(new Error(`无法读取${label}，请重新选择`));
          // #endif
        }
      },
      fail(error) {
        if (/cancel/i.test(error?.errMsg || "")) resolve(null);
        else reject(new Error(`无法打开${label}选择器，请重试`));
      },
    };
    if (!video) {
      uni.chooseImage(options);
      return;
    }
    // #ifdef MP-WEIXIN
    uni.chooseMedia({ ...options, mediaType: ["video"], sourceType: ["album"], sizeType: ["original"] });
    // #endif
    // #ifdef H5
    uni.chooseFile({ ...options, type: "video", extension: [".mp4"], sourceType: ["album"] });
    // #endif
  });
}
