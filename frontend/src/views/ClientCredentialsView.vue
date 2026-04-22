<template>
  <OAuth2Layout>
    <div class="m2m-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="适用场景">微服务 / BFF / 定时任务 / 后端到后端 API</el-descriptions-item>
          <el-descriptions-item label="说明">可直接切换不同客户端，观察 client_credentials 下 read / write 权限是否生效</el-descriptions-item>
          <el-descriptions-item label="资源权限">/resource/read 需要 read，/resource/write 需要 write</el-descriptions-item>
          <el-descriptions-item label="生命周期">支持演示 introspection 与 revocation，观察 token 激活状态变化</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-form :model="m2mForm" inline class="form-row">
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
        type="info"
        show-icon
        :closable="false"
        :title="m2mSelectedClient.description"
      />

      <div class="actions-row">
        <el-button type="primary" :icon="Key" @click="getM2mToken">获取服务 Token</el-button>
        <el-button type="success" :disabled="!m2mToken" @click="callReadWithM2m">调用只读资源</el-button>
        <el-button type="danger" :disabled="!m2mToken" @click="callWriteWithM2m">调用写资源</el-button>
        <el-button :disabled="!m2mLifecycleToken" @click="introspectM2mToken">Introspect Token</el-button>
        <el-button type="warning" :disabled="!m2mToken" @click="revokeM2mToken">Revoke Token</el-button>
      </div>

      <el-card shadow="never" class="token-card">
        <template #header><span>M2M Token</span></template>
        <div class="token-box">{{ m2mToken || '暂无' }}</div>
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
import { handleOAuth2Error, generateBasicAuth } from '../utils/oauth2Helper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const m2mToken = ref('')
const m2mLastIssuedToken = ref('')

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

const m2mForm = reactive({
  selectedClientKey: m2mClients[0].key,
  clientId: m2mClients[0].clientId,
  clientSecret: m2mClients[0].clientSecret,
  scope: m2mClients[0].scope
})

const m2mSelectedClient = computed(
  () => m2mClients.find(client => client.key === m2mForm.selectedClientKey) || m2mClients[0]
)

const m2mLifecycleToken = computed(() => m2mToken.value || m2mLastIssuedToken.value)

async function getM2mToken() {
  try {
    const basic = generateBasicAuth(m2mForm.clientId, m2mForm.clientSecret)
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
    ElMessage.success('Token 获取成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function introspectM2mToken() {
  try {
    const token = m2mLifecycleToken.value
    const basic = generateBasicAuth(m2mForm.clientId, m2mForm.clientSecret)
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
      inspectedTokenPreview: m2mToken.value ? `${m2mToken.value.slice(0, 20)}...` : '无',
      introspection: data
    }
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}

async function revokeM2mToken() {
  try {
    const token = m2mToken.value
    const basic = generateBasicAuth(m2mForm.clientId, m2mForm.clientSecret)
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
      revokedTokenPreview: token ? `${token.slice(0, 20)}...` : '无',
      message: 'access_token 已撤销。此时再做 introspection，active 应为 false。'
    }
    ElMessage.success('M2M access_token 已撤销')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
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
    ElMessage.success('读取资源成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
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
    ElMessage.success('写入资源成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
}
</script>

<style scoped>
.m2m-container {
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

.token-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.token-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 150px;
  overflow-y: auto;
}
</style>