<template>
  <OAuth2Layout>
    <div class="claims-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">并排对比用户 access_token、用户 id_token、机器 access_token 的 claims 差异</el-descriptions-item>
          <el-descriptions-item label="重点观察">sub、client_id、scope、aud、preferred_username、email、authorities 等字段</el-descriptions-item>
          <el-descriptions-item label="准备条件">先完成一次 PKCE 登录，再获取一次 Client Credentials token，下面的对比表才会完整</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="id_token 更偏身份声明，access_token 更偏资源访问授权；client_credentials token 通常没有用户身份字段。"
      />

      <div class="actions-row">
        <el-button :disabled="!hasAnyJwtForComparison" @click="writeClaimsComparisonResult">输出当前 Claims 差异总结</el-button>
      </div>

      <el-row :gutter="12" class="cards-row">
        <el-col :xs="24" :md="8">
          <el-card shadow="never">
            <template #header><span>用户 Access Token</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="状态">{{ decodedUserAccessToken ? '已获取' : '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="sub">{{ summarizeClaim(decodedUserAccessToken?.payload?.sub) }}</el-descriptions-item>
              <el-descriptions-item label="scope">{{ summarizeClaim(decodedUserAccessToken?.payload?.scope) }}</el-descriptions-item>
              <el-descriptions-item label="client_id">{{ summarizeClaim(decodedUserAccessToken?.payload?.client_id) }}</el-descriptions-item>
              <el-descriptions-item label="token">
                <div class="token-box">{{ maskToken(tokenState.accessToken) || '暂无' }}</div>
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
              <el-descriptions-item label="token">
                <div class="token-box">{{ maskToken(tokenState.idToken) || '暂无' }}</div>
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
              <el-descriptions-item label="client_id">{{ summarizeClaim(decodedMachineAccessToken?.payload?.client_id) }}</el-descriptions-item>
              <el-descriptions-item label="token">
                <div class="token-box">{{ maskToken(machineToken) || '暂无' }}</div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never">
        <template #header><span>关键 Claims 对比</span></template>
        <el-table :data="claimsComparisonRows" border table-layout="fixed" empty-text="先获取用户 token 或 M2M token，再查看对比结果。">
          <el-table-column prop="claim" label="Claim" min-width="120" />
          <el-table-column prop="userAccessToken" label="用户 access_token" min-width="160" show-overflow-tooltip />
          <el-table-column prop="idToken" label="用户 id_token" min-width="160" show-overflow-tooltip />
          <el-table-column prop="machineAccessToken" label="机器 access_token" min-width="160" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never">
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
import { ref, reactive, computed } from 'vue'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { decodeJwt, formatClaimValue, buildClaimsRow, buildClaimsHighlights } from '../utils/jwtHelper'
import { maskToken } from '../utils/tokenHelper'
import { getTokenState } from '../utils/tokenHelper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const tokenState = reactive(getTokenState())
const machineToken = ref('')

// 从 sessionStorage 获取 M2M token（如果存在）
const storedM2MToken = sessionStorage.getItem('oauth2_m2m_token')
if (storedM2MToken) {
  machineToken.value = storedM2MToken
}

const decodedUserAccessToken = computed(() => decodeJwt(tokenState.accessToken))
const decodedIdToken = computed(() => decodeJwt(tokenState.idToken))
const decodedMachineAccessToken = computed(() => decodeJwt(machineToken.value))

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

function summarizeClaim(value) {
  return formatClaimValue(value)
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
  padding: 6px 4px;
  overflow-x: hidden;
}

.info-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.actions-row {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.cards-row {
  margin: 12px 0;
  margin-left: 0 !important;
  margin-right: 0 !important;
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
  max-height: 100px;
  overflow-y: auto;
}

.mb16 {
  margin-bottom: 12px;
}
</style>