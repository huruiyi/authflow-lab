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
            <el-button type="warning" plain @click="startScopeConsentDemo">演示 Scope 缩减授权</el-button>
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
            <el-button type="info" plain @click="startOidcLogout" :disabled="!idToken">OIDC Logout</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="2. Client Credentials" name="m2m">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">微服务 / BFF / 定时任务 / 后端到后端 API</el-descriptions-item>
            <el-descriptions-item label="说明">可直接切换不同客户端，观察 client_credentials 下 read / write 权限是否生效</el-descriptions-item>
            <el-descriptions-item label="资源权限">/resource/read 需要 read，/resource/write 需要 write</el-descriptions-item>
            <el-descriptions-item label="生命周期">支持演示 introspection 与 revocation，观察 token 激活状态变化</el-descriptions-item>
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
            <el-button :disabled="!m2mLifecycleToken" @click="introspectM2mToken">Introspect Token</el-button>
            <el-button type="warning" :disabled="!m2mToken" @click="revokeM2mToken">Revoke Token</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>M2M Token</span></template>
            <div class="token-box">{{ m2mToken || '暂无' }}</div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="3. Client 认证方式差异" name="client-auth">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">对比 client_secret_basic、client_secret_post、none 三种 token endpoint 客户端认证模型</el-descriptions-item>
            <el-descriptions-item label="重点理解">机密客户端要证明“我是谁”，公开客户端则不保存 secret，而是依赖 PKCE</el-descriptions-item>
            <el-descriptions-item label="当前客户端">all-in-one-client 同时支持 basic/post；spa-public-client 代表公开客户端</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            title="同一个 token endpoint，机密客户端可以通过 Header 或 Body 认证；公开客户端不应持有 client_secret。"
          />

          <el-row :gutter="16" class="mt16">
            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>机密客户端：basic / post</span></template>
                <el-form :model="clientAuthForm" label-position="top">
                  <el-form-item label="client_id">
                    <el-input v-model="clientAuthForm.confidentialClientId" />
                  </el-form-item>
                  <el-form-item label="client_secret">
                    <el-input v-model="clientAuthForm.confidentialClientSecret" show-password />
                  </el-form-item>
                  <el-form-item label="scope">
                    <el-input v-model="clientAuthForm.confidentialScope" />
                  </el-form-item>
                </el-form>

                <div class="actions-row">
                  <el-button type="primary" @click="getClientAuthMethodToken('basic')">用 Basic 获取 Token</el-button>
                  <el-button type="success" @click="getClientAuthMethodToken('post')">用 Post 获取 Token</el-button>
                </div>

                <el-descriptions :column="1" border class="mt16">
                  <el-descriptions-item label="Basic Token">
                    <div class="token-box">{{ maskToken(clientAuthTokens.basic) || '暂无' }}</div>
                  </el-descriptions-item>
                  <el-descriptions-item label="Post Token">
                    <div class="token-box">{{ maskToken(clientAuthTokens.post) || '暂无' }}</div>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>公开客户端：none + PKCE</span></template>
                <el-form :model="clientAuthForm" label-position="top">
                  <el-form-item label="client_id">
                    <el-input v-model="clientAuthForm.publicClientId" />
                  </el-form-item>
                  <el-form-item label="scope">
                    <el-input v-model="clientAuthForm.publicScope" />
                  </el-form-item>
                </el-form>

                <div class="actions-row">
                  <el-button type="warning" plain @click="startPublicClientAuthDemo">公开客户端发起 PKCE 登录</el-button>
                  <el-button type="danger" plain @click="tryPublicClientCredentials">错误示例：public client 用 client_credentials</el-button>
                </div>

                <el-alert
                  class="mt16"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="公开客户端没有 client_secret，因此不能像服务端那样用 client_credentials 直接向 token 端点换服务 Token。"
                />
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" class="mt16">
            <template #header><span>三种认证方式对比</span></template>
            <el-table :data="clientAuthComparisonRows" border>
              <el-table-column prop="dimension" label="对比维度" min-width="180" />
              <el-table-column prop="basic" label="client_secret_basic" min-width="220" show-overflow-tooltip />
              <el-table-column prop="post" label="client_secret_post" min-width="220" show-overflow-tooltip />
              <el-table-column prop="none" label="none" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card shadow="never" class="mt16">
            <template #header><span>差异结论</span></template>
            <div class="actions-row">
              <el-button @click="writeClientAuthSummary">输出当前客户端认证总结</el-button>
            </div>
            <el-alert
              v-for="highlight in clientAuthHighlights"
              :key="highlight"
              class="mt16"
              type="success"
              show-icon
              :closable="false"
              :title="highlight"
            />
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="4. Device Code" name="device">
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
            <el-button
              type="success"
              :disabled="!deviceAuth.device_code || isPollingDeviceToken"
              :loading="isPollingDeviceToken"
              @click="pollDeviceToken"
            >{{ isPollingDeviceToken ? '轮询中...' : '轮询 Token' }}</el-button>
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

        <el-tab-pane label="5. JWT Claims 差异" name="claims">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">并排对比用户 access_token、用户 id_token、机器 access_token 的 claims 差异</el-descriptions-item>
            <el-descriptions-item label="重点观察">sub、client_id、scope、aud、preferred_username、email、authorities 等字段</el-descriptions-item>
            <el-descriptions-item label="准备条件">先完成一次 PKCE 登录，再获取一次 Client Credentials token，下面的对比表才会完整</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            title="id_token 更偏身份声明，access_token 更偏资源访问授权；client_credentials token 通常没有用户身份字段。"
          />

          <div class="actions-row">
            <el-button @click="writeClaimsComparisonResult" :disabled="!hasAnyJwtForComparison">输出当前 Claims 差异总结</el-button>
          </div>

          <el-row :gutter="16" class="mt16">
            <el-col :xs="24" :md="8">
              <el-card shadow="never">
                <template #header><span>用户 Access Token</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="状态">{{ decodedUserAccessToken ? '已获取' : '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="sub">{{ summarizeClaim(decodedUserAccessToken?.payload?.sub) }}</el-descriptions-item>
                  <el-descriptions-item label="scope">{{ summarizeClaim(decodedUserAccessToken?.payload?.scope) }}</el-descriptions-item>
                  <el-descriptions-item label="client_id">{{ summarizeClaim(decodedUserAccessToken?.payload?.client_id) }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ maskToken(accessToken) || '暂无' }}</div> </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-card shadow="never">
                <template #header><span>用户 ID Token</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="状态">{{ decodedIdToken ? '已获取' : '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="sub">{{ summarizeClaim(decodedIdToken?.payload?.sub) }}</el-descriptions-item>
                  <el-descriptions-item label="aud">{{ summarizeClaim(decodedIdToken?.payload?.aud) }}</el-descriptions-item>
                  <el-descriptions-item label="preferred_username">{{ summarizeClaim(decodedIdToken?.payload?.preferred_username) }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ maskToken(idToken) || '暂无' }}</div> </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-card shadow="never">
                <template #header><span>机器 Access Token</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="状态">{{ decodedMachineAccessToken ? '已获取' : '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="sub">{{ summarizeClaim(decodedMachineAccessToken?.payload?.sub) }}</el-descriptions-item>
                  <el-descriptions-item label="scope">{{ summarizeClaim(decodedMachineAccessToken?.payload?.scope) }}</el-descriptions-item>
                  <el-descriptions-item label="client_id">{{ summarizeClaim(decodedMachineAccessToken?.payload?.client_id) }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ maskToken(m2mLifecycleToken) || '暂无' }}</div> </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" class="mt16">
            <template #header><span>关键 Claims 对比</span></template>
            <el-table :data="claimsComparisonRows" border empty-text="先获取用户 token 或 M2M token，再查看对比结果。">
              <el-table-column prop="claim" label="Claim" min-width="180" />
              <el-table-column prop="userAccessToken" label="用户 access_token" min-width="220" show-overflow-tooltip />
              <el-table-column prop="idToken" label="用户 id_token" min-width="220" show-overflow-tooltip />
              <el-table-column prop="machineAccessToken" label="机器 access_token" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card shadow="never" class="mt16">
            <template #header><span>差异结论</span></template>
            <el-alert
              v-for="highlight in claimsHighlights"
              :key="highlight"
              class="mb16"
              type="success"
              show-icon
              :closable="false"
              :title="highlight"
            />
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="6. 场景说明" name="scenes">
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
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import { authApi } from '../api/auth'
import { oauth2Api } from '../api/oauth2'
import { generatePkcePair, generateRandomString } from '../composables/pkce'

