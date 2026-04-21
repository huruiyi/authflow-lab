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

      <el-descriptions :column="1" border class="mb16">
        <el-descriptions-item label="client_id">{{ clientDisplayText }}</el-descriptions-item>
        <el-descriptions-item label="user_code">{{ userCode || '缺失' }}</el-descriptions-item>
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
        <el-form-item>
          <el-checkbox-group v-model="approvedScopes">
            <el-checkbox v-for="scope in scopes" :key="scope" :label="scope">
              允许访问 {{ scope }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="actions">
          <el-button type="primary" :loading="submitting" @click="approveConsent">确认授权</el-button>
          <el-button :loading="submitting" @click="denyConsent">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const authServerOrigin = import.meta.env.VITE_BACKEND_ORIGIN || `${window.location.protocol}//${window.location.hostname}:30000`
const submitting = ref(false)
const success = ref(false)
const message = ref('')
const messageType = ref('info')
const clientId = computed(() => getSingleValue(route.query.client_id))
const state = computed(() => getSingleValue(route.query.state))
const userCode = computed(() => getSingleValue(route.query.user_code))
const csrfToken = computed(() => getSingleValue(route.query._csrf))
const csrfParameter = computed(() => getSingleValue(route.query._csrf_parameter) || '_csrf')
const scopes = computed(() => normalizeScopes(route.query.scope))
const approvedScopes = ref([...scopes.value])
const requiresConsent = computed(() => Boolean(clientId.value && state.value))
const clientDisplayText = computed(() => {
  if (clientId.value) return clientId.value
  return requiresConsent.value ? '待确认客户端' : '设备验证码校验中'
})
const scopeDisplayText = computed(() => {
  return requiresConsent.value ? '未申请额外 scope' : '提交验证码后展示授权 scope'
})

function approveConsent() {
  if (!userCode.value) {
    message.value = '缺少 user_code 参数，无法继续授权。'
    messageType.value = 'error'
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
  appendInput(form, 'user_code', userCode.value)
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
  appendInput(form, 'user_code', userCode.value)
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

function normalizeScopes(value) {
  const raw = Array.isArray(value) ? value.flatMap(item => item.split(' ')) : typeof value === 'string' ? value.split(' ') : []
  return raw.map(item => item.trim()).filter(Boolean)
}

function normalizeReturnTo(value) {
  if (typeof value !== 'string' || !value.startsWith('/')) return ''
  return value.startsWith('//') ? '' : value
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
