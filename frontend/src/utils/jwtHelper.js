/**
 * JWT 处理工具函数
 * 提供JWT解析、Base64URL解码等功能
 */

/**
 * Base64URL 解码
 * @param {string} value - Base64URL编码的字符串
 * @returns {string} 解码后的字符串
 */
export function base64UrlDecode(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return decodeURIComponent(Array.from(atob(padded))
    .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
    .join(''))
}

/**
 * 解析JWT Token
 * @param {string} token - JWT Token字符串
 * @returns {Object|null} 解析后的JWT对象，包含header和payload，解析失败返回null
 */
export function decodeJwt(token) {
  if (typeof token !== 'string' || !token) {
    return null
  }

  const segments = token.split('.')
  if (segments.length < 2) {
    return null
  }

  try {
    return {
      header: JSON.parse(base64UrlDecode(segments[0])),
      payload: JSON.parse(base64UrlDecode(segments[1]))
    }
  } catch {
    return null
  }
}

/**
 * 格式化Claim值用于显示
 * @param {*} value - Claim值
 * @returns {string} 格式化后的字符串
 */
export function formatClaimValue(value) {
  if (value === null || value === undefined || value === '') {
    return '暂无'
  }
  if (Array.isArray(value)) {
    return value.join(', ')
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

/**
 * 标准化Claim值，用于表格对比
 * @param {*} value - Claim值
 * @returns {string} 标准化后的值，空值返回"—"
 */
export function normalizeClaimValue(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  if (Array.isArray(value)) {
    return value.join(', ')
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

/**
 * 构建JWT对比行
 * @param {string} claim - Claim名称
 * @param {*} userAccessTokenValue - 用户Access Token的值
 * @param {*} idTokenValue - ID Token的值
 * @param {*} machineAccessTokenValue - 机器Access Token的值
 * @returns {Object} 对比行对象
 */
export function buildClaimsRow(claim, userAccessTokenValue, idTokenValue, machineAccessTokenValue) {
  return {
    claim,
    userAccessToken: normalizeClaimValue(userAccessTokenValue),
    idToken: normalizeClaimValue(idTokenValue),
    machineAccessToken: normalizeClaimValue(machineAccessTokenValue)
  }
}

/**
 * 生成JWT对比的高亮结论
 * @param {Object} tokens - 包含userAccessToken, idToken, machineAccessToken的对象
 * @returns {Array<string]} 高亮结论数组
 */
export function buildClaimsHighlights({ userAccessToken, idToken: decodedIdToken, machineAccessToken }) {
  const highlights = []

  if (decodedIdToken?.payload) {
    highlights.push('id_token 面向客户端身份会话，通常携带 preferred_username、email、sid 等 OIDC 身份声明。')
  }
  if (userAccessToken?.payload) {
    highlights.push('用户 access_token 面向资源服务器授权，保留 scope 等访问控制字段；它不一定包含完整的用户资料声明。')
  }
  if (machineAccessToken?.payload) {
    highlights.push('client_credentials access_token 通常没有真实用户身份字段，更常见的是 sub 与 client_id 对应，表达"客户端自己"在访问资源。')
  }

  const userSubject = userAccessToken?.payload?.sub
  const machineSubject = machineAccessToken?.payload?.sub
  if (userSubject && machineSubject && userSubject !== machineSubject) {
    highlights.push(`当前对比中，用户 token 的 sub 是 ${userSubject}，机器 token 的 sub 是 ${machineSubject}，两者主体并不相同。`)
  }

  if (!highlights.length) {
    highlights.push('先完成一次 PKCE 登录或获取一次 M2M token，再观察三类 JWT 的 claims 差异。')
  }

  return highlights
}