const authServerOrigin = import.meta.env.VITE_BACKEND_ORIGIN || `${window.location.protocol}//${window.location.hostname}:30000`

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

const clientAuthDemoClients = {
  basic: {
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    transport: 'HTTP Header'
  },
  post: {
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    transport: 'Form Body'
  },
  none: {
    clientId: 'spa-public-client',
    clientSecret: '',
    scope: 'openid profile email read write',
    transport: 'No secret'
  }
}

const route = useRoute()
const router = useRouter()
const activeTab = ref(['client-auth', 'device', 'claims'].includes(route.query.tab) ? route.query.tab : 'pkce')
const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const accessToken = ref(sessionStorage.getItem('oauth2_access_token') || '')
const idToken = ref(sessionStorage.getItem('oauth2_id_token') || '')
const refreshToken = ref(sessionStorage.getItem('oauth2_refresh_token') || '')
const currentScope = ref(sessionStorage.getItem('oauth2_scope') || '')
const m2mToken = ref('')
const m2mLastIssuedToken = ref('')
const deviceAuth = ref({})
const isPollingDeviceToken = ref(false)
let devicePollingAborted = false

const DEVICE_FLOW_TIMEOUT_MS = 2 * 60 * 1000
const DEFAULT_DEVICE_INTERVAL_SECONDS = 5

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

