<template>
  <el-container class="layout">
    <!-- Header -->
    <el-header class="header">
      <div class="header-left">
        <div class="logo-icon">
          <el-icon :size="28" color="#fff"><Key /></el-icon>
        </div>
        <div class="header-title">
          <span class="title">OAuth2 Client 管理</span>
          <span class="subtitle">安全授权管理中心</span>
        </div>
      </div>
      <div class="header-right">
        <div class="user-info">
          <el-icon :size="18" color="#fff"><User /></el-icon>
          <span class="username">{{ username }}</span>
        </div>
        <el-button type="default" size="small" class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </el-header>

    <el-main class="main-content">
      <el-container class="content-shell">
        <el-aside class="side-nav" width="320px">
          <div class="side-nav-header">
            <div class="side-nav-title">功能导航</div>
            <div class="side-nav-subtitle">快速访问</div>
          </div>
          <el-menu class="side-menu" default-active="/clients" @select="handleNavSelect">
            <el-menu-item v-for="item in flowNavItems" :key="item.path" :index="item.path">
              <el-icon><Document /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
            <el-menu-item index="/clients">
              <el-icon><Setting /></el-icon>
              <span>11. Client 管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-main">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" :icon="Plus" @click="openCreate" class="add-btn">
                新增 Client
              </el-button>
              <el-button :icon="Refresh" @click="loadClients" :loading="tableLoading" class="refresh-btn">
                刷新
              </el-button>
            </div>
            <div class="toolbar-right">
              <el-tag type="info" size="large">共 {{ clients.length }} 个客户端</el-tag>
            </div>
          </div>

          <div class="table-container">
            <el-table :data="clients" v-loading="tableLoading" border stripe class="custom-table">
              <el-table-column prop="clientId" label="Client ID" min-width="180" show-overflow-tooltip />
              <el-table-column prop="clientName" label="名称" min-width="140" show-overflow-tooltip />
              <el-table-column label="认证方式" min-width="200">
                <template #default="{ row }">
                  <el-tag
                    v-for="m in row.clientAuthenticationMethods"
                    :key="m"
                    size="small"
                    class="tag-gap"
                    type="primary"
                  >{{ m }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="授权类型" min-width="240">
                <template #default="{ row }">
                  <el-tag
                    v-for="g in row.authorizationGrantTypes"
                    :key="g"
                    type="success"
                    size="small"
                    class="tag-gap"
                  >{{ g }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Scopes" min-width="180">
                <template #default="{ row }">
                  <el-tag
                    v-for="s in row.scopes"
                    :key="s"
                    type="warning"
                    size="small"
                    class="tag-gap"
                  >{{ s }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="clientIdIssuedAt" label="创建时间" min-width="160" />
              <el-table-column
                label="操作"
                width="180"
                fixed="right"
                class-name="actions-col"
                header-cell-class-name="actions-col-header"
              >
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button type="primary" size="small" :icon="Edit" @click="openEdit(row.id)" class="action-btn">
                      编辑
                    </el-button>
                    <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)" class="action-btn">
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-main>
      </el-container>
    </el-main>
  </el-container>

  <!-- Create / Edit Dialog -->
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑 Client' : '新增 Client'"
    width="680px"
    @close="resetForm"
  >
    <el-form :model="form" :rules="formRules" ref="formRef" label-width="140px">
      <el-form-item label="Client ID" prop="clientId">
        <el-input v-model="form.clientId" :disabled="isEdit" placeholder="my-client" />
      </el-form-item>
      <el-form-item :label="isEdit ? '新密钥（可选）' : 'Client Secret'" prop="clientSecret">
        <el-input
          v-model="form.clientSecret"
          type="password"
          show-password
          :placeholder="isEdit ? '留空则保留原密钥' : '请输入密钥'"
        />
      </el-form-item>
      <el-form-item label="Client 名称" prop="clientName">
        <el-input v-model="form.clientName" placeholder="My Application" />
      </el-form-item>

      <el-form-item label="认证方式" prop="clientAuthenticationMethods">
        <el-checkbox-group v-model="form.clientAuthenticationMethods">
          <el-checkbox label="client_secret_basic">client_secret_basic</el-checkbox>
          <el-checkbox label="client_secret_post">client_secret_post</el-checkbox>
          <el-checkbox label="none">none (公开客户端)</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="授权类型" prop="authorizationGrantTypes">
        <el-checkbox-group v-model="form.authorizationGrantTypes">
          <el-checkbox label="authorization_code">authorization_code</el-checkbox>
          <el-checkbox label="client_credentials">client_credentials</el-checkbox>
          <el-checkbox label="refresh_token">refresh_token</el-checkbox>
          <el-checkbox label="urn:ietf:params:oauth:grant-type:device_code">device_code</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="Redirect URIs">
        <div v-for="(uri, idx) in form.redirectUris" :key="idx" class="uri-row">
          <el-input v-model="form.redirectUris[idx]" placeholder="http://localhost:8080/callback" />
          <el-button :icon="Minus" circle size="small" @click="removeUri(idx)" />
        </div>
        <el-button size="small" :icon="Plus" @click="addUri">添加 URI</el-button>
      </el-form-item>

      <el-form-item label="Post-Logout URIs">
        <div v-for="(uri, idx) in form.postLogoutRedirectUris" :key="idx" class="uri-row">
          <el-input v-model="form.postLogoutRedirectUris[idx]" placeholder="http://localhost:8080" />
          <el-button :icon="Minus" circle size="small" @click="removePostLogoutUri(idx)" />
        </div>
        <el-button size="small" :icon="Plus" @click="addPostLogoutUri">添加 URI</el-button>
      </el-form-item>

      <el-form-item label="Scopes" prop="scopes">
        <el-select
          v-model="form.scopes"
          multiple
          filterable
          allow-create
          placeholder="openid profile email"
          style="width: 100%"
        >
          <el-option label="openid" value="openid" />
          <el-option label="profile" value="profile" />
          <el-option label="email" value="email" />
          <el-option label="read" value="read" />
          <el-option label="write" value="write" />
        </el-select>
      </el-form-item>

      <el-form-item label="要求 PKCE">
        <el-switch v-model="form.requireProofKey" />
      </el-form-item>
      <el-form-item label="要求授权确认">
        <el-switch v-model="form.requireAuthorizationConsent" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clientApi } from '../api/client'
import { authApi } from '../api/auth'

const router = useRouter()
const username = ref('')
const clients = ref([])
const tableLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  clientId: '',
  clientSecret: '',
  clientName: '',
  clientAuthenticationMethods: ['client_secret_basic'],
  authorizationGrantTypes: ['authorization_code'],
  redirectUris: [],
  postLogoutRedirectUris: [],
  scopes: ['openid', 'profile'],
  requireProofKey: false,
  requireAuthorizationConsent: false
})

