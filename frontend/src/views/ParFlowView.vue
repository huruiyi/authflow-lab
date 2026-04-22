<template>
  <OAuth2Layout>
    <div class="par-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">由后端代发 PAR，前端拿到 request_uri 后再跳转授权端点</el-descriptions-item>
          <el-descriptions-item label="安全价值">SPA 不直接持有 client_secret，敏感客户端认证由后端代办</el-descriptions-item>
          <el-descriptions-item label="演示方式">BFF / 后端代理式 PAR 演示：前端只保留 PKCE 和浏览器跳转</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header><span>PAR 请求参数</span></template>
        <el-form label-width="140px" class="inline-form">
          <el-form-item label="client_id">
            <el-input v-model="parForm.clientId" />
          </el-form-item>
          <el-form-item label="scope">
            <el-input v-model="parForm.scope" />
          </el-form-item>
        </el-form>

        <div class="actions-row">
          <el-button type="primary" :icon="Connection" @click="startParFlow">1) 提交 PAR 并发起授权</el-button>
          <el-button :icon="Document" @click="writeParSummary">输出 PAR 总结</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span>PAR 响应状态</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="request_uri">{{ parState.requestUri || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="expires_in">{{ parState.expiresIn ? `${parState.expiresIn} 秒` : '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="最近状态">{{ parState.lastStatus || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { oauth2Api } from '../api/oauth2'
import { handleOAuth2Error, getAuthServerOrigin } from '../utils/oauth2Helper'
import { generatePkcePair, generateRandomString } from '../composables/pkce'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })

const parForm = reactive({
  clientId: 'spa-par-client',
  scope: 'openid profile email read write'
})

const parState = reactive({
  requestUri: '',
  expiresIn: 0,
  lastStatus: ''
})

async function startParFlow() {
  try {
    const frontendOrigin = window.location.origin
    const redirectUri = `${frontendOrigin}/callback`
    const state = generateRandomString(32)
    const pkcePair = await generatePkcePair()

    sessionStorage.setItem('oauth2_state', state)
    sessionStorage.setItem('pkce_mode', 'required')
    sessionStorage.setItem('pkce_code_verifier', pkcePair.codeVerifier)
    sessionStorage.setItem('oauth2_demo_scenario', 'par')
    sessionStorage.setItem('oauth2_client_id', parForm.clientId)
    sessionStorage.setItem('oauth2_return_to', '/par')

    const { data } = await oauth2Api.pushedAuthorization({
      response_type: 'code',
      client_id: parForm.clientId,
      redirect_uri: redirectUri,
      scope: parForm.scope,
      state,
      code_challenge: pkcePair.codeChallenge,
      code_challenge_method: 'S256'
    })

    parState.requestUri = data.request_uri || ''
    parState.expiresIn = data.expires_in || 0
    parState.lastStatus = 'PAR 请求成功，准备跳转授权端点'

    result.value = {
      operation: 'par_request',
      clientId: parForm.clientId,
      response: data,
      message: '已获取 request_uri，将使用精简参数发起授权。'
    }

    const authServerOrigin = getAuthServerOrigin()
    const authorizeParams = new URLSearchParams({
      client_id: parForm.clientId,
      request_uri: data.request_uri
    })
    window.location.href = `${authServerOrigin}/oauth2/authorize?${authorizeParams.toString()}`
  } catch (e) {
    parState.lastStatus = 'PAR 请求失败'
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

function writeParSummary() {
  result.value = {
    operation: 'par_summary',
    clientId: parForm.clientId,
    requestUri: parState.requestUri || null,
    requestUriExpiresIn: parState.expiresIn || null,
    keyPoints: [
      'PAR 先在后端通道提交完整授权参数，返回 request_uri。',
      '浏览器跳转授权端点时可不再携带完整 scope / redirect_uri / code_challenge。',
      '该模式更适合高安全、合规要求高的企业场景。'
    ]
  }
}
</script>

<style scoped>
.par-container {
  padding: 12px 0;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.inline-form {
  max-width: 600px;
}

.actions-row {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}
</style>