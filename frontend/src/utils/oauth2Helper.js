/**
 * OAuth2 辅助工具函数
 * 提供OAuth2流程相关的常用功能
 */

import { generatePkcePair, generateRandomString } from '../composables/pkce'

export const DEFAULT_OAUTH2_SYNC_CHANNEL = 'oauth2-token-sync'
export const OAUTH2_SYNC_EVENT = 'oauth2-callback-complete'

/**
 * 获取授权服务器地址
 * @returns {string} 授权服务器地址
 */
export function getAuthServerOrigin() {
  return import.meta.env.VITE_BACKEND_ORIGIN || `${window.location.protocol}//${window.location.hostname}:30000`
}

export function prepareOAuth2Redirect({
  clientId,
  clientSecret = '',
  usePkce,
  scenario,
  returnTo,
  syncChannel = DEFAULT_OAUTH2_SYNC_CHANNEL,
  syncTarget = ''
}, {
  state,
  codeVerifier = ''
}) {
  sessionStorage.setItem('oauth2_state', state)
  sessionStorage.setItem('pkce_mode', usePkce ? 'required' : 'disabled')
  sessionStorage.setItem('oauth2_demo_scenario', scenario)
  sessionStorage.setItem('oauth2_client_id', clientId)
  if (clientSecret) {
    sessionStorage.setItem('oauth2_client_secret', clientSecret)
    sessionStorage.setItem('oauth2_active_client_secret', clientSecret)
  } else {
    sessionStorage.removeItem('oauth2_client_secret')
    sessionStorage.removeItem('oauth2_active_client_secret')
  }
  sessionStorage.setItem('oauth2_return_to', returnTo)
  sessionStorage.setItem('oauth2_sync_channel', syncChannel)
  sessionStorage.setItem('oauth2_sync_target', syncTarget)

  if (usePkce) {
    sessionStorage.setItem('pkce_code_verifier', codeVerifier)
    return
  }

  sessionStorage.removeItem('pkce_code_verifier')
}

export function readOAuth2SyncContext() {
  return {
    syncChannel: sessionStorage.getItem('oauth2_sync_channel') || DEFAULT_OAUTH2_SYNC_CHANNEL,
    syncTarget: sessionStorage.getItem('oauth2_sync_target') || '',
    clientId: sessionStorage.getItem('oauth2_client_id') || 'spa-public-client',
    demoScenario: sessionStorage.getItem('oauth2_demo_scenario') || ''
  }
}

export function clearOAuth2RedirectContext() {
  sessionStorage.removeItem('oauth2_state')
  sessionStorage.removeItem('pkce_code_verifier')
  sessionStorage.removeItem('pkce_mode')
  sessionStorage.removeItem('oauth2_demo_scenario')
  sessionStorage.removeItem('oauth2_client_secret')
  sessionStorage.removeItem('oauth2_sync_channel')
  sessionStorage.removeItem('oauth2_sync_target')
}

export function buildOAuth2SyncPayload(data, overrides = {}) {
  const context = readOAuth2SyncContext()
  return {
    access_token: data.access_token || '',
    id_token: data.id_token || '',
    refresh_token: data.refresh_token || '',
    scope: data.scope || '',
    expires_in: data.expires_in || 0,
    client_id: context.clientId,
    syncTarget: context.syncTarget || (context.demoScenario === 'claims-user-login' ? 'claims' : ''),
    ...overrides
  }
}

export function broadcastOAuth2Sync(data, overrides = {}) {
  const { syncChannel } = readOAuth2SyncContext()
  const payload = buildOAuth2SyncPayload(data, overrides)

  if (typeof BroadcastChannel !== 'undefined') {
    const channel = new BroadcastChannel(syncChannel)
    channel.postMessage({ type: OAUTH2_SYNC_EVENT, payload })
    channel.close()
    return payload
  }

  localStorage.setItem(syncChannel, JSON.stringify({
    type: OAUTH2_SYNC_EVENT,
    payload,
    timestamp: Date.now()
  }))
  localStorage.removeItem(syncChannel)
  return payload
}

