/**
 * 设备流程工具函数
 * 提供Device Code Flow相关的辅助功能
 */

const DEVICE_FLOW_TIMEOUT_MS = 2 * 60 * 1000
const DEFAULT_DEVICE_INTERVAL_SECONDS = 5

/**
 * 获取设备流程存储键
 * @param {string} userCode - 用户码
 * @returns {string} 存储键
 */
export function getDeviceFlowStorageKey(userCode) {
  return `oauth2_device_flow_${userCode}`
}

/**
 * 持久化设备流程时间信息
 * @param {string} userCode - 用户码
 * @param {number} startedAt - 开始时间戳
 * @param {number} expiresAt - 过期时间戳
 */
export function persistDeviceFlowTiming(userCode, startedAt, expiresAt) {
  if (typeof userCode !== 'string' || !userCode) return
  localStorage.setItem(getDeviceFlowStorageKey(userCode), JSON.stringify({ startedAt, expiresAt }))
}

/**
 * 获取持久化的设备流程时间信息
 * @param {string} userCode - 用户码
 * @returns {Object|null} 时间信息对象，不存在时返回null
 */
export function getPersistedDeviceFlowTiming(userCode) {
  if (typeof userCode !== 'string' || !userCode) return null
  const rawValue = localStorage.getItem(getDeviceFlowStorageKey(userCode))
  if (!rawValue) return null

  try {
    const parsed = JSON.parse(rawValue)
    if (Number.isFinite(parsed?.startedAt) && Number.isFinite(parsed?.expiresAt)) {
      return parsed
    }
  } catch {
    localStorage.removeItem(getDeviceFlowStorageKey(userCode))
  }

  return null
}

/**
 * 获取设备流程开始时间
 * @param {Object} deviceAuthValue - 设备授权信息对象
 * @returns {number} 开始时间戳
 */
export function getDeviceFlowStartedAt(deviceAuthValue) {
  const startedAt = Number(deviceAuthValue?.started_at)
  if (Number.isFinite(startedAt) && startedAt > 0) {
    return startedAt
  }

  const persistedTiming = getPersistedDeviceFlowTiming(deviceAuthValue?.user_code)
  if (persistedTiming?.startedAt) {
    return persistedTiming.startedAt
  }

  return Date.now()
}

/**
 * 获取设备轮询间隔毫秒数
 * @param {number} intervalSeconds - 服务端返回的间隔秒数
 * @returns {number} 轮询间隔毫秒数
 */
export function getDevicePollingIntervalMs(intervalSeconds) {
  const parsedInterval = Number(intervalSeconds)
  const safeInterval = Number.isFinite(parsedInterval) && parsedInterval > 0
    ? parsedInterval
    : DEFAULT_DEVICE_INTERVAL_SECONDS
  return safeInterval * 1000
}

/**
 * 向设备流程URL添加时间参数
 * @param {string} url - 原始URL
 * @param {number} startedAt - 开始时间戳
 * @param {number} expiresAt - 过期时间戳
 * @returns {string} 添加时间参数后的URL
 */
export function appendDeviceTimingParams(url, startedAt, expiresAt) {
  if (typeof url !== 'string' || !url) return url
  const parsedUrl = new URL(url)
  parsedUrl.searchParams.set('started_at', String(startedAt))
  parsedUrl.searchParams.set('expires_at', String(expiresAt))
  return parsedUrl.toString()
}

/**
 * 装饰设备授权信息
 * @param {Object} data - 原始设备授权响应数据
 * @returns {Object} 装饰后的设备授权信息
 */
export function decorateDeviceAuth(data) {
  const startedAt = Date.now()
  const expiresAt = startedAt + DEVICE_FLOW_TIMEOUT_MS
  const verificationUri = appendDeviceTimingParams(data.verification_uri, startedAt, expiresAt)
  const verificationUriComplete = appendDeviceTimingParams(data.verification_uri_complete, startedAt, expiresAt)

  persistDeviceFlowTiming(data.user_code, startedAt, expiresAt)

  return {
    ...data,
    started_at: startedAt,
    expires_at: expiresAt,
    verification_uri: verificationUri,
    verification_uri_complete: verificationUriComplete
  }
}

/**
 * 检查设备流程是否超时
 * @param {Object} deviceAuthValue - 设备授权信息对象
 * @returns {boolean} 是否超时
 */
export function isDeviceFlowExpired(deviceAuthValue) {
  const startedAt = getDeviceFlowStartedAt(deviceAuthValue)
  return Date.now() - startedAt >= DEVICE_FLOW_TIMEOUT_MS
}

/**
 * 获取设备流程超时时间戳
 * @param {Object} deviceAuthValue - 设备授权信息对象
 * @returns {number} 超时时间戳
 */
export function getDeviceFlowExpiresAt(deviceAuthValue) {
  return deviceAuthValue?.expires_at || (getDeviceFlowStartedAt(deviceAuthValue) + DEVICE_FLOW_TIMEOUT_MS)
}

/**
 * 清理过期的设备流程数据
 */
export function cleanupExpiredDeviceFlows() {
  const now = Date.now()
  const prefix = 'oauth2_device_flow_'

  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith(prefix)) {
      try {
        const rawValue = localStorage.getItem(key)
        if (rawValue) {
          const parsed = JSON.parse(rawValue)
          if (parsed.expiresAt && now > parsed.expiresAt) {
            localStorage.removeItem(key)
          }
        }
      } catch {
        // 忽略解析错误，保留该键
      }
    }
  }
}