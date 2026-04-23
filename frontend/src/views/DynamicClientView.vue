<template>
  <OAuth2Layout>
    <div class="dynamic-client-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">通过 API 动态注册一个公开客户端，然后直接发起 Authorization Code + PKCE</el-descriptions-item>
          <el-descriptions-item label="关键差异">无需提前在 data.sql 固化，运行时创建 client_id、redirect_uri、scope</el-descriptions-item>
          <el-descriptions-item label="本项目实现">复用 /api/clients 管理接口模拟 DCR 过程，便于本地教学演示</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never">
        <template #header><span>动态注册参数</span></template>
        <el-form label-width="160px" class="inline-form">
          <el-form-item label="client_id">
            <el-input v-model="dynamicClientForm.clientId" placeholder="dynamic-spa-xxx">
              <template #append>
                <el-button @click="regenerateDynamicClientId">重生成</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="client_name">
            <el-input v-model="dynamicClientForm.clientName" placeholder="动态注册 SPA 客户端" />
          </el-form-item>
          <el-form-item label="scope（空格分隔）">
            <el-input v-model="dynamicClientForm.scope" placeholder="openid profile email read write" />
          </el-form-item>
          <el-form-item label="授权许可">
            <el-switch v-model="dynamicClientForm.requireAuthorizationConsent" />
          </el-form-item>
        </el-form>

        <div class="actions-row">
          <el-button type="primary" :icon="Plus" @click="registerDynamicClient">1) 动态注册客户端</el-button>
          <el-button type="success" :disabled="!dynamicClientState.id" :icon="Connection" @click="startDynamicClientAuthorization">2) 使用新客户端发起授权</el-button>
          <el-button type="danger" plain :disabled="!dynamicClientState.id" :icon="Delete" @click="deleteDynamicClient">3) 删除动态客户端</el-button>
          <el-button :icon="Document" @click="writeDynamicClientSummary">输出 DCR 总结</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span>注册结果</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="registered_client_id">{{ dynamicClientState.id || '未注册' }}</el-descriptions-item>
          <el-descriptions-item label="client_id">{{ dynamicClientState.clientId || '未注册' }}</el-descriptions-item>
          <el-descriptions-item label="最近操作">{{ dynamicClientState.lastOperation || '暂无' }}</el-descriptions-item>
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
import { startAuthorizationCodeFlow, handleOAuth2Error, buildDynamicClientId, splitScopeString } from '../utils/oauth2Helper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })

const dynamicClientForm = reactive({
  clientId: buildDynamicClientId(),
  clientName: '动态注册 SPA 客户端',
  scope: 'openid profile email read write',
  requireAuthorizationConsent: false
})

const dynamicClientState = reactive({
  id: '',
  clientId: '',
  lastOperation: ''
})

function regenerateDynamicClientId() {
  dynamicClientForm.clientId = buildDynamicClientId()
}

async function registerDynamicClient() {
  try {
    const scopeList = splitScopeString(dynamicClientForm.scope)
    const redirectUri = `${window.location.origin}/callback`
    const payload = {
      clientId: dynamicClientForm.clientId,
      clientSecret: '',
      clientName: dynamicClientForm.clientName,
      clientAuthenticationMethods: ['none'],
      authorizationGrantTypes: ['authorization_code', 'refresh_token'],
      redirectUris: [redirectUri],
      postLogoutRedirectUris: [window.location.origin],
      scopes: scopeList,
      requireProofKey: true,
      requireAuthorizationConsent: dynamicClientForm.requireAuthorizationConsent
    }

    const { data } = await oauth2Api.createClient(payload)
    dynamicClientState.id = data.id
    dynamicClientState.clientId = data.clientId
    dynamicClientState.lastOperation = '动态注册成功'
    result.value = {
      operation: 'dynamic_client_registration',
      payload,
      registered: data,
      message: '动态注册成功，可直接点击"使用新客户端发起授权"。'
    }
    ElMessage.success('动态注册成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function startDynamicClientAuthorization() {
  if (!dynamicClientState.clientId) {
    ElMessage.warning('请先完成动态注册')
    return
  }
  await startAuthorizationCodeFlow({
    clientId: dynamicClientState.clientId,
    scope: dynamicClientForm.scope,
    usePkce: true,
    scenario: 'dynamic-client-registration',
    returnTo: '/dynamic-client'
  })
}

async function deleteDynamicClient() {
  if (!dynamicClientState.id) {
    return
  }
  try {
    await oauth2Api.deleteClient(dynamicClientState.id)
    result.value = {
      operation: 'dynamic_client_delete',
      id: dynamicClientState.id,
      clientId: dynamicClientState.clientId,
      message: '已删除动态注册客户端。'
    }
    dynamicClientState.id = ''
    dynamicClientState.clientId = ''
    dynamicClientState.lastOperation = '动态客户端已删除'
    ElMessage.success('动态客户端已删除')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

function writeDynamicClientSummary() {
  result.value = {
    operation: 'dynamic_client_summary',
    registeredClientId: dynamicClientState.id || null,
    dynamicClientId: dynamicClientState.clientId || dynamicClientForm.clientId,
    requireProofKey: true,
    requestedScope: dynamicClientForm.scope,
    nextStep: dynamicClientState.id
      ? '可使用该 client_id 再次发起 Authorization Code + PKCE。'
      : '先执行动态注册，再进行授权演示。'
  }
}
</script>

<style scoped>
.dynamic-client-container {
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