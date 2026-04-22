<template>
  <OAuth2Layout>
    <div class="pkce-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="适用场景">SPA / Vue / React / 移动端公开客户端</el-descriptions-item>
          <el-descriptions-item label="Client">spa-public-client</el-descriptions-item>
          <el-descriptions-item label="特点">无 client_secret，前端生成 PKCE，跳转授权页，换取 access_token / refresh_token</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <div class="actions-row">
        <el-button type="primary" :icon="Connection" @click="startPkceFlow">发起授权登录</el-button>
        <el-button type="warning" plain :icon="Shield" @click="startScopeConsentDemo">演示 Scope 缩减授权</el-button>
        <el-button :icon="Document" @click="loadDiscovery">查看 Discovery 文档</el-button>
        <el-button :icon="Key" @click="loadJwks">查看 JWK Set</el-button>
      </div>

      <TokenDisplay
        :access-token="tokenState.accessToken"
        :refresh-token="tokenState.refreshToken"
        :id-token="tokenState.idToken"
        :scope="tokenState.scope"
        :token-type="'Bearer'"
        :expires-in="getExpiresInValue()"
        :show-expiry="true"
        title="当前 Token"
      />

      <el-divider />

      <div class="actions-row">
        <el-button @click="callPublic">访问公开资源</el-button>
        <el-button type="success" :disabled="!tokenState.accessToken" @click="callProfile">读取用户 Profile</el-button>
        <el-button type="success" :disabled="!tokenState.accessToken" @click="callUserInfo">调用 UserInfo</el-button>
        <el-button type="warning" :disabled="!tokenState.accessToken" @click="callRead">读取业务资源</el-button>
        <el-button type="danger" :disabled="!tokenState.accessToken" @click="callWrite">写入业务资源</el-button>
        <el-button :disabled="!tokenState.accessToken" @click="callTokenInfo">查看 Token Claims</el-button>
        <el-button type="primary" plain :disabled="!tokenState.refreshToken" @click="doRefreshToken">刷新 Access Token</el-button>
        <el-button type="info" plain :disabled="!tokenState.idToken" @click="startOidcLogout">OIDC Logout</el-button>
      </div>

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import TokenDisplay from '../components/TokenDisplay.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { oauth2Api } from '../api/oauth2'
import { startAuthorizationCodeFlow, handleOAuth2Error, createOAuth2SyncListener } from '../utils/oauth2Helper'
import { saveTokens } from '../utils/tokenHelper'
import { getTokenState, clearTokens } from '../utils/tokenHelper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const tokenState = reactive(getTokenState())
const oauth2SyncChannel = 'oauth2-token-sync-pkce'
let disposeTokenSync = null

function getExpiresInValue() {
  const expiresIn = sessionStorage.getItem('oauth2_access_token_expires_in')
  return expiresIn || ''
}

onMounted(() => {
  Object.assign(tokenState, getTokenState())
  disposeTokenSync = createOAuth2SyncListener(oauth2SyncChannel, applySyncedTokens)
})

onBeforeUnmount(() => {
  disposeTokenSync?.()
})

async function startPkceFlow() {
  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'pkce',
    returnTo: '/pkce',
    openInNewWindow: true,
    syncChannel: oauth2SyncChannel
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
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'pkce-consent',
    returnTo: '/pkce',
    openInNewWindow: true,
    syncChannel: oauth2SyncChannel
  })
}

function applySyncedTokens(payload) {
  saveTokens(payload)
  Object.assign(tokenState, getTokenState())
  result.value = {
    operation: 'oauth2_callback_sync',
    message: '已同步新窗口授权结果。',
    scope: payload.scope,
    expires_in: payload.expires_in,
    refresh_token: payload.refresh_token ? '已返回' : '无'
  }
}

async function loadDiscovery() {
  try {
    const { data } = await oauth2Api.getDiscovery()
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function loadJwks() {
  try {
    const { data } = await oauth2Api.getJwks()
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callPublic() {
  try {
    const { data } = await oauth2Api.callPublicResource()
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callProfile() {
  try {
    const { data } = await oauth2Api.callProfileResource(tokenState.accessToken)
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callUserInfo() {
  try {
    const { data } = await oauth2Api.getUserInfo(tokenState.accessToken)
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callRead() {
  try {
    const { data } = await oauth2Api.callReadResource(tokenState.accessToken)
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callWrite() {
  try {
    const { data } = await oauth2Api.callWriteResource(tokenState.accessToken, {
      orderNo: 'ORD-' + Date.now(),
      amount: 299,
      status: 'PAID'
    })
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function callTokenInfo() {
  try {
    const { data } = await oauth2Api.callTokenInfo(tokenState.accessToken)
    result.value = data
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function doRefreshToken() {
  const clientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  try {
    const { data } = await oauth2Api.refreshToken({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: tokenState.refreshToken
    })
    saveTokens(data)
    Object.assign(tokenState, getTokenState())
    result.value = data
    ElMessage.success('refresh_token 刷新成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
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
    endSessionUrl.searchParams.set('id_token_hint', tokenState.idToken)
    endSessionUrl.searchParams.set('post_logout_redirect_uri', window.location.origin)

    sessionStorage.setItem('oauth2_oidc_logout_message', '1')
    clearTokens()

    window.location.href = endSessionUrl.toString()
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}
</script>

<style scoped>
.pkce-container {
  padding: 12px 0;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.actions-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
</style>