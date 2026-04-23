<template>
  <OAuth2Layout>
    <div class="no-pkce-container">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="演示目标">对比同一公开客户端在"带 PKCE / 不带 PKCE"两种请求下的行为差异</el-descriptions-item>
          <el-descriptions-item label="当前客户端">spa-public-client（公开客户端，服务端要求 PKCE）</el-descriptions-item>
          <el-descriptions-item label="预期结果">带 PKCE 能完成授权；不带 PKCE 会在授权阶段被拒绝，体现 PKCE 的安全约束</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="info-card">
        <el-alert
            type="warning"
            show-icon
            :closable="false"
            title="PKCE 的核心价值是把授权码绑定到发起方，防止授权码被截获后由其他客户端换 token。"
        />
        <div class="actions-row">
          <el-button type="primary" :icon="Shield" @click="startPkceControlFlow">带 PKCE 发起授权（预期成功）</el-button>
          <el-button type="danger" plain :icon="Unlock" @click="startNoPkceFlow">不带 PKCE 发起授权（预期失败）</el-button>
          <el-button :icon="Document" @click="writeNoPkceSummary">输出对比总结</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span>对比观察点</span></template>
        <el-table :data="noPkceComparisonRows" border>
          <el-table-column prop="dimension" label="维度" min-width="180" />
          <el-table-column prop="withPkce" label="带 PKCE" min-width="260" show-overflow-tooltip />
          <el-table-column prop="withoutPkce" label="不带 PKCE" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header><span>结论</span></template>
        <el-alert
          class="mb16"
          type="success"
          show-icon
          :closable="false"
          title="公开客户端不持有 client_secret，因此必须依赖 PKCE 绑定授权码与发起方。"
        />
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="如果去掉 code_challenge / code_verifier，服务端会拒绝请求，这正是 PKCE 防护生效的表现。"
        />
      </el-card>

      <ApiResultBox :result="result" />
    </div>
  </OAuth2Layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import OAuth2Layout from '../components/OAuth2Layout.vue'
import ApiResultBox from '../components/ApiResultBox.vue'
import { startAuthorizationCodeFlow } from '../utils/oauth2Helper'

const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })

const noPkceComparisonRows = computed(() => [
  {
    dimension: '授权请求参数',
    withPkce: '包含 code_challenge + code_challenge_method=S256',
    withoutPkce: '缺少 code_challenge'
  },
  {
    dimension: '换 token 参数',
    withPkce: '必须提交 code_verifier',
    withoutPkce: '没有 code_verifier（请求应在前置阶段被拒绝）'
  },
  {
    dimension: '服务端行为',
    withPkce: '允许继续授权流程并返回 code',
    withoutPkce: '返回 invalid_request / 要求必须提供 PKCE 参数'
  },
  {
    dimension: '安全含义',
    withPkce: '授权码绑定发起方，降低截获风险',
    withoutPkce: '授权码无法证明发起者身份，存在被截获滥用风险'
  }
])

async function startPkceControlFlow() {
  result.value = {
    operation: 'authorization_code_with_pkce_control',
    message: '即将发起带 PKCE 的授权请求，预期可以正常进入授权并完成换 token。',
    clientId: 'spa-public-client'
  }
  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'with-pkce-control',
    returnTo: '/no-pkce'
  })
}

async function startNoPkceFlow() {
  result.value = {
    operation: 'authorization_code_without_pkce_control',
    message: '即将发起不带 PKCE 的授权请求。对于公开客户端，服务端应拒绝该请求。',
    clientId: 'spa-public-client'
  }
  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: false,
    scenario: 'without-pkce-control',
    returnTo: '/no-pkce'
  })
}

function writeNoPkceSummary() {
  result.value = {
    operation: 'authorization_code_without_pkce_summary',
    clientId: 'spa-public-client',
    expected: {
      withPkce: '授权成功并可换取 token',
      withoutPkce: '授权请求被拒绝，提示缺少 PKCE 参数'
    },
    comparisonRows: noPkceComparisonRows.value,
    securityConclusion: [
      '公开客户端不持有 client_secret，PKCE 是其核心安全机制。',
      '去掉 PKCE 后，授权码可能被截获并被第三方客户端滥用，因此服务端应强制拒绝。'
    ]
  }
}
</script>

<style scoped>
.no-pkce-container {
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

.mb16 {
  margin-bottom: 16px;
}
</style>