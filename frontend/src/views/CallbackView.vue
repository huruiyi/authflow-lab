<template>
  <div class="callback-page">
    <el-card class="callback-card">
      <template #header>
        <div class="header">
          <span>OAuth2 回调处理中</span>
        </div>
      </template>

      <div v-if="loading" class="state-block">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else>
        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          :closable="false"
        />

        <div v-if="error" class="actions">
          <el-button type="primary" @click="goFlows">返回功能演示</el-button>
          <el-button @click="goClients">返回 Client 管理</el-button>
        </div>

        <div v-else class="result-block">
          <el-alert title="授权成功，已完成授权码兑换" type="success" show-icon :closable="false" />

          <el-descriptions :column="1" border class="desc">
            <el-descriptions-item label="当前前端地址">{{ frontendOrigin }}</el-descriptions-item>
            <el-descriptions-item label="access_token">
              <div class="token-box">{{ tokenResult.access_token }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="id_token">
              <div class="token-box">{{ tokenResult.id_token || '无' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="refresh_token">
              <div class="token-box">{{ tokenResult.refresh_token || '无' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="scope">{{ tokenResult.scope || '无' }}</el-descriptions-item>
            <el-descriptions-item label="token_type">{{ tokenResult.token_type }}</el-descriptions-item>
            <el-descriptions-item label="expires_in">{{ tokenResult.expires_in }}</el-descriptions-item>
          </el-descriptions>

          <div class="actions">
            <el-button type="primary" @click="goFlows">返回功能演示</el-button>
            <el-button @click="goClients">返回 Client 管理</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oauth2Api } from '../api/oauth2'
import { saveTokens } from '../utils/tokenHelper'
import { broadcastOAuth2Sync, clearOAuth2RedirectContext } from '../utils/oauth2Helper'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const tokenResult = ref({})
const frontendOrigin = window.location.origin

onMounted(async () => {
  const code = route.query.code
  const state = route.query.state
  const errorParam = route.query.error

  if (errorParam) {
    error.value = `授权失败：${errorParam}`
    loading.value = false
    return
  }

  if (!code || !state) {
    error.value = '缺少 code 或 state 参数'
    loading.value = false
    return
  }

  const savedState = sessionStorage.getItem('oauth2_state')
  const codeVerifier = sessionStorage.getItem('pkce_code_verifier')
  const clientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  const clientSecret = sessionStorage.getItem('oauth2_client_secret') || ''
  const pkceMode = sessionStorage.getItem('pkce_mode') || 'required'
  const redirectUri = `${window.location.origin}${route.path}`

  if (!savedState || state !== savedState) {
    error.value = 'state 校验失败'
    loading.value = false
    return
  }

  if (pkceMode === 'required' && !codeVerifier) {
    error.value = '缺少 code_verifier，无法完成 PKCE 换 token'
    loading.value = false
    return
  }

  try {
    const tokenRequestBody = {
      grant_type: 'authorization_code',
      client_id: clientId,
      code,
      redirect_uri: redirectUri
    }

    if (pkceMode === 'required') {
      tokenRequestBody.code_verifier = codeVerifier
    }
    if (clientSecret) {
      tokenRequestBody.client_secret = clientSecret
    }

    const { data } = await oauth2Api.exchangeCode({
      ...tokenRequestBody
    })
    tokenResult.value = data
    saveTokens(data)
    broadcastOAuth2Sync(data)
    ElMessage.success(`授权成功，已回到 ${window.location.origin}`)
  } catch (e) {
    error.value = e.response?.data?.error_description || e.response?.data?.error || e.message
  } finally {
    clearOAuth2RedirectContext()
    loading.value = false
  }
})

function goFlows() {
  const returnTo = sessionStorage.getItem('oauth2_return_to') || '/pkce'
  sessionStorage.removeItem('oauth2_return_to')
  router.push(returnTo)
}

function goClients() {
  router.push('/clients')
}
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.callback-card {
  width: min(960px, 100%);
}
.header {
  font-size: 18px;
  font-weight: 600;
}
.desc {
  margin-top: 16px;
}
.token-box {
  max-width: 100%;
  word-break: break-all;
  white-space: pre-wrap;
  font-family: monospace;
}
.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