const form = reactive(defaultForm())

const formRules = {
  clientId: [{ required: true, message: '请输入 Client ID', trigger: 'blur' }],
  clientName: [{ required: true, message: '请输入 Client 名称', trigger: 'blur' }],
  clientAuthenticationMethods: [{ required: true, type: 'array', min: 1, message: '至少选择一种认证方式', trigger: 'change' }],
  authorizationGrantTypes: [{ required: true, type: 'array', min: 1, message: '至少选择一种授权类型', trigger: 'change' }],
  scopes: [{ required: true, type: 'array', min: 1, message: '至少填写一个 Scope', trigger: 'change' }]
}

const flowNavItems = [
  { path: '/pkce', label: '1. Authorization Code + PKCE' },
  { path: '/m2m', label: '2. Client Credentials' },
  { path: '/client-auth', label: '3. Client 认证方式差异' },
  { path: '/no-pkce', label: '4. Authorization Code without PKCE' },
  { path: '/token-lifecycle', label: '5. Access Token 过期与 Refresh Token 轮换' },
  { path: '/dynamic-client', label: '6. Dynamic Client Registration' },
  { path: '/par', label: '7. Pushed Authorization Request（PAR）' },
  { path: '/device', label: '8. Device Code' },
  { path: '/claims', label: '9. JWT Claims 差异' },
  { path: '/scenarios', label: '10. 场景说明' }
]