const clientAuthForm = reactive({
  confidentialClientId: clientAuthDemoClients.basic.clientId,
  confidentialClientSecret: clientAuthDemoClients.basic.clientSecret,
  confidentialScope: clientAuthDemoClients.basic.scope,
  publicClientId: clientAuthDemoClients.none.clientId,
  publicScope: clientAuthDemoClients.none.scope
})

const clientAuthTokens = reactive({
  basic: '',
  post: ''
})

const prettyResult = computed(() => JSON.stringify(result.value, null, 2))
const m2mSelectedClient = computed(
  () => m2mClients.find(client => client.key === m2mForm.selectedClientKey) || m2mClients[0]
)
const m2mLifecycleToken = computed(() => m2mToken.value || m2mLastIssuedToken.value)
const clientAuthComparisonRows = computed(() => {
  const basicConfig = clientAuthDemoClients.basic
  const postConfig = clientAuthDemoClients.post
  const noneConfig = clientAuthDemoClients.none

  return [
    {
      dimension: '是否携带 client_secret',
      basic: '是，放在 Basic Header 中',
      post: '是，放在表单 body 中',
      none: '否，公开客户端不保存密钥'
    },
    {
      dimension: '典型 grant_type',
      basic: 'client_credentials / authorization_code',
      post: 'authorization_code / client_credentials（服务端允许时）',
      none: 'authorization_code + PKCE'
    },
    {
      dimension: '当前演示客户端',
      basic: basicConfig.clientId,
      post: postConfig.clientId,
      none: noneConfig.clientId
    },
    {
      dimension: '请求位置',
      basic: basicConfig.transport,
      post: postConfig.transport,
      none: noneConfig.transport
    },
    {
      dimension: '适用对象',
      basic: '机密客户端 / 服务端应用 / 服务间调用',
      post: '机密客户端，但运行环境不便设置 Authorization Header',
      none: 'SPA / 移动端 / 原生 App'
    }
  ]
})
const clientAuthHighlights = computed(() => {
  const highlights = [
    'client_secret_basic 与 client_secret_post 都属于机密客户端认证，只是密钥传输位置不同。',
    'none 不是“更弱的 basic”，而是公开客户端模型：不保存密钥，靠 PKCE 证明是同一发起方。'
  ]

  if (clientAuthTokens.basic && clientAuthTokens.post) {
    highlights.push('当前 Basic 与 Post 都已成功拿到 token，说明服务端同时接受两种机密客户端认证方式。')
  }

  if (!clientAuthTokens.basic && !clientAuthTokens.post) {
    highlights.push('先分别点一次 Basic / Post 获取 token，再观察机密客户端两种认证方式的差异。')
  }

  return highlights
})

watch(activeTab, (tab) => {
  const nextQuery = { ...route.query }
  if (['client-auth', 'device', 'claims'].includes(tab)) {
    nextQuery.tab = tab
  } else {
    delete nextQuery.tab
  }
  router.replace({ query: nextQuery })
})

