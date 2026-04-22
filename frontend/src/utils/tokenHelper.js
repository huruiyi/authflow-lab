/**
 * Token 处理工具函数
 * 提供Token的显示、遮罩、过期时间管理等常用功能
 */

/**
 * Token 遮罩显示
 * @param {string} token - 原始token
 * @returns {string} 遮罩后的token
 */
export function maskToken(token) {
  if (typeof token !== 'string' || token.length < 24) return token || ''
  return `${token.slice(0, 12)}...${token.slice(-12)}`
}

/**
 * 持久化Token过期信息
 * @param {number} expiresInSeconds - 过期秒数
 */
export function persistAccessTokenExpiryMetadata(expiresInSeconds) {
  if (!expiresInSeconds) {
    return
  }
  const issuedAt = Date.now()
  sessionStorage.setItem('oauth2_access_token_issued_at', String(issuedAt))
  sessionStorage.setItem('oauth2_access_token_expires_at', String(issuedAt + Number(expiresInSeconds) * 1000))
  sessionStorage.setItem('oauth2_access_token_expires_in', String(expiresInSeconds))
}

/**
 * 获取Token剩余秒数
 * @returns {number|null} 剩余秒数，未知时返回null
 */
export function getTokenRemainingSeconds() {
  const expiresAtRaw = sessionStorage.getItem('oauth2_access_token_expires_at')
  const expiresAt = Number(expiresAtRaw)
  if (!Number.isFinite(expiresAt) || expiresAt <= 0) {
    return null
  }
  return Math.floor((expiresAt - Date.now()) / 1000)
}

/**
 * 格式化Token剩余时间显示
 * @returns {string} 格式化的剩余时间字符串
 */
export function formatTokenRemainingTime() {
  const remainingSeconds = getTokenRemainingSeconds()
  if (remainingSeconds === null) {
    return '未知（请先获取 token）'
  }
  if (remainingSeconds < 0) {
    return `${Math.abs(remainingSeconds)} 秒前已过期`
  }
  return `${remainingSeconds} 秒`
}

/**
 * 保存Token到sessionStorage
 * @param {Object} data - Token响应数据
 */
export function saveTokens(data) {
  if (data.access_token) {
    sessionStorage.setItem('oauth2_access_token', data.access_token)
  }
  if (data.id_token) {
    sessionStorage.setItem('oauth2_id_token', data.id_token)
  }
  if (data.refresh_token) {
    sessionStorage.setItem('oauth2_refresh_token', data.refresh_token)
  }
  if (data.scope) {
    sessionStorage.setItem('oauth2_scope', data.scope)
  }
  if (data.expires_in) {
    persistAccessTokenExpiryMetadata(data.expires_in)
  }
}

/**
 * 清除所有Token
 */
export function clearTokens() {
  sessionStorage.removeItem('oauth2_access_token')
  sessionStorage.removeItem('oauth2_id_token')
  sessionStorage.removeItem('oauth2_refresh_token')
  sessionStorage.removeItem('oauth2_scope')
  sessionStorage.removeItem('oauth2_access_token_issued_at')
  sessionStorage.removeItem('oauth2_access_token_expires_at')
  sessionStorage.removeItem('oauth2_access_token_expires_in')
}

/**
 * 获取当前Token状态
 * @returns {Object} Token状态对象
 */
export function getTokenState() {
  return {
    accessToken: sessionStorage.getItem('oauth2_access_token') || '',
    idToken: sessionStorage.getItem('oauth2_id_token') || '',
    refreshToken: sessionStorage.getItem('oauth2_refresh_token') || '',
    scope: sessionStorage.getItem('oauth2_scope') || '',
    remainingSeconds: getTokenRemainingSeconds(),
    isExpired: getTokenRemainingSeconds() !== null && getTokenRemainingSeconds() < 0
  }
}