<template>
  <OAuth2Layout>
    <div class="claims-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">并排对比用户 access_token、用户 id_token、机器 access_token 的 claims 差异</el-descriptions-item>
          <el-descriptions-item label="重点观察">sub、client_id、scope、aud、preferred_username、email、authorities 等字段</el-descriptions-item>
          <el-descriptions-item label="当前方式">本页可直接生成对比所需 token，无需依赖其他 demo 先完成登录或取 token</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-alert
        class="mt16"
        type="info"
        show-icon
        :closable="false"
        title="id_token 更偏身份声明，access_token 更偏资源访问授权；client_credentials token 通常没有用户身份字段。"
      />

      <div class="actions-row">
        <el-button type="primary" @click="startClaimsUserLogin">获取用户 Token</el-button>
        <el-button type="success" @click="getClaimsMachineToken">获取机器 Token</el-button>
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
              <el-descriptions-item label="client_id">{{ claimsState.userClientId || 'spa-public-client' }}</el-descriptions-item>
              <el-descriptions-item label="scope">{{ claimsState.userScope || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="token">
                <div class="token-box">{{ claimsState.userAccessToken || '暂无' }}</div>
              </el-descriptions-item>
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
              <el-descriptions-item label="scope">{{ claimsState.userScope || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="token">
                <div class="token-box">{{ claimsState.userIdToken || '暂无' }}</div>
              </el-descriptions-item>
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
              <el-descriptions-item label="client_id">{{ claimsState.machineClientId || 'm2m-service-client' }}</el-descriptions-item>
              <el-descriptions-item label="scope">{{ claimsState.machineScope || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="token">
                <div class="token-box">{{ claimsState.machineAccessToken || '暂无' }}</div>
              </el-descriptions-item>
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

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { oauth2Api } from '../api/oauth2'
import {
  createOAuth2SyncListener,
  handleOAuth2Error,
  startAuthorizationCodeFlow
} from '../utils/oauth2Helper'
import { decodeJwt, formatClaimValue, buildClaimsRow, buildClaimsHighlights } from '../utils/jwtHelper'

const claimsSyncChannel = 'oauth2-token-sync-claims-view'
let disposeClaimsSync = null

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const claimsState = reactive({
  userAccessToken: '',
  userIdToken: '',
  userRefreshToken: '',
  userScope: '',
  userClientId: 'spa-public-client',
  machineAccessToken: '',
  machineScope: '',
  machineClientId: 'm2m-service-client',
  lastSource: ''
})

const decodedUserAccessToken = computed(() => decodeJwt(claimsState.userAccessToken))
const decodedIdToken = computed(() => decodeJwt(claimsState.userIdToken))
const decodedMachineAccessToken = computed(() => decodeJwt(claimsState.machineAccessToken))

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

function applySyncedTokens(payload) {
  claimsState.userAccessToken = payload.access_token || ''
  claimsState.userIdToken = payload.id_token || ''
  claimsState.userRefreshToken = payload.refresh_token || ''
  claimsState.userScope = payload.scope || ''
  claimsState.userClientId = payload.client_id || 'spa-public-client'
  claimsState.lastSource = 'claims-user-login'
  result.value = {
    operation: 'oauth2_callback_sync_claims',
    message: 'Claims 独立视图已同步新窗口中的用户 Token。',
    scope: payload.scope,
    expires_in: payload.expires_in,
    refresh_token: payload.refresh_token ? '已返回' : '无'
  }
  ElMessage.success('Claims 用户 Token 已同步')
}

onMounted(() => {
  disposeClaimsSync = createOAuth2SyncListener(claimsSyncChannel, applySyncedTokens)
})

onBeforeUnmount(() => {
  disposeClaimsSync?.()
})

function summarizeClaim(value) {
  return formatClaimValue(value)
}

async function startClaimsUserLogin() {
  claimsState.lastSource = 'claims-user-login'
  result.value = {
    operation: 'claims_user_login_prepare',
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    message: '即将为独立 Claims 页面在新窗口中发起用户授权，完成后会把 access_token 与 id_token 同步回当前页。'
  }

  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'claims-user-login',
    openInNewWindow: true,
    syncChannel: claimsSyncChannel,
    syncTarget: 'claims',
    returnTo: '/claims'
  })
}

async function getClaimsMachineToken() {
  try {
    const clientId = 'm2m-service-client'
    const clientSecret = 'm2m-secret'
    const scope = 'read write'
    const basic = btoa(`${clientId}:${clientSecret}`)
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      scope
    }, {
      Authorization: `Basic ${basic}`
    })

    claimsState.machineAccessToken = data.access_token || ''
    claimsState.machineScope = data.scope || scope
    claimsState.machineClientId = clientId
    claimsState.lastSource = 'claims-machine-token'
    result.value = {
      operation: 'claims_machine_token',
      clientId,
      requestedScope: scope,
      response: data,
      message: 'Claims 独立视图已单独获取机器 Token。'
    }
    ElMessage.success('Claims 机器 Token 获取成功')
  } catch (e) {
    result.value = handleOAuth2Error(e, ElMessage.error)
  }
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
</script>

<style scoped>
.claims-container {
  padding: 12px 0;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.actions-row {
  display: flex;
  gap: 12px;
  margin: 20px 0;
  flex-wrap: wrap;
}

.token-box {
  max-width: 100%;
  word-break: break-all;
  white-space: pre-wrap;
  font-family: monospace;
}

.mt16 {
  margin-top: 16px;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
