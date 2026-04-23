<template>
  <el-container class="oauth2-layout">
    <!-- Header -->
    <el-header class="header">
      <div class="header-left">
        <div class="logo-icon">
          <el-icon :size="28" color="#fff"><Connection /></el-icon>
        </div>
        <div class="header-title">
          <span class="title">OAuth2 功能演示</span>
          <span class="subtitle">{{ currentTitle }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" class="all-in-one-btn" @click="openAllInOne">
          All in One
        </el-button>
        <el-button type="default" size="small" class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </el-header>

    <!-- Main Content -->
    <el-main class="main-content">
      <el-container class="content-shell">
        <!-- Navigation Sidebar -->
        <el-aside class="side-nav" width="320px">
          <div class="side-nav-header">
            <div class="side-nav-title">OAuth2 流程演示</div>
            <div class="side-nav-subtitle">快速访问</div>
          </div>
          <el-menu class="side-menu" :default-active="currentRoute" @select="handleNavSelect">
            <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- Page Content -->
        <el-main class="content-main">
          <slot></slot>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/pkce', label: '1. Authorization Code + PKCE', icon: 'Key' },
  { path: '/m2m', label: '2. Client Credentials', icon: 'Connection' },
  { path: '/client-auth', label: '3. Client 认证方式差异', icon: 'Lock' },
  { path: '/no-pkce', label: '4. Authorization Code without PKCE', icon: 'Warning' },
  { path: '/token-lifecycle', label: '5. Access Token 过期与 Refresh Token 轮换', icon: 'Refresh' },
  { path: '/dynamic-client', label: '6. Dynamic Client Registration', icon: 'Plus' },
  { path: '/par', label: '7. Pushed Authorization Request（PAR）', icon: 'Upload' },
  { path: '/device', label: '8. Device Code', icon: 'Monitor' },
  { path: '/claims', label: '9. JWT Claims 差异', icon: 'Tickets' },
  { path: '/scenarios', label: '10. 场景说明', icon: 'Reading' },
  { path: '/clients', label: '11. Client 管理', icon: 'User' }
]

const currentRoute = computed(() => route.path)

const currentTitle = computed(() => {
  const currentItem = navItems.find(item => item.path === route.path)
  return currentItem ? currentItem.label : 'OAuth2 演示'
})

function handleNavSelect(path) {
  const item = navItems.find(i => i.path === path)
  if (item?.newWindow) {
    window.open(path, '_blank')
    return
  }
  if (path !== route.path) {
    router.push(path)
  }
}

function openAllInOne() {
  window.open('/flows-demo', '_blank')
}

async function handleLogout() {
  await authApi.logout().catch(() => {})
  // 清除所有认证相关的sessionStorage
  sessionStorage.removeItem('oauth2_access_token')
  sessionStorage.removeItem('oauth2_id_token')
  sessionStorage.removeItem('oauth2_refresh_token')
  sessionStorage.removeItem('oauth2_scope')
  sessionStorage.removeItem('oauth2_client_id')
  sessionStorage.removeItem('oauth2_state')
  sessionStorage.removeItem('pkce_code_verifier')
  sessionStorage.removeItem('oauth2_demo_scenario')
  sessionStorage.removeItem('oauth2_return_to')
  router.push('/login')
}
</script>

<style scoped>
.oauth2-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.main-content {
  padding: 8px;
  height: calc(100vh - 64px);
  overflow-y: auto;
}

.content-shell {
  height: 100%;
  gap: 8px;
  background: transparent;
}

/* Header Styles */
.header {
  height: 52px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
  position: relative;
  z-index: 10;
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.header-title {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 0;
  min-width: 0;
  white-space: nowrap;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.3px;
}

.subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
  margin-left: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subtitle::before {
  content: '|';
  margin-right: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logout-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  border-radius: 8px;
  padding: 6px 12px;
  transition: all 0.3s ease;
}

.all-in-one-btn {
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  font-weight: 600;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

/* Side Navigation */
.side-nav {
  width: 320px;
  max-width: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.side-nav-header {
  padding: 10px 10px 8px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
}

.side-nav-title {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 2px;
}

.side-nav-subtitle {
  font-size: 10px;
  color: #909399;
}

.side-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding: 4px;
  --el-menu-item-height: 34px;
  --el-menu-sub-item-height: 34px;
}

.side-menu::-webkit-scrollbar {
  width: 4px;
}

.side-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 6px;
  margin: 1px 0;
  height: 34px;
  line-height: 34px;
  padding: 0 8px;
  font-size: 12px;
  transition: all 0.3s ease;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(2px);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.side-menu :deep(.el-menu-item .el-icon) {
  margin-right: 5px;
  font-size: 14px;
}

.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}

/* Main Content Area */
.content-main {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* Responsive Design */

@media (max-width: 960px) {
  .content-shell {
    display: block;
    height: auto;
    overflow-y: auto;
  }

  .side-nav {
    width: 100%;
    margin-bottom: 20px;
    max-height: 300px;
  }

  .main-content {
    height: auto;
    overflow: visible;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 16px;
    height: 56px;
  }

  .header-title {
    display: none;
  }

  .main-content {
    padding: 12px;
    height: auto;
  }
}
</style>