const decodedUserAccessToken = computed(() => decodeJwt(accessToken.value))
const decodedIdToken = computed(() => decodeJwt(idToken.value))
const decodedMachineAccessToken = computed(() => decodeJwt(m2mLifecycleToken.value))
const hasAnyJwtForComparison = computed(() => Boolean(
  decodedUserAccessToken.value || decodedIdToken.value || decodedMachineAccessToken.value
))
const claimsComparisonRows = computed(() => {
  const userClaims = decodedUserAccessToken.value?.payload || {}
  const idClaims = decodedIdToken.value?.payload || {}
  const machineClaims = decodedMachineAccessToken.value?.payload || {}

  return [
    buildClaimsRow('sub', userClaims.sub, idClaims.sub, machineClaims.sub),
    buildClaimsRow('client_id', userClaims.client_id, idClaims.client_id, machineClaims.client_id),
    buildClaimsRow('scope', userClaims.scope, idClaims.scope, machineClaims.scope),
    buildClaimsRow('aud', userClaims.aud, idClaims.aud, machineClaims.aud),
    buildClaimsRow('iss', userClaims.iss, idClaims.iss, machineClaims.iss),
    buildClaimsRow('preferred_username', userClaims.preferred_username, idClaims.preferred_username, machineClaims.preferred_username),
    buildClaimsRow('email', userClaims.email, idClaims.email, machineClaims.email),
    buildClaimsRow('authorities', userClaims.authorities, idClaims.authorities, machineClaims.authorities),
    buildClaimsRow('azp', userClaims.azp, idClaims.azp, machineClaims.azp),
    buildClaimsRow('sid', userClaims.sid, idClaims.sid, machineClaims.sid)
  ]
})
const claimsHighlights = computed(() => buildClaimsHighlights({
  userAccessToken: decodedUserAccessToken.value,
  idToken: decodedIdToken.value,
  machineAccessToken: decodedMachineAccessToken.value
}))

onBeforeUnmount(() => {
  devicePollingAborted = true
})

async function startPkceFlow() {
  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write'
  })
}

async function startScopeConsentDemo() {
  result.value = {
    operation: 'scope_consent_demo',
    message: '即将使用 spa-consent-client 发起授权。进入授权确认页后，可以取消勾选 write，只保留 read 进行 scope 缩减授权演示。',
    requestedScope: 'openid profile email read write',
    expectedScopeAfterConsent: '例如 openid profile email read'
  }
  await startAuthorizationCodeFlow({
    clientId: 'spa-consent-client',
    scope: 'openid profile email read write'
  })
}

