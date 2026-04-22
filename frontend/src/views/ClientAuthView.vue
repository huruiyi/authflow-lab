<template>
  <OAuth2Layout>
    <div class="client-auth-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">对比 client_secret_basic、client_secret_post、none 三种 token endpoint 客户端认证模型</el-descriptions-item>
          <el-descriptions-item label="重点理解">机密客户端要证明"我是谁"，公开客户端则不保存 secret，而是依赖 PKCE</el-descriptions-item>
          <el-descriptions-item label="当前客户端">all-in-one-client 同时支持 basic/post；spa-public-client 代表公开客户端</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="同一个 token endpoint，机密客户端可以通过 Header 或 Body 认证；公开客户端不应持有 client_secret。"
      />

      <el-row :gutter="12" class="cards-row">
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
              type="warning"
              show-icon
              :closable="false"
              title="公开客户端没有 client_secret，因此不能像服务端那样用 client_credentials 直接向 token 端点换服务 Token。"
            />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never">
        <template #header><span>三种认证方式对比</span></template>
        <el-table :data="clientAuthComparisonRows" border table-layout="fixed">
          <el-table-column prop="dimension" label="对比维度" min-width="130" />
          <el-table-column prop="basic" label="client_secret_basic" min-width="160" show-overflow-tooltip />
          <el-table-column prop="post" label="client_secret_post" min-width="160" show-overflow-tooltip />
          <el-table-column prop="none" label="none" min-width="160" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header><span>差异结论</span></template>
        <div class="actions-row">
          <el-button @click="writeClientAuthSummary">输出当前客户端认证总结</el-button>
        </div>
        <el-alert
          v-for="highlight in clientAuthHighlights"
          :key="highlight"
          class="mb16"
          type="success"
          show-icon
          :closable="false"
          :title="highlight"
        />
      </el-card>

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { oauth2Api } from '../api/oauth2'
import { startAuthorizationCodeFlow, handleOAuth2Error, generateBasicAuth } from '../utils/oauth2Helper'
import { maskToken } from '../utils/tokenHelper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })

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
    'none 不是"更弱的 basic"，而是公开客户端模型：不保存密钥，靠 PKCE 证明是同一发起方。'
  ]

  if (clientAuthTokens.basic && clientAuthTokens.post) {
    highlights.push('当前 Basic 与 Post 都已成功拿到 token，说明服务端同时接受两种机密客户端认证方式。')
  }

  if (!clientAuthTokens.basic && !clientAuthTokens.post) {
    highlights.push('先分别点一次 Basic / Post 获取 token，再观察机密客户端两种认证方式的差异。')
  }

  return highlights
})

async function getClientAuthMethodToken(method) {
  try {
    let data

    if (method === 'basic') {
      const basic = generateBasicAuth(clientAuthForm.confidentialClientId, clientAuthForm.confidentialClientSecret)
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
    result.value = handleOAuth2Error(e, ElMessage.error)
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
    scope: clientAuthForm.publicScope,
    usePkce: true,
    scenario: 'client-auth-none',
    returnTo: '/client-auth'
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
      scope: clientAuthForm.publicScope
    },
    comparisonRows: clientAuthComparisonRows.value,
    highlights: clientAuthHighlights.value
  }
}
</script>

<style scoped>
.client-auth-container {
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

.token-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}

.mt16 {
  margin-top: 12px;
}

.mb16 {
  margin-bottom: 12px;
}
</style>