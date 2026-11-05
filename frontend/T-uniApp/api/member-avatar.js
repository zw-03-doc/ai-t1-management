import { API_BASE_URL } from '../utils/config'
import { getMemberSession } from '../utils/auth'

export const MEMBER_AVATAR_PREFIX = '/fitness/member/avatars/'
export const MAX_AVATAR_BYTES = 5 * 1024 * 1024

function memberHeaders() {
  const { accessToken, clientId } = getMemberSession()
  if (!accessToken || !clientId) throw new Error('请重新登录会员账号')
  return {
    Authorization: /^Bearer /i.test(accessToken) ? accessToken : 'Bearer ' + accessToken,
    clientid: clientId
  }
}

export function uploadMemberAvatar(filePath) {
  const header = memberHeaders()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: API_BASE_URL.replace(/\/$/, '') + '/fitness/mini/mine/avatar',
      filePath,
      name: 'file',
      header,
      success(response) {
        let result
        try {
          result = typeof response.data === 'string' ? JSON.parse(response.data) : response.data
        } catch {
          reject(new Error('头像上传失败，请重试'))
          return
        }
        const avatarUrl = result?.data?.avatarUrl
        if (response.statusCode >= 200 && response.statusCode < 300 && Number(result?.code) === 200
            && typeof avatarUrl === 'string' && avatarUrl.startsWith(MEMBER_AVATAR_PREFIX)) {
          resolve(avatarUrl)
        } else {
          reject(new Error(result?.msg || '头像上传失败，请重试'))
        }
      },
      fail: () => reject(new Error('头像上传失败，请检查网络后重试'))
    })
  })
}

export async function downloadMemberAvatar(path) {
  if (!path.startsWith(MEMBER_AVATAR_PREFIX)) throw new Error('头像地址无效')
  const url = API_BASE_URL.replace(/\/$/, '') + path
  const headers = memberHeaders()
  // #ifdef H5
  const response = await fetch(url, { headers })
  if (!response.ok || !response.headers.get('content-type')?.startsWith('image/')) throw new Error('头像读取失败')
  return URL.createObjectURL(await response.blob())
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url,
      header: headers,
      success: result => result.statusCode === 200 ? resolve(result.tempFilePath) : reject(new Error('头像读取失败')),
      fail: reject
    })
  })
  // #endif
}

export function releaseMemberAvatar(url) {
  // #ifdef H5
  if (url?.startsWith('blob:')) URL.revokeObjectURL(url)
  // #endif
}