onMounted(async () => {
  try {
    const { data } = await authApi.status()
    username.value = data.username || ''
  } catch {
    router.push('/login')
    return
  }
  loadClients()
})

async function loadClients() {
  tableLoading.value = true
  try {
    const { data } = await clientApi.findAll()
    clients.value = data
  } catch (e) {
    ElMessage.error('加载失败：' + (e.response?.data?.error || e.message))
  } finally {
    tableLoading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function openEdit(id) {
  isEdit.value = true
  editingId.value = id
  try {
    const { data } = await clientApi.findById(id)
    Object.assign(form, {
      clientId: data.clientId,
      clientSecret: '',
      clientName: data.clientName,
      clientAuthenticationMethods: [...(data.clientAuthenticationMethods || [])],
      authorizationGrantTypes: [...(data.authorizationGrantTypes || [])],
      redirectUris: [...(data.redirectUris || [])],
      postLogoutRedirectUris: [...(data.postLogoutRedirectUris || [])],
      scopes: [...(data.scopes || [])],
      requireProofKey: data.requireProofKey,
      requireAuthorizationConsent: data.requireAuthorizationConsent
    })
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败：' + (e.response?.data?.error || e.message))
  }
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await clientApi.update(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await clientApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadClients()
  } catch (e) {
    ElMessage.error('操作失败：' + (e.response?.data?.error || e.message))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 Client「${row.clientId}」吗？此操作将同时删除关联的授权记录，不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await clientApi.remove(row.id)
    ElMessage.success('删除成功')
    loadClients()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.response?.data?.error || e.message))
    }
  }
}

function handleNavSelect(index) {
  if (index === '/clients') {
    return
  }
  router.push(index)
}

async function handleLogout() {
  await authApi.logout().catch(() => {})
  router.push('/login')
}

function addUri() { form.redirectUris.push('') }
function removeUri(idx) { form.redirectUris.splice(idx, 1) }
function addPostLogoutUri() { form.postLogoutRedirectUris.push('') }
function removePostLogoutUri(idx) { form.postLogoutRedirectUris.splice(idx, 1) }
</script>

<style scoped>
.layout {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 13px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.username {
  font-weight: 500;
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
  margin: 1px 0;
  height: 34px;
  line-height: 34px;
  padding: 0 8px;
  font-size: 12px;
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
  margin-right: 5px;
  font-size: 14px;
}

/* Main Content Area */
.content-main {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 0 4px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 8px 16px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.5);
}

.refresh-btn {
  padding: 8px 14px;
  font-weight: 500;
  border-radius: 8px;
}

.table-container {
  flex: 1;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 8px;
}

.custom-table {
  height: 100%;
}

:deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 6px;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

:deep(.el-table th) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
  padding: 10px 8px;
}

:deep(.custom-table .el-table__fixed-right th),
:deep(.custom-table .el-table__fixed-right-patch) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

:deep(.custom-table .actions-col-header .cell) {
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

:deep(.custom-table .el-table__fixed-right .actions-col-header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

:deep(.custom-table .el-table__fixed-right .actions-col-header .cell) {
  color: #fff !important;
  visibility: visible !important;
  opacity: 1 !important;
}

:deep(.el-table td) {
  padding: 8px;
  font-size: 13px;
}

:deep(.el-table__row) {
  transition: all 0.3s ease;
}

:deep(.el-table__row:hover) {
  background: rgba(102, 126, 234, 0.05);
  transform: scale(1.01);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  padding: 8px 16px;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.tag-gap {
  margin: 2px;
  border-radius: 4px;
  font-weight: 500;
}

/* Dialog Styles */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
  margin: 0;
}

:deep(.el-dialog__title) {
  color: white;
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  background: #f8f9fa;
  border-top: 1px solid #ebeef5;
}

.uri-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

/* Responsive Design */
@media (max-width: 1200px) {

  .table-container {
    padding: 16px;
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

  .table-container {
    overflow-x: auto;
  }

  .custom-table {
    height: auto;
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

  .user-info {
    display: none;
  }

  .main-content {
    padding: 12px;
    height: auto;
  }

  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .toolbar-left {
    flex-direction: column;
  }
}
</style>
