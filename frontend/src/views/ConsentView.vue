<template>
  <div class="oauth-page">
    <el-card class="oauth-card">
      <template #header>
        <div class="header-row">
          <div>
            <div class="page-title">授权确认</div>
            <div class="page-subtitle">请确认要授予这个应用的访问权限</div>
          </div>
          <el-tag type="primary">Authorization Code</el-tag>
        </div>
      </template>

      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="mb16"
      />

      <el-descriptions :column="1" border class="mb16">
        <el-descriptions-item label="client_id">{{ clientId || '未知客户端' }}</el-descriptions-item>
        <el-descriptions-item label="scope">
          <div class="scope-list">
            <el-tag v-for="scope in scopes" :key="scope" class="scope-tag">{{ scope }}</el-tag>
            <span v-if="!scopes.length">未申请额外 scope</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-form @submit.prevent>
        <el-form-item>
          <el-checkbox-group v-model="approvedScopes">
            <el-checkbox v-for="scope in scopes" :key="scope" :label="scope">
              允许访问 {{ scope }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="actions">
          <el-button type="primary" :loading="submitting" @click="approveConsent">同意授权</el-button>
          <el-button :loading="submitting" @click="denyConsent">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'

const route = useRoute()
const submitting = ref(false)
const error = ref('')
const clientId = computed(() => getSingleValue(route.query.client_id))
const state = computed(() => getSingleValue(route.query.state))
const csrfToken = computed(() => getSingleValue(route.query._csrf))
const csrfParameter = computed(() => getSingleValue(route.query._csrf_parameter) || '_csrf')
const scopes = computed(() => normalizeScopes(route.query.scope))
const approvedScopes = ref([...scopes.value])

function approveConsent() {
  submitConsent(approvedScopes.value)
}

function denyConsent() {
  submitConsent([])
}

function submitConsent(scopeValues) {
  if (!clientId.value || !state.value) {
    error.value = '缺少 client_id 或 state 参数，无法继续授权。'
    return
  }

  if (!csrfToken.value) {
    error.value = '缺少 CSRF 参数，无法继续授权。'
    return
  }

  submitting.value = true
  error.value = ''

  const form = document.createElement('form')
  form.method = 'post'
  form.action = '/oauth2/authorize'
  appendInput(form, 'client_id', clientId.value)
  appendInput(form, 'state', state.value)
  appendInput(form, csrfParameter.value, csrfToken.value)
  scopeValues.forEach(scope => appendInput(form, 'scope', scope))
  document.body.appendChild(form)
  ElMessage.success(scopeValues.length ? '正在提交授权结果' : '正在取消授权')
  form.submit()
}

function appendInput(form, name, value) {
  const input = document.createElement('input')
  input.type = 'hidden'
  input.name = name
  input.value = value
  form.appendChild(input)
}

function getSingleValue(value) {
  return typeof value === 'string' ? value : Array.isArray(value) ? value[0] : ''
}

function normalizeScopes(value) {
  const raw = Array.isArray(value) ? value.flatMap(item => item.split(' ')) : typeof value === 'string' ? value.split(' ') : []
  return raw.map(item => item.trim()).filter(Boolean)
}
</script>

<style scoped>
.oauth-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.oauth-card {
  width: min(760px, 100%);
}
.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.page-subtitle {
  margin-top: 6px;
  color: #606266;
}
.mb16 {
  margin-bottom: 16px;
}
.scope-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.scope-tag {
  margin: 0;
}
.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
