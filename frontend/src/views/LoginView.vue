<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <el-icon :size="32" color="#409eff"><Lock /></el-icon>
          <h2>OAuth2 管理后台</h2>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Key"
            autocomplete="current-password"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
        <el-alert v-if="logoutMessage" :title="logoutMessage" type="success" show-icon :closable="false" class="return-alert" />
        <el-alert v-if="returnToLabel" :title="returnToLabel" type="info" show-icon :closable="false" class="return-alert" />
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User, Key } from '@element-plus/icons-vue'
import { authApi } from '../api/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const error = ref('')

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const oidcLogoutCompleted = sessionStorage.getItem('oauth2_oidc_logout_message') === '1'
if (oidcLogoutCompleted) {
  sessionStorage.removeItem('oauth2_oidc_logout_message')
}

const returnTo = computed(() => normalizeReturnTo(route.query.returnTo))
const returnToLabel = computed(() => returnTo.value ? `登录成功后将返回 ${returnTo.value}` : '')
const logoutMessage = computed(() => oidcLogoutCompleted ? 'OIDC 会话已退出，请重新登录。' : '')

async function handleLogin() {
  error.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authApi.login(form.username, form.password)
    await router.push(returnTo.value || '/flows')
  } catch (e) {
    error.value = e.response?.data?.error || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}

function normalizeReturnTo(value) {
  if (typeof value !== 'string' || !value.startsWith('/')) return ''
  return value.startsWith('//') ? '' : value
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
}
.login-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.login-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}
.return-alert {
  margin-bottom: 18px;
}
</style>
