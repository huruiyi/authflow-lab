<template>
  <div class="login-container">
    <div class="login-background"></div>
    <el-card class="login-card fade-in">
      <template #header>
        <div class="login-header">
          <div class="logo-container">
            <el-icon :size="28" color="#fff"><Lock /></el-icon>
          </div>
          <div class="header-text">
            <h2>OAuth2 管理后台</h2>
            <p>安全 · 可靠 · 高效</p>
          </div>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="default"
            :prefix-icon="User"
            autocomplete="username"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="default"
            :prefix-icon="Key"
            autocomplete="current-password"
            show-password
            @keyup.enter="handleLogin"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="default"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
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
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(120, 219, 255, 0.2) 0%, transparent 40%);
  animation: backgroundMove 20s ease-in-out infinite;
}

@keyframes backgroundMove {
  0%, 100% {
    transform: scale(1) rotate(0deg);
  }
  50% {
    transform: scale(1.1) rotate(3deg);
  }
}

.login-card {
  width: 376px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
  padding: 14px 14px;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-container {
  width: 42px;
  height: 42px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.header-text {
  flex: 1;
}

.header-text h2 {
  margin: 0;
  font-size: 18px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.header-text p {
  margin: 2px 0 0 0;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
}

.login-form {
  padding: 12px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 6px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.6);
}

.login-button:active {
  transform: translateY(0);
}

.return-alert {
  margin-bottom: 8px;
  border-radius: 8px;
}

:deep(.el-alert) {
  border-radius: 8px;
  border: none;
}

:deep(.el-form-item__content) {
  line-height: normal;
}

@media (max-width: 480px) {
  .login-card {
    width: 90%;
    max-width: 344px;
  }

  .header-text h2 {
    font-size: 17px;
  }
}
</style>
