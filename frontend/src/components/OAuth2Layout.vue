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
        <el-aside class="side-nav">
          <el-menu class="side-menu" :default-active="currentRoute" :default-openeds="defaultOpeneds" @select="handleNavSelect">
            <el-sub-menu v-for="group in navGroups" :key="group.key" :index="group.key">
              <template #title>
                <el-icon><Document /></el-icon>
                <span>{{ group.label }}</span>
              </template>
              <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
                <el-icon><Document /></el-icon>
                <span>{{ item.label }}</span>
              </el-menu-item>
            </el-sub-menu>
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

const navGroups = [
  {
    key: 'oauth2-demos',
    label: 'OAuth2 流程演示',
    items: [
      { path: '/pkce', label: '1. Authorization Code + PKCE' },
      { path: '/m2m', label: '2. Client Credentials' },
      { path: '/client-auth', label: '3. Client 认证方式差异' },
      { path: '/no-pkce', label: '4. Authorization Code without PKCE 对比' },
      { path: '/token-lifecycle', label: '5. Access Token 过期与 Refresh Token 轮换' },
      { path: '/dynamic-client', label: '6. Dynamic Client Registration' },
      { path: '/par', label: '7. Pushed Authorization Request（PAR）' },
      { path: '/device', label: '8. Device Code' },
      { path: '/claims', label: '9. JWT Claims 差异' },
      { path: '/scenarios', label: '10. 场景说明' },
      { path: '/clients', label: '11. Client 管理' },
      { path: '/flows-demo', label: 'Flows Demo', newWindow: true }
    ]
  }
]

const navItems = navGroups.flatMap(group => group.items)
const defaultOpeneds = navGroups.map(group => group.key)

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
  padding: 12px 12px 8px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
}

.side-nav-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.side-nav-subtitle {
  font-size: 11px;
  color: #909399;
}

.side-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding: 6px 6px;
}

.side-menu::-webkit-scrollbar {
  width: 4px;
}

.side-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

:deep(.el-menu-item) {
  border-radius: 6px;
  margin: 2px 0;
  height: 38px;
  line-height: 38px;
  padding: 0 10px;
  font-size: 13px;
  transition: all 0.3s ease;
}

:deep(.el-menu-item:hover) {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(2px);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 6px;
  font-size: 15px;
}

/* Main Content Area */
.content-main {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .side-nav {
    width: 240px;
  }
}

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