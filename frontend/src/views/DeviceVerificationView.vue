<template>
  <div class="oauth-page">
    <el-card class="oauth-card">
      <template #header>
        <div class="header-row">
          <div>
            <div class="page-title">设备授权确认</div>
            <div class="page-subtitle">确认后浏览器会返回之前的页面，设备端继续完成轮询</div>
          </div>
          <el-tag type="success">Device Code</el-tag>
        </div>
      </template>

      <el-alert
        v-if="message"
        :title="message"
        :type="messageType"
        show-icon
        :closable="false"
        class="mb16"
      />

      <div v-if="hasCountdown" class="countdown-box mb16" :class="{ expired: isExpired }">
        <div class="countdown-label">剩余授权时间</div>
        <div class="countdown-value">{{ countdownText }}</div>
      </div>

      <div v-if="success" class="success-state">
        <p class="success-text">设备授权已经完成，设备端现在可以继续轮询 token 端点获取访问令牌。</p>
      </div>

      <el-descriptions v-else :column="1" border class="mb16">
        <el-descriptions-item label="client_id">{{ clientDisplayText }}</el-descriptions-item>
        <el-descriptions-item label="user_code">{{ normalizedUserCode || '待输入' }}</el-descriptions-item>
        <el-descriptions-item label="scope">
          <div class="scope-list">
            <el-tag v-for="scope in scopes" :key="scope" class="scope-tag">{{ scope }}</el-tag>
            <span v-if="!scopes.length">{{ scopeDisplayText }}</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="success" class="actions">
        <el-button type="primary" @click="goBack">返回上一页</el-button>
        <el-button @click="goFlows">去设备演示页</el-button>
      </div>

      <el-form v-else @submit.prevent>
        <el-form-item v-if="showManualUserCodeInput" label="user_code">
          <el-input
            v-model="manualUserCode"
            placeholder="请输入设备页面显示的 user_code，例如 WBHJ-JVZK"
            maxlength="32"
            clearable
            @keyup.enter="resolveDeviceVerificationContext"
          >
            <template #append>
              <el-button :loading="resolvingContext" @click="resolveDeviceVerificationContext">识别验证码</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-checkbox-group v-model="approvedScopes">
            <el-checkbox v-for="scope in scopes" :key="scope" :label="scope">
              允许访问 {{ scope }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="actions">
          <el-button type="primary" :loading="submitting" :disabled="isExpired || resolvingContext" @click="approveConsent">确认授权</el-button>
          <el-button :loading="submitting" @click="denyConsent">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const authServerOrigin = import.meta.env.VITE_BACKEND_ORIGIN || `${window.location.protocol}//${window.location.hostname}:30000`
const countdownNow = ref(Date.now())
const countdownTimerId = window.setInterval(() => {
  countdownNow.value = Date.now()
}, 1000)
const resolvingContext = ref(false)
const submitting = ref(false)
const success = ref(hasSuccessFlag(route.query.success))
const message = ref(success.value ? '设备授权已提交成功，现在可以回到先前页面继续查看轮询结果。' : '')
const messageType = ref(success.value ? 'success' : 'info')
const clientId = computed(() => getSingleValue(route.query.client_id))
const state = computed(() => getSingleValue(route.query.state))
const routeUserCode = computed(() => getSingleValue(route.query.user_code))
const manualUserCode = ref(routeUserCode.value)
const csrfToken = computed(() => getSingleValue(route.query._csrf))
const csrfParameter = computed(() => getSingleValue(route.query._csrf_parameter) || '_csrf')
const scopes = computed(() => normalizeScopes(route.query.scope))
const approvedScopes = ref([...scopes.value])
const requiresConsent = computed(() => Boolean(clientId.value && state.value))
const normalizedUserCode = computed(() => normalizeUserCode(routeUserCode.value || manualUserCode.value))
const showManualUserCodeInput = computed(() => !routeUserCode.value)
const deviceFlowTiming = getDeviceFlowTiming(getSingleValue(route.query.started_at), getSingleValue(route.query.expires_at), normalizedUserCode.value)
const expiresAt = computed(() => deviceFlowTiming.expiresAt)
const hasCountdown = computed(() => Number.isFinite(expiresAt.value) && expiresAt.value > 0)
const remainingMs = computed(() => {
  if (!hasCountdown.value) return 0
  return Math.max(expiresAt.value - countdownNow.value, 0)
})
const isExpired = computed(() => hasCountdown.value && remainingMs.value <= 0)
const countdownText = computed(() => {
  if (!hasCountdown.value) return '未设置'
  if (isExpired.value) return '已过期'
  const totalSeconds = Math.ceil(remainingMs.value / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
const clientDisplayText = computed(() => {
  if (clientId.value) return clientId.value
  return normalizedUserCode.value ? '点击“识别验证码”后自动识别' : '待输入 user_code'
})
const scopeDisplayText = computed(() => {
  return normalizedUserCode.value ? '识别验证码后展示授权 scope' : '输入 user_code 后展示授权范围'
})

if (!success.value && isExpired.value) {
  message.value = '该 Device Code 已过期，请回到设备演示页重新申请。'
  messageType.value = 'warning'
}

onBeforeUnmount(() => {
  window.clearInterval(countdownTimerId)
})

function approveConsent() {
  if (isExpired.value) {
    message.value = '该 Device Code 已过期，请回到设备演示页重新申请。'
    messageType.value = 'warning'
    return
  }

  if (!normalizedUserCode.value) {
    message.value = '缺少 user_code 参数，无法继续授权。'
    messageType.value = 'error'
    return
  }

  if (!routeUserCode.value) {
    resolveDeviceVerificationContext()
    return
  }

  if (!csrfToken.value) {
    message.value = '缺少 CSRF 参数，无法继续授权。'
    messageType.value = 'error'
    return
  }

  submitting.value = true
  message.value = ''

  if (!requiresConsent.value) {
    submitDeviceVerification()
    return
  }

  submitConsentApproval()
}

function submitDeviceVerification() {
  const form = document.createElement('form')
  form.method = 'post'
  form.action = `${authServerOrigin}/oauth2/device_verification`
  appendInput(form, 'user_code', normalizedUserCode.value)
  appendInput(form, csrfParameter.value, csrfToken.value)
  document.body.appendChild(form)
  form.submit()
}

function submitConsentApproval() {
  const iframeName = `device-verification-${Date.now()}`
  const iframe = document.createElement('iframe')
  iframe.name = iframeName
  iframe.style.display = 'none'
  iframe.addEventListener('load', () => handleSuccess('设备授权已提交成功，现在可以回到先前页面继续查看轮询结果。'), { once: true })
  document.body.appendChild(iframe)

  const form = document.createElement('form')
  form.method = 'post'
  form.action = `${authServerOrigin}/oauth2/device_verification`
  form.target = iframeName
  appendInput(form, 'client_id', clientId.value)
  appendInput(form, 'state', state.value)
  appendInput(form, 'user_code', normalizedUserCode.value)
  appendInput(form, csrfParameter.value, csrfToken.value)
  approvedScopes.value.forEach(scope => appendInput(form, 'scope', scope))
  document.body.appendChild(form)
  form.submit()

  setTimeout(() => {
    if (!success.value) {
      handleSuccess('设备授权已提交成功，现在可以回到先前页面继续查看轮询结果。')
    }
  }, 1200)
}

function resolveDeviceVerificationContext() {
  if (resolvingContext.value) return

  if (isExpired.value) {
    message.value = '该 Device Code 已过期，请回到设备演示页重新申请。'
    messageType.value = 'warning'
    return
  }

  if (!normalizedUserCode.value) {
    message.value = '请输入有效的 user_code 后再识别。'
    messageType.value = 'error'
    return
  }

  resolvingContext.value = true
  message.value = '正在识别验证码并加载客户端信息...'
  messageType.value = 'info'

  const targetUrl = new URL(`${authServerOrigin}/activate`)
  targetUrl.searchParams.set('user_code', normalizedUserCode.value)
  appendTimingParams(targetUrl)
  window.location.href = targetUrl.toString()
}

function denyConsent() {
  const fallback = '/flows?tab=device'
  sessionStorage.removeItem('oauth2_device_return_to')
  router.push(fallback)
}

function handleSuccess(successMessage = '设备授权已提交成功，现在可以回到先前页面继续查看轮询结果。') {
  if (success.value) return
  success.value = true
  submitting.value = false
  message.value = successMessage
  messageType.value = 'success'
}

function goBack() {
  const returnTo = normalizeReturnTo(sessionStorage.getItem('oauth2_device_return_to')) || '/flows?tab=device'
  sessionStorage.removeItem('oauth2_device_return_to')
  router.push(returnTo)
}

function goFlows() {
  sessionStorage.removeItem('oauth2_device_return_to')
  router.push('/flows?tab=device')
}

function appendInput(form, name, value) {
  const input = document.createElement('input')
  input.type = 'hidden'
  input.name = name
  input.value = value
  form.appendChild(input)
}

function getSingleValue(value) {
  return typeof value === 'string' ? value : Array.isArray(value) ? value[0] : ''
}

function normalizeUserCode(value) {
  if (typeof value !== 'string') return ''
  return value.trim().toUpperCase()
}

function normalizeScopes(value) {
  const raw = Array.isArray(value) ? value.flatMap(item => item.split(' ')) : typeof value === 'string' ? value.split(' ') : []
  return raw.map(item => item.trim()).filter(Boolean)
}

function normalizeReturnTo(value) {
  if (typeof value !== 'string' || !value.startsWith('/')) return ''
  return value.startsWith('//') ? '' : value
}

function hasSuccessFlag(value) {
  if (Array.isArray(value)) return value.some(hasSuccessFlag)
  return value === '' || value === 'true' || value === '1' || value === 'success'
}

function getDeviceFlowTiming(startedAtValue, expiresAtValue, userCode) {
  const startedAt = Number(startedAtValue)
  const expiresAt = Number(expiresAtValue)
  if (Number.isFinite(startedAt) && Number.isFinite(expiresAt)) {
    persistDeviceFlowTiming(userCode, startedAt, expiresAt)
    return { startedAt, expiresAt }
  }

  const persisted = readPersistedDeviceFlowTiming(userCode)
  if (persisted) {
    return persisted
  }

  return { startedAt: NaN, expiresAt: NaN }
}

function persistDeviceFlowTiming(userCode, startedAt, expiresAt) {
  if (typeof userCode !== 'string' || !userCode) return
  localStorage.setItem(getDeviceFlowStorageKey(userCode), JSON.stringify({ startedAt, expiresAt }))
}

function readPersistedDeviceFlowTiming(userCode) {
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

function getDeviceFlowStorageKey(userCode) {
  return `oauth2_device_flow_${userCode}`
}

function appendTimingParams(url) {
  if (!hasCountdown.value) return
  const startedAt = deviceFlowTiming.startedAt
  const expiresAtValue = expiresAt.value
  if (Number.isFinite(startedAt) && startedAt > 0) {
    url.searchParams.set('started_at', String(startedAt))
  }
  if (Number.isFinite(expiresAtValue) && expiresAtValue > 0) {
    url.searchParams.set('expires_at', String(expiresAtValue))
  }
}
</script>

<style scoped>
.oauth-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.oauth-card {
  width: min(760px, 100%);
}
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.page-subtitle {
  margin-top: 6px;
  color: #606266;
}
.mb16 {
  margin-bottom: 16px;
}
.countdown-box {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 10px;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}
.countdown-box.expired {
  background: #fef0f0;
  border-color: #fbc4c4;
}
.countdown-label {
  color: #606266;
  font-size: 13px;
}
.countdown-value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
}
.success-state {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 10px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
}
.success-text {
  margin: 0;
  color: #2f5d24;
  line-height: 1.6;
}
.scope-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.scope-tag {
  margin: 0;
}
.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
