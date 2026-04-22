<template>
  <OAuth2Layout>
    <div class="token-lifecycle-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">观察 access_token 短过期后的刷新行为，并对比 refresh_token 是否轮换</el-descriptions-item>
          <el-descriptions-item label="对比客户端">spa-public-client（默认复用 refresh_token） vs spa-rotation-client（每次刷新轮换）</el-descriptions-item>
          <el-descriptions-item label="观察重点">expires_in 倒计时、刷新后 refresh_token 是否变化、过期后资源访问结果</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="12" class="cards-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header><span>启动对比会话</span></template>
            <div class="actions-row">
              <el-button type="primary" @click="startTokenLifecycleLogin('default')">登录默认策略（复用 refresh_token）</el-button>
              <el-button type="warning" plain @click="startTokenLifecycleLogin('rotation')">登录轮换策略（刷新后换新 refresh_token）</el-button>
              <el-button @click="loadTokenLifecycleFromCurrentSession">从当前会话加载</el-button>
            </div>

            <el-alert
              type="info"
              show-icon
              :closable="false"
              :title="tokenLifecycleState.policyHint"
            />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header><span>当前状态</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="当前 client_id">{{ tokenLifecycleState.clientId || '未指定' }}</el-descriptions-item>
              <el-descriptions-item label="access_token 剩余秒数">{{ tokenLifecycleRemainingSecondsDisplay }}</el-descriptions-item>
              <el-descriptions-item label="刷新次数">{{ tokenLifecycleState.refreshCount }}</el-descriptions-item>
              <el-descriptions-item label="最近一次刷新结果">{{ tokenLifecycleState.lastRefreshOutcome || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <TokenDisplay
        :access-token="tokenState.accessToken"
        :refresh-token="tokenState.refreshToken"
        :id-token="tokenState.idToken"
        :scope="tokenState.scope"
        :show-expiry="true"
        title="Token 明细"
      />

      <div class="actions-row">
        <el-button type="success" :disabled="!tokenState.refreshToken" @click="refreshForLifecycleDemo">执行 Refresh Token</el-button>
        <el-button type="danger" plain :disabled="!tokenState.accessToken" @click="simulateAccessTokenExpired">模拟 access_token 已过期</el-button>
        <el-button type="warning" :disabled="!tokenState.accessToken" @click="verifyLifecycleReadResource">验证读取资源</el-button>
        <el-button @click="writeTokenLifecycleSummary">输出生命周期总结</el-button>
      </div>

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import TokenDisplay from '../components/TokenDisplay.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { oauth2Api } from '../api/oauth2'
import { startAuthorizationCodeFlow, handleOAuth2Error } from '../utils/oauth2Helper'
import { saveTokens, getTokenRemainingSeconds, getTokenState } from '../utils/tokenHelper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const tokenState = reactive(getTokenState())

const tokenLifecycleState = reactive({
  clientId: sessionStorage.getItem('oauth2_client_id') || 'spa-public-client',
  refreshCount: 0,
  previousRefreshToken: '',
  lastRefreshOutcome: '',
  policyHint: '默认可直接复用当前会话。若要对比轮换策略，请点"登录轮换策略"。'
})

const nowTimestamp = ref(Date.now())
let lifecycleTimer = null

const tokenLifecycleRemainingSecondsDisplay = computed(() => {
  const remainingSeconds = getTokenRemainingSeconds()
  if (remainingSeconds === null) {
    return '未知（请先获取 token）'
  }
  if (remainingSeconds < 0) {
    return `${Math.abs(remainingSeconds)} 秒前已过期`
  }
  return `${remainingSeconds} 秒`
})

onMounted(() => {
  lifecycleTimer = window.setInterval(() => {
    nowTimestamp.value = Date.now()
  }, 1000)
  loadTokenLifecycleFromCurrentSession()
})

onBeforeUnmount(() => {
  if (lifecycleTimer) {
    window.clearInterval(lifecycleTimer)
    lifecycleTimer = null
  }
})

async function startTokenLifecycleLogin(mode) {
  const useRotationPolicy = mode === 'rotation'
  const clientId = useRotationPolicy ? 'spa-rotation-client' : 'spa-public-client'
  const policyHint = useRotationPolicy
    ? '当前使用 spa-rotation-client：access_token 默认 30 秒过期，refresh_token 每次刷新后应轮换。'
    : '当前使用 spa-public-client：refresh_token 默认可复用，刷新后通常保持不变。'

  tokenLifecycleState.clientId = clientId
  tokenLifecycleState.refreshCount = 0
  tokenLifecycleState.previousRefreshToken = ''
  tokenLifecycleState.lastRefreshOutcome = ''
  tokenLifecycleState.policyHint = policyHint

  result.value = {
    operation: 'token_lifecycle_login_prepare',
    mode,
    clientId,
    policyHint,
    message: '即将发起授权，完成后返回当前标签继续演示。'
  }

  await startAuthorizationCodeFlow({
    clientId,
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: useRotationPolicy ? 'token-lifecycle-rotation' : 'token-lifecycle-default',
    returnTo: '/token-lifecycle',
    openInNewWindow: true
  })
}

function loadTokenLifecycleFromCurrentSession() {
  Object.assign(tokenState, getTokenState())
  const currentClientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  tokenLifecycleState.clientId = currentClientId
  tokenLifecycleState.policyHint = currentClientId === 'spa-rotation-client'
    ? '当前会话是轮换策略客户端，刷新后 refresh_token 应变化。'
    : '当前会话是默认策略客户端，refresh_token 可能保持不变。'
}

async function refreshForLifecycleDemo() {
  const previousRefreshToken = tokenState.refreshToken
  await doRefreshToken()
  tokenLifecycleState.refreshCount += 1
  tokenLifecycleState.previousRefreshToken = previousRefreshToken
}

async function doRefreshToken() {
  const clientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  try {
    const previousRefreshToken = tokenState.refreshToken
    const { data } = await oauth2Api.refreshToken({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: tokenState.refreshToken
    })
    saveTokens(data)
    Object.assign(tokenState, getTokenState())
    result.value = data
    tokenLifecycleState.previousRefreshToken = previousRefreshToken
    tokenLifecycleState.lastRefreshOutcome = data.refresh_token && data.refresh_token !== previousRefreshToken
      ? 'refresh_token 已轮换'
      : 'refresh_token 复用或服务端未返回新值'
    ElMessage.success('refresh_token 刷新成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

function simulateAccessTokenExpired() {
  const expiredAt = Date.now() - 1000
  sessionStorage.setItem('oauth2_access_token_expires_at', String(expiredAt))
  tokenLifecycleState.lastRefreshOutcome = '已手动标记 access_token 过期（仅前端演示）'
  result.value = {
    operation: 'simulate_access_token_expired',
    expiresAt: expiredAt,
    message: '已把 access_token 过期时间改为过去时刻，可继续点"验证读取资源"或"执行 Refresh Token"。'
  }
  ElMessage.success('已模拟 token 过期')
}

async function verifyLifecycleReadResource() {
  try {
    const { data } = await oauth2Api.callReadResource(tokenState.accessToken)
    result.value = {
      operation: 'token_lifecycle_verify_read',
      status: 'success',
      remainingSeconds: getTokenRemainingSeconds(),
      response: data
    }
    ElMessage.success('当前 access_token 仍可访问资源')
  } catch (e) {
    result.value = {
      operation: 'token_lifecycle_verify_read',
      status: 'failed',
      remainingSeconds: getTokenRemainingSeconds(),
      error: e.response?.data || { error: e.message },
      hint: '若是 token 过期导致失败，可点"执行 Refresh Token"后再重试。'
    }
    ElMessage.warning('资源访问失败，可能是 token 过期或权限问题')
  }
}

function writeTokenLifecycleSummary() {
  result.value = {
    operation: 'token_lifecycle_summary',
    clientId: tokenLifecycleState.clientId,
    refreshCount: tokenLifecycleState.refreshCount,
    remainingSeconds: getTokenRemainingSeconds(),
    refreshTokenChanged: tokenLifecycleState.previousRefreshToken
      ? tokenState.refreshToken !== tokenLifecycleState.previousRefreshToken
      : null,
    previousRefreshTokenPreview: tokenLifecycleState.previousRefreshToken ? `${tokenLifecycleState.previousRefreshToken.slice(0, 20)}...` : '无',
    currentRefreshTokenPreview: tokenState.refreshToken ? `${tokenState.refreshToken.slice(0, 20)}...` : '无',
    hints: [
      '默认策略客户端通常复用 refresh_token。',
      '轮换策略客户端刷新后应返回新的 refresh_token。',
      'access_token 过期后应使用 refresh_token 续期，而不是强制用户重新登录。'
    ]
  }
}
</script>

<style scoped>
.token-lifecycle-container {
  padding: 6px 4px;
  overflow-x: hidden;
}

.info-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.cards-row {
  margin: 12px 0;
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.actions-row {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .actions-row :deep(.el-button) {
    margin-left: 0;
  }
}
</style>