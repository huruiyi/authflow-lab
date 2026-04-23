<template>
  <OAuth2Layout>
    <div class="device-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="适用场景">智能电视、IoT 设备、无浏览器输入能力的终端</el-descriptions-item>
          <el-descriptions-item label="Client">device-flow-client</el-descriptions-item>
          <el-descriptions-item label="特点">设备先申请 device_code，用户在另一终端访问 verification_uri 完成授权</el-descriptions-item>
          <el-descriptions-item label="默认 Scope">profile read（当前设备流演示不申请 openid）</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="info-card">
        <el-form :model="deviceForm" inline class="form-row">
          <el-form-item label="client_id">
            <el-input v-model="deviceForm.clientId" style="width: 220px"/>
          </el-form-item>
          <el-form-item label="client_secret">
            <el-input v-model="deviceForm.clientSecret" show-password style="width: 220px"/>
          </el-form-item>
          <el-form-item label="scope">
            <el-input v-model="deviceForm.scope" style="width: 220px"/>
          </el-form-item>
        </el-form>

        <div class="actions-row">
          <el-button type="primary" :icon="Key" @click="startDeviceFlow">申请 Device Code</el-button>
          <el-button
              type="success"
              :disabled="!deviceAuth.device_code || isPollingDeviceToken"
              :loading="isPollingDeviceToken"
              @click="pollDeviceToken"
          >{{ isPollingDeviceToken ? '轮询中...' : '轮询 Token' }}
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <span>Device 授权信息</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="device_code">
            <div class="token-box">{{ deviceAuth.device_code || '暂无' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="user_code">
            <div class="user-code">{{ deviceAuth.user_code || '暂无' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="verification_uri">
            <div class="uri-box">
              {{ deviceAuth.verification_uri || '暂无' }}
              <el-button v-if="deviceAuth.verification_uri" size="small" @click="openVerificationUri">打开</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="verification_uri_complete">
            <div class="uri-box">
              {{ deviceAuth.verification_uri_complete || '暂无' }}
              <el-button v-if="deviceAuth.verification_uri_complete" size="small" @click="openVerificationUriComplete">打开</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="interval">{{ deviceAuth.interval || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <TokenDisplay
          :access-token="tokenState.accessToken"
          :refresh-token="tokenState.refreshToken"
          :scope="tokenState.scope"
          title="获取的 Token"
      />

      <ApiResultBox :result="result"/>
    </div>
  </OAuth2Layout>
</template>

<script setup>
import {onBeforeUnmount, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import TokenDisplay from '../components/TokenDisplay.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import {oauth2Api} from '../api/oauth2'
import {delay, generateBasicAuth, handleOAuth2Error} from '../utils/oauth2Helper'
import {decorateDeviceAuth, isDeviceFlowExpired} from '../utils/deviceFlowHelper'
import {getTokenState, saveTokens} from '../utils/tokenHelper'

const result = ref({message: '点击上方按钮开始体验 OAuth2 场景。'})
const tokenState = reactive(getTokenState())
const deviceAuth = ref({})
const isPollingDeviceToken = ref(false)

let devicePollingAborted = false
const DEVICE_FLOW_TIMEOUT_MS = 2 * 60 * 1000

const deviceForm = reactive({
  clientId: 'device-flow-client',
  clientSecret: 'device-secret',
  scope: 'profile read'
})

onBeforeUnmount(() => {
  devicePollingAborted = true
})

async function startDeviceFlow() {
  try {
    devicePollingAborted = false
    isPollingDeviceToken.value = false
    const basic = generateBasicAuth(deviceForm.clientId, deviceForm.clientSecret)
    sessionStorage.setItem('oauth2_device_return_to', '/device')
    const {data} = await oauth2Api.deviceAuthorize({
      client_id: deviceForm.clientId,
      scope: deviceForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    deviceAuth.value = decorateDeviceAuth(data)
    result.value = deviceAuth.value
    ElMessage.success('设备码申请成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function pollDeviceToken() {
  if (!deviceAuth.value.device_code || isPollingDeviceToken.value) return

  isPollingDeviceToken.value = true
  devicePollingAborted = false
  const startedAt = deviceAuth.value.started_at || Date.now()
  let waitMs = (deviceAuth.value.interval || 5) * 1000

  try {
    while (!devicePollingAborted) {
      const basic = generateBasicAuth(deviceForm.clientId, deviceForm.clientSecret)
      try {
        const {data} = await oauth2Api.pollDeviceToken({
          grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
          device_code: deviceAuth.value.device_code,
          client_id: deviceForm.clientId
        }, {
          Authorization: `Basic ${basic}`
        })
        result.value = data
        if (data.access_token) {
          saveTokens(data)
          Object.assign(tokenState, getTokenState())
          ElMessage.success('设备授权成功，已获取 Token')
          return
        }
      } catch (e) {
        const errorCode = e.response?.data?.error
        if (errorCode === 'authorization_pending') {
          result.value = {
            error: errorCode,
            message: '用户还没有在 verification_uri 完成授权，系统会持续轮询直到获取结果或 2 分钟超时。',
            verificationUri: deviceAuth.value.verification_uri,
            verificationUriComplete: deviceAuth.value.verification_uri_complete,
            userCode: deviceAuth.value.user_code,
            interval: deviceAuth.value.interval,
            timeoutSeconds: DEVICE_FLOW_TIMEOUT_MS / 1000,
            expiresAt: deviceAuth.value.expires_at
          }
        } else if (errorCode === 'slow_down') {
          waitMs += 5000
          result.value = {
            error: errorCode,
            message: '服务端要求降低轮询频率，系统会自动放慢重试。',
            interval: Math.ceil(waitMs / 1000)
          }
        } else {
          result.value = handleOAuth2Error(e, ElMessage.error)
          return
        }
      }

      if (isDeviceFlowExpired(deviceAuth.value)) {
        result.value = {
          error: 'polling_timeout',
          message: '自申请 Device Code 成功起 2 分钟内仍未获取到设备授权结果，请重新申请。',
          verificationUri: deviceAuth.value.verification_uri,
          verificationUriComplete: deviceAuth.value.verification_uri_complete,
          userCode: deviceAuth.value.user_code,
          interval: Math.ceil(waitMs / 1000),
          expiresAt: deviceAuth.value.expires_at
        }
        ElMessage.warning('设备码已超时，请重新申请 Device Code')
        return
      }

      await delay(waitMs)
    }
  } finally {
    isPollingDeviceToken.value = false
  }
}

function openVerificationUri() {
  if (deviceAuth.value.verification_uri) {
    window.open(deviceAuth.value.verification_uri, '_blank')
  }
}

function openVerificationUriComplete() {
  if (deviceAuth.value.verification_uri_complete) {
    window.open(deviceAuth.value.verification_uri_complete, '_blank')
  }
}
</script>

<style scoped>
.device-container {
  padding: 12px 0;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.form-row {
  margin-bottom: 20px;
}

.actions-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.token-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  max-height: 100px;
  overflow-y: auto;
}

.user-code {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  letter-spacing: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.uri-box {
  display: flex;
  align-items: center;
  gap: 8px;
  word-break: break-all;
}
</style>