export function createOAuth2SyncListener(syncChannel, onPayload) {
  let channel = null

  const handleMessage = message => {
    if (message?.type === OAUTH2_SYNC_EVENT) {
      onPayload(message.payload)
    }
  }

  const handleStorage = event => {
    if (event.key !== syncChannel || !event.newValue) {
      return
    }

    try {
      handleMessage(JSON.parse(event.newValue))
    } catch {
      // ignore malformed sync payload
    }
  }

  if (typeof BroadcastChannel !== 'undefined') {
    channel = new BroadcastChannel(syncChannel)
    channel.onmessage = event => {
      handleMessage(event.data)
    }
  } else {
    window.addEventListener('storage', handleStorage)
  }

  return () => {
    channel?.close()
    window.removeEventListener('storage', handleStorage)
  }
}

/**
 * 启动授权码流程
 * @param {Object} options - 授权选项
 * @param {string} options.clientId - 客户端ID
 * @param {string} options.scope - 授权范围
 * @param {boolean} options.usePkce - 是否使用PKCE
 * @param {string} options.scenario - 场景标识
 * @param {string} options.returnTo - 授权后返回的路径
 */
export async function startAuthorizationCodeFlow({ clientId, clientSecret = '', scope, usePkce = true, scenario = 'default', returnTo = '/pkce', openInNewWindow = false, syncChannel = DEFAULT_OAUTH2_SYNC_CHANNEL, syncTarget = '' }) {
  const frontendOrigin = window.location.origin
  const redirectUri = `${frontendOrigin}/callback`
  const state = generateRandomString(32)
  let codeVerifier = ''
  let codeChallenge = ''

  if (usePkce) {
    const pkcePair = await generatePkcePair()
    codeVerifier = pkcePair.codeVerifier
    codeChallenge = pkcePair.codeChallenge
  }

  prepareOAuth2Redirect({
    clientId,
    clientSecret,
    usePkce,
    scenario,
    returnTo,
    syncChannel,
    syncTarget
  }, {
    state,
    codeVerifier
  })

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope,
    state
  })

  if (usePkce) {
    params.set('code_challenge', codeChallenge)
    params.set('code_challenge_method', 'S256')
  }

  const authServerOrigin = getAuthServerOrigin()
  const authorizeUrl = `${authServerOrigin}/oauth2/authorize?${params.toString()}`
  if (openInNewWindow) {
    window.open(authorizeUrl, '_blank')
    return
  }
  window.location.href = authorizeUrl
}

/**
 * 解析Scope字符串为数组
 * @param {string} scopeText - 空格分隔的scope字符串
 * @returns {Array<string>} Scope数组
 */
export function splitScopeString(scopeText) {
  if (!scopeText) {
    return ['openid', 'profile']
  }
  const scopes = scopeText
    .split(/\s+/)
    .map(item => item.trim())
    .filter(Boolean)

  return scopes.length > 0 ? scopes : ['openid', 'profile']
}

/**
 * 统一错误处理
 * @param {Error} e - 错误对象
 * @param {Function} showMessage - 显示消息的函数（如ElMessage.error）
 * @returns {Object} 错误信息对象
 */
export function handleOAuth2Error(e, showMessage) {
  const errorData = e.response?.data || { error: e.message }
  const errorMessage = e.response?.data?.error_description || e.response?.data?.error || e.message

  if (showMessage) {
    showMessage(errorMessage)
  }

  return errorData
}

/**
 * 生成Basic认证头
 * @param {string} clientId - 客户端ID
 * @param {string} clientSecret - 客户端密钥
 * @returns {string} Basic认证头值
 */
export function generateBasicAuth(clientId, clientSecret) {
  return btoa(`${clientId}:${clientSecret}`)
}

/**
 * 构建动态客户端ID
 * @returns {string} 动态生成的客户端ID
 */
export function buildDynamicClientId() {
  return `dynamic-spa-${Date.now().toString().slice(-8)}`
}

/**
 * 规范化返回路径
 * @param {string} value - 原始返回路径
 * @returns {string} 规范化后的路径，无效时返回空字符串
 */
export function normalizeReturnTo(value) {
  if (typeof value !== 'string' || !value.startsWith('/')) return ''
  return value.startsWith('//') ? '' : value
}

/**
 * 延迟函数
 * @param {number} ms - 延迟毫秒数
 * @returns {Promise} Promise对象
 */
export function delay(ms) {
  return new Promise(resolve => {
    window.setTimeout(resolve, ms)
  })
}
