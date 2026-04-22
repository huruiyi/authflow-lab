/**
 * OAuth2 辅助工具函数
 * 提供OAuth2流程相关的常用功能
 */

import { generatePkcePair, generateRandomString } from '../composables/pkce'
import { saveTokens } from './tokenHelper'

/**
 * 获取授权服务器地址
 * @returns {string} 授权服务器地址
 */
export function getAuthServerOrigin() {
  return import.meta.env.VITE_BACKEND_ORIGIN || `${window.location.protocol}//${window.location.hostname}:30000`
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
export async function startAuthorizationCodeFlow({ clientId, scope, usePkce = true, scenario = 'default', returnTo = '/pkce' }) {
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

  sessionStorage.setItem('oauth2_state', state)
  sessionStorage.setItem('pkce_mode', usePkce ? 'required' : 'disabled')
  sessionStorage.setItem('oauth2_demo_scenario', scenario)
  if (usePkce) {
    sessionStorage.setItem('pkce_code_verifier', codeVerifier)
  } else {
    sessionStorage.removeItem('pkce_code_verifier')
  }
  sessionStorage.setItem('oauth2_client_id', clientId)
  sessionStorage.setItem('oauth2_return_to', returnTo)

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
  window.location.href = `${authServerOrigin}/oauth2/authorize?${params.toString()}`
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