async function startAuthorizationCodeFlow({ clientId, scope }) {
  const frontendOrigin = window.location.origin
  const redirectUri = `${frontendOrigin}/callback`
  const state = generateRandomString(32)
  const { codeVerifier, codeChallenge } = await generatePkcePair()

  sessionStorage.setItem('oauth2_state', state)
  sessionStorage.setItem('pkce_code_verifier', codeVerifier)
  sessionStorage.setItem('oauth2_client_id', clientId)
  sessionStorage.setItem('oauth2_return_to', `/flows${activeTab.value === 'pkce' ? '' : `?tab=${activeTab.value}`}`)

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope,
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

async function startOidcLogout() {
  try {
    const { data } = await oauth2Api.getDiscovery()
    if (!data.end_session_endpoint) {
      result.value = {
        operation: 'oidc_logout',
        message: 'discovery 文档未暴露 end_session_endpoint，当前无法演示 OIDC Logout。'
      }
      ElMessage.warning('当前服务未暴露 end_session_endpoint')
      return
    }

    const endSessionUrl = new URL(data.end_session_endpoint)
    endSessionUrl.searchParams.set('id_token_hint', idToken.value)
    endSessionUrl.searchParams.set('post_logout_redirect_uri', window.location.origin)

    sessionStorage.setItem('oauth2_oidc_logout_message', '1')

    sessionStorage.removeItem('oauth2_access_token')
    sessionStorage.removeItem('oauth2_id_token')
    sessionStorage.removeItem('oauth2_refresh_token')
    sessionStorage.removeItem('oauth2_scope')
    accessToken.value = ''
    idToken.value = ''
    refreshToken.value = ''
    currentScope.value = ''

    window.location.href = endSessionUrl.toString()
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
    m2mLastIssuedToken.value = data.access_token
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

async function getClientAuthMethodToken(method) {
  try {
    let data

    if (method === 'basic') {
      const basic = btoa(`${clientAuthForm.confidentialClientId}:${clientAuthForm.confidentialClientSecret}`)
      ;({ data } = await oauth2Api.clientCredentials({
        grant_type: 'client_credentials',
        scope: clientAuthForm.confidentialScope
      }, {
        Authorization: `Basic ${basic}`
      }))
    } else {
      ;({ data } = await oauth2Api.clientCredentials({
        grant_type: 'client_credentials',
        client_id: clientAuthForm.confidentialClientId,
        client_secret: clientAuthForm.confidentialClientSecret,
        scope: clientAuthForm.confidentialScope
      }))
    }

    clientAuthTokens[method] = data.access_token
    result.value = {
      operation: `client_auth_${method}`,
      authMethod: method === 'basic' ? 'client_secret_basic' : 'client_secret_post',
      clientId: clientAuthForm.confidentialClientId,
      scope: clientAuthForm.confidentialScope,
      requestPlacement: method === 'basic' ? 'Authorization Header' : 'Form Body',
      tokenPreview: maskToken(data.access_token),
      response: data
    }
    ElMessage.success(`${method === 'basic' ? 'Basic' : 'Post'} 认证成功`)
  } catch (e) {
    showError(e)
  }
}

async function startPublicClientAuthDemo() {
  result.value = {
    operation: 'client_auth_none',
    authMethod: 'none',
    clientId: clientAuthForm.publicClientId,
    message: '公开客户端不会携带 client_secret，而是通过 Authorization Code + PKCE 发起授权。完成登录后会回到当前演示标签页。',
    scope: clientAuthForm.publicScope
  }
  await startAuthorizationCodeFlow({
    clientId: clientAuthForm.publicClientId,
    scope: clientAuthForm.publicScope
  })
}

async function tryPublicClientCredentials() {
  try {
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      client_id: clientAuthForm.publicClientId,
      scope: 'read'
    })
    result.value = {
      operation: 'public_client_credentials_unexpected_success',
      response: data
    }
  } catch (e) {
    result.value = {
      operation: 'public_client_credentials_failed_as_expected',
      authMethod: 'none',
      clientId: clientAuthForm.publicClientId,
      message: '公开客户端尝试直接使用 client_credentials 时失败，说明它不能像机密客户端那样靠 secret 向 token endpoint 认证。',
      error: e.response?.data || { error: e.message }
    }
    ElMessage.success('公开客户端错误示例已返回预期失败')
  }
}

async function introspectM2mToken() {
  try {
    const token = m2mLifecycleToken.value
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    const { data } = await oauth2Api.introspectToken({
      token,
      token_type_hint: 'access_token'
    }, {
      Authorization: `Basic ${basic}`
    })
    result.value = {
      operation: 'introspection',
      clientId: m2mForm.clientId,
      selectedClient: m2mSelectedClient.value.label,
      inspectedTokenPreview: maskToken(token),
      introspection: data
    }
  } catch (e) {
    showError(e)
  }
}

async function revokeM2mToken() {
  try {
    const token = m2mToken.value
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    await oauth2Api.revokeToken({
      token,
      token_type_hint: 'access_token'
    }, {
      Authorization: `Basic ${basic}`
    })
    m2mLastIssuedToken.value = token
    m2mToken.value = ''
    result.value = {
      operation: 'revocation',
      clientId: m2mForm.clientId,
      selectedClient: m2mSelectedClient.value.label,
      revokedTokenPreview: maskToken(token),
      message: 'access_token 已撤销。此时再做 introspection，active 应为 false。'
    }
    ElMessage.success('M2M access_token 已撤销')
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
    devicePollingAborted = false
    isPollingDeviceToken.value = false
    const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
    sessionStorage.setItem('oauth2_device_return_to', '/flows?tab=device')
    const { data } = await oauth2Api.deviceAuthorize({
      client_id: deviceForm.clientId,
      scope: deviceForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    deviceAuth.value = decorateDeviceAuth(data)
    result.value = deviceAuth.value
  } catch (e) {
    showError(e)
  }
}

async function pollDeviceToken() {
  if (!deviceAuth.value.device_code || isPollingDeviceToken.value) return

  isPollingDeviceToken.value = true
  devicePollingAborted = false
  const startedAt = getDeviceFlowStartedAt(deviceAuth.value)
  let waitMs = getDevicePollingIntervalMs(deviceAuth.value.interval)

  try {
    while (!devicePollingAborted) {
      const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
      try {
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
        ElMessage.success('设备授权成功，已获取 Token')
        return
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
          showError(e)
          return
        }
      }

      if (Date.now() - startedAt >= DEVICE_FLOW_TIMEOUT_MS) {
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

async function handleLogout() {
  await authApi.logout().catch(() => {})
  sessionStorage.removeItem('oauth2_access_token')
  sessionStorage.removeItem('oauth2_id_token')
  sessionStorage.removeItem('oauth2_refresh_token')
  sessionStorage.removeItem('oauth2_scope')
  router.push('/login')
}

function goClients() {
  router.push('/clients')
}

function showError(e) {
  result.value = e.response?.data || { error: e.message }
  ElMessage.error(e.response?.data?.error_description || e.response?.data?.error || e.message)
}

function maskToken(token) {
  if (typeof token !== 'string' || token.length < 24) return token || ''
  return `${token.slice(0, 12)}...${token.slice(-12)}`
}

function summarizeClaim(value) {
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

function buildClaimsRow(claim, userAccessTokenValue, idTokenValue, machineAccessTokenValue) {
  return {
    claim,
    userAccessToken: normalizeClaimValue(userAccessTokenValue),
    idToken: normalizeClaimValue(idTokenValue),
    machineAccessToken: normalizeClaimValue(machineAccessTokenValue)
  }
}

function normalizeClaimValue(value) {
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

function decodeJwt(token) {
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

function base64UrlDecode(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return decodeURIComponent(Array.from(atob(padded))
    .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
    .join(''))
}

function buildClaimsHighlights({ userAccessToken, idToken: decodedIdTokenValue, machineAccessToken }) {
  const highlights = []

  if (decodedIdTokenValue?.payload) {
    highlights.push('id_token 面向客户端身份会话，通常携带 preferred_username、email、sid 等 OIDC 身份声明。')
  }
  if (userAccessToken?.payload) {
    highlights.push('用户 access_token 面向资源服务器授权，保留 scope 等访问控制字段；它不一定包含完整的用户资料声明。')
  }
  if (machineAccessToken?.payload) {
    highlights.push('client_credentials access_token 通常没有真实用户身份字段，更常见的是 sub 与 client_id 对应，表达“客户端自己”在访问资源。')
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

function writeClaimsComparisonResult() {
  result.value = {
    operation: 'jwt_claims_comparison',
    userAccessToken: decodedUserAccessToken.value,
    idToken: decodedIdToken.value,
    machineAccessToken: decodedMachineAccessToken.value,
    highlights: claimsHighlights.value,
    rows: claimsComparisonRows.value
  }
}

function writeClientAuthSummary() {
  result.value = {
    operation: 'client_auth_summary',
    confidentialClient: {
      clientId: clientAuthForm.confidentialClientId,
      basicTokenPreview: maskToken(clientAuthTokens.basic),
      postTokenPreview: maskToken(clientAuthTokens.post),
      scope: clientAuthForm.confidentialScope
    },
    publicClient: {
      clientId: clientAuthForm.publicClientId,
      loginModel: 'authorization_code + PKCE',
      currentUserAccessTokenPreview: maskToken(accessToken.value),
      scope: clientAuthForm.publicScope
    },
    comparisonRows: clientAuthComparisonRows.value,
    highlights: clientAuthHighlights.value
  }
}

function decorateDeviceAuth(data) {
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

function appendDeviceTimingParams(url, startedAt, expiresAt) {
  if (typeof url !== 'string' || !url) return url
  const parsedUrl = new URL(url)
  parsedUrl.searchParams.set('started_at', String(startedAt))
  parsedUrl.searchParams.set('expires_at', String(expiresAt))
  return parsedUrl.toString()
}

function persistDeviceFlowTiming(userCode, startedAt, expiresAt) {
  if (typeof userCode !== 'string' || !userCode) return
  localStorage.setItem(getDeviceFlowStorageKey(userCode), JSON.stringify({ startedAt, expiresAt }))
}

function getDeviceFlowStartedAt(deviceAuthValue) {
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

function getPersistedDeviceFlowTiming(userCode) {
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

function getDevicePollingIntervalMs(intervalSeconds) {
  const parsedInterval = Number(intervalSeconds)
  const safeInterval = Number.isFinite(parsedInterval) && parsedInterval > 0
    ? parsedInterval
    : DEFAULT_DEVICE_INTERVAL_SECONDS
  return safeInterval * 1000
}

function delay(ms) {
  return new Promise(resolve => {
    window.setTimeout(resolve, ms)
  })
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
