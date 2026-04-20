<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24" color="#fff"><Connection /></el-icon>
        <span class="title">OAuth2 真实使用场景演示</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="goClients">Client 管理</el-button>
        <el-button type="default" size="small" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-main>
      <el-alert
        title="这个页面不只是管理 Client，而是演示 OAuth2 在真实业务中的使用：用户登录授权、服务间调用、携带 Token 访问受保护资源。"
        type="info"
        show-icon
        :closable="false"
        class="mb16"
      />

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="1. Authorization Code + PKCE" name="pkce">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">SPA / Vue / React / 移动端公开客户端</el-descriptions-item>
            <el-descriptions-item label="Client">spa-public-client</el-descriptions-item>
            <el-descriptions-item label="特点">无 client_secret，前端生成 PKCE，跳转授权页，换取 access_token / refresh_token</el-descriptions-item>
          </el-descriptions>

          <div class="actions-row">
            <el-button type="primary" @click="startPkceFlow">发起授权登录</el-button>
            <el-button @click="loadDiscovery">查看 Discovery 文档</el-button>
            <el-button @click="loadJwks">查看 JWK Set</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>当前 Token</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="access_token">
                <div class="token-box">{{ accessToken || '暂无' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="refresh_token">
                <div class="token-box">{{ refreshToken || '暂无' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="scope">{{ currentScope || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <div class="actions-row mt16">
            <el-button @click="callPublic">访问公开资源</el-button>
            <el-button type="success" @click="callProfile" :disabled="!accessToken">读取用户 Profile</el-button>
            <el-button type="success" @click="callUserInfo" :disabled="!accessToken">调用 UserInfo</el-button>
            <el-button type="warning" @click="callRead" :disabled="!accessToken">读取业务资源</el-button>
            <el-button type="danger" @click="callWrite" :disabled="!accessToken">写入业务资源</el-button>
            <el-button @click="callTokenInfo" :disabled="!accessToken">查看 Token Claims</el-button>
            <el-button type="primary" plain @click="doRefreshToken" :disabled="!refreshToken">刷新 Access Token</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="2. Client Credentials" name="m2m">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">微服务 / BFF / 定时任务 / 后端到后端 API</el-descriptions-item>
            <el-descriptions-item label="说明">可直接切换不同客户端，观察 client_credentials 下 read / write 权限是否生效</el-descriptions-item>
            <el-descriptions-item label="资源权限">/resource/read 需要 read，/resource/write 需要 write</el-descriptions-item>
          </el-descriptions>

          <el-form :model="m2mForm" inline class="mt16">
            <el-form-item label="选择客户端">
              <el-select v-model="m2mForm.selectedClientKey" style="width: 260px" @change="handleM2mClientChange">
                <el-option
                  v-for="client in m2mClients"
                  :key="client.key"
                  :label="client.label"
                  :value="client.key"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="client_id">
              <el-input v-model="m2mForm.clientId" style="width: 220px" />
            </el-form-item>
            <el-form-item label="client_secret">
              <el-input v-model="m2mForm.clientSecret" show-password style="width: 220px" />
            </el-form-item>
            <el-form-item label="scope">
              <el-input v-model="m2mForm.scope" style="width: 220px" />
            </el-form-item>
          </el-form>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            :title="m2mSelectedClient.description"
          />

          <div class="actions-row">
            <el-button type="primary" @click="getM2mToken">获取服务 Token</el-button>
            <el-button type="success" :disabled="!m2mToken" @click="callReadWithM2m">调用只读资源</el-button>
            <el-button type="danger" :disabled="!m2mToken" @click="callWriteWithM2m">调用写资源</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>M2M Token</span></template>
            <div class="token-box">{{ m2mToken || '暂无' }}</div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="3. Device Code" name="device">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">智能电视、IoT 设备、无浏览器输入能力的终端</el-descriptions-item>
            <el-descriptions-item label="Client">device-flow-client</el-descriptions-item>
            <el-descriptions-item label="特点">设备先申请 device_code，用户在另一终端访问 verification_uri 完成授权</el-descriptions-item>
          </el-descriptions>

          <el-form :model="deviceForm" inline class="mt16">
            <el-form-item label="client_id">
              <el-input v-model="deviceForm.clientId" style="width: 220px" />
            </el-form-item>
            <el-form-item label="client_secret">
              <el-input v-model="deviceForm.clientSecret" show-password style="width: 220px" />
            </el-form-item>
            <el-form-item label="scope">
              <el-input v-model="deviceForm.scope" style="width: 220px" />
            </el-form-item>
          </el-form>

          <div class="actions-row">
            <el-button type="primary" @click="startDeviceFlow">申请 Device Code</el-button>
            <el-button type="success" :disabled="!deviceAuth.device_code" @click="pollDeviceToken">轮询 Token</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>Device 授权信息</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="device_code"><div class="token-box">{{ deviceAuth.device_code || '暂无' }}</div></el-descriptions-item>
              <el-descriptions-item label="user_code">{{ deviceAuth.user_code || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="verification_uri">{{ deviceAuth.verification_uri || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="verification_uri_complete">{{ deviceAuth.verification_uri_complete || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="interval">{{ deviceAuth.interval || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="4. 场景说明" name="scenes">
          <el-timeline>
            <el-timeline-item timestamp="前后端分离登录" placement="top">
              用户在 Vue 前端点击登录，跳转授权服务器，登录并授权后回调前端，前端使用 code + code_verifier 换取 token。
            </el-timeline-item>
            <el-timeline-item timestamp="UserInfo" placement="top">
              前端拿到 access_token 后，调用 OIDC UserInfo 端点，读取标准用户声明，如 sub、preferred_username、email。
            </el-timeline-item>
            <el-timeline-item timestamp="Refresh Token" placement="top">
              access_token 过期前后，客户端可通过 refresh_token 向 token 端点换取新的 access_token。
            </el-timeline-item>
            <el-timeline-item timestamp="业务 API 权限控制" placement="top">
              资源服务器根据 scope 控制 read / write 权限：[ResourceServerConfig.java:19-25](backend/src/main/java/com/demo/authserver/config/ResourceServerConfig.java#L19-L25)。
            </el-timeline-item>
            <el-timeline-item timestamp="设备码流程" placement="top">
              设备获取 device_code 和 user_code，提示用户去浏览器输入 user_code 完成授权，设备再轮询 token 端点。
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>

      <el-card shadow="never" class="mt16">
        <template #header><span>接口响应 / 调试输出</span></template>
        <pre class="result-box">{{ prettyResult }}</pre>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import { authApi } from '../api/auth'
import { oauth2Api } from '../api/oauth2'
import { generatePkcePair, generateRandomString } from '../composables/pkce'

const m2mClients = [
  {
    key: 'm2m-service-client-rw',
    label: 'm2m-service-client（read + write）',
    clientId: 'm2m-service-client',
    clientSecret: 'm2m-secret',
    scope: 'read write',
    description: '这个客户端申请 read write scope。获取 token 后读取和写入资源都应该成功。'
  },
  {
    key: 'm2m-service-client-read',
    label: 'm2m-service-client（仅 read）',
    clientId: 'm2m-service-client',
    clientSecret: 'm2m-secret',
    scope: 'read',
    description: '这个客户端只申请 read scope。获取 token 后读取资源应成功，写入资源应返回 403。'
  },
  {
    key: 'all-in-one-client-rw',
    label: 'all-in-one-client（read + write）',
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    description: '这个综合测试客户端支持 client_credentials，并申请 read write scope，读取和写入资源都应该成功。'
  }
]

const router = useRouter()
const activeTab = ref('pkce')
const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const accessToken = ref(sessionStorage.getItem('oauth2_access_token') || '')
const refreshToken = ref(sessionStorage.getItem('oauth2_refresh_token') || '')
const currentScope = ref(sessionStorage.getItem('oauth2_scope') || '')
const m2mToken = ref('')
const deviceAuth = ref({})

const m2mForm = reactive({
  selectedClientKey: m2mClients[0].key,
  clientId: m2mClients[0].clientId,
  clientSecret: m2mClients[0].clientSecret,
  scope: m2mClients[0].scope
})

const deviceForm = reactive({
  clientId: 'device-flow-client',
  clientSecret: 'device-secret',
  scope: 'openid profile read'
})

const prettyResult = computed(() => JSON.stringify(result.value, null, 2))
const m2mSelectedClient = computed(
  () => m2mClients.find(client => client.key === m2mForm.selectedClientKey) || m2mClients[0]
)

async function startPkceFlow() {
  const clientId = 'spa-public-client'
  const frontendOrigin = window.location.origin
  const authServerOrigin = 'http://localhost:9000'
  const redirectUri = `${frontendOrigin}/callback`
  const state = generateRandomString(32)
  const { codeVerifier, codeChallenge } = await generatePkcePair()

  sessionStorage.setItem('oauth2_state', state)
  sessionStorage.setItem('pkce_code_verifier', codeVerifier)
  sessionStorage.setItem('oauth2_client_id', clientId)

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'openid profile email read write',
    state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })

  window.location.href = `${authServerOrigin}/oauth2/authorize?${params.toString()}`
}

async function loadDiscovery() {
  try {
    const { data } = await oauth2Api.getDiscovery()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function loadJwks() {
  try {
    const { data } = await oauth2Api.getJwks()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callPublic() {
  try {
    const { data } = await oauth2Api.callPublicResource()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callProfile() {
  try {
    const { data } = await oauth2Api.callProfileResource(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callUserInfo() {
  try {
    const { data } = await oauth2Api.getUserInfo(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callRead() {
  try {
    const { data } = await oauth2Api.callReadResource(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callWrite() {
  try {
    const { data } = await oauth2Api.callWriteResource(accessToken.value, {
      orderNo: 'ORD-' + Date.now(),
      amount: 299,
      status: 'PAID'
    })
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callTokenInfo() {
  try {
    const { data } = await oauth2Api.callTokenInfo(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function doRefreshToken() {
  const clientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  try {
    const { data } = await oauth2Api.refreshToken({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: refreshToken.value
    })
    accessToken.value = data.access_token
    sessionStorage.setItem('oauth2_access_token', data.access_token)
    if (data.refresh_token) {
      refreshToken.value = data.refresh_token
      sessionStorage.setItem('oauth2_refresh_token', data.refresh_token)
    }
    if (data.scope) {
      currentScope.value = data.scope
      sessionStorage.setItem('oauth2_scope', data.scope)
    }
    result.value = data
    ElMessage.success('refresh_token 刷新成功')
  } catch (e) {
    showError(e)
  }
}

async function getM2mToken() {
  try {
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      scope: m2mForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    m2mToken.value = data.access_token
    result.value = {
      clientId: m2mForm.clientId,
      requestedScope: m2mForm.scope,
      selectedClient: m2mSelectedClient.value.label,
      ...data
    }
  } catch (e) {
    showError(e)
  }
}

function handleM2mClientChange(selectedKey) {
  const selectedClient = m2mClients.find(client => client.key === selectedKey)
  if (!selectedClient) return
  m2mForm.clientId = selectedClient.clientId
  m2mForm.clientSecret = selectedClient.clientSecret
  m2mForm.scope = selectedClient.scope
  m2mToken.value = ''
  result.value = {
    message: `已切换到 ${selectedClient.label}`,
    expectedBehavior: selectedClient.description,
    clientId: selectedClient.clientId,
    scope: selectedClient.scope
  }
}

async function callReadWithM2m() {
  try {
    const { data } = await oauth2Api.callReadResource(m2mToken.value)
    result.value = {
      clientId: m2mForm.clientId,
      operation: 'read',
      expected: '需要 read scope',
      actual: data
    }
  } catch (e) {
    showError(e)
  }
}

async function callWriteWithM2m() {
  try {
    const { data } = await oauth2Api.callWriteResource(m2mToken.value, {
      batchNo: 'SYNC-' + Date.now(),
      operation: 'inventory_update',
      source: 'scheduler-job'
    })
    result.value = {
      clientId: m2mForm.clientId,
      operation: 'write',
      expected: '需要 write scope',
      actual: data
    }
  } catch (e) {
    showError(e)
  }
}

async function startDeviceFlow() {
  try {
    const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
    const { data } = await oauth2Api.deviceAuthorize({
      client_id: deviceForm.clientId,
      scope: deviceForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    deviceAuth.value = data
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function pollDeviceToken() {
  try {
    const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
    const { data } = await oauth2Api.pollDeviceToken({
      grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
      device_code: deviceAuth.value.device_code,
      client_id: deviceForm.clientId
    }, {
      Authorization: `Basic ${basic}`
    })
    result.value = data
    if (data.access_token) {
      accessToken.value = data.access_token
      sessionStorage.setItem('oauth2_access_token', data.access_token)
    }
    if (data.refresh_token) {
      refreshToken.value = data.refresh_token
      sessionStorage.setItem('oauth2_refresh_token', data.refresh_token)
    }
    if (data.scope) {
      currentScope.value = data.scope
      sessionStorage.setItem('oauth2_scope', data.scope)
    }
  } catch (e) {
    showError(e)
  }
}

async function handleLogout() {
  await authApi.logout().catch(() => {})
  sessionStorage.removeItem('oauth2_access_token')
  sessionStorage.removeItem('oauth2_refresh_token')
  sessionStorage.removeItem('oauth2_scope')
  router.push('/login')
}

function goClients() {
  router.push('/clients')
}

function showError(e) {
  result.value = e.response?.data || { error: e.message }
  ElMessage.error(e.response?.data?.error || e.message)
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-right {
  display: flex;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}
.mb16 {
  margin-bottom: 16px;
}
.mt16 {
  margin-top: 16px;
}
.actions-row {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.token-box,
.result-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: monospace;
}
.result-box {
  margin: 0;
}
</style>
