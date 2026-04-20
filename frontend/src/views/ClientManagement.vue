<template>
  <el-container class="layout">
    <!-- Header -->
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24" color="#fff"><Key /></el-icon>
        <span class="title">OAuth2 Client 管理</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="goFlows">OAuth2 场景演示</el-button>
        <span class="username">{{ username }}</span>
        <el-button type="default" size="small" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-main>
      <!-- Toolbar -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增 Client</el-button>
        <el-button :icon="Refresh" @click="loadClients" :loading="tableLoading">刷新</el-button>
      </div>

      <!-- Table -->
      <el-table :data="clients" v-loading="tableLoading" border stripe style="width: 100%">
        <el-table-column prop="clientId" label="Client ID" min-width="160" />
        <el-table-column prop="clientName" label="名称" min-width="120" />
        <el-table-column label="认证方式" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="m in row.clientAuthenticationMethods"
              :key="m"
              size="small"
              class="tag-gap"
            >{{ m }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授权类型" min-width="220">
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
        <el-table-column label="Scopes" min-width="160">
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
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text :icon="Edit" @click="openEdit(row.id)">编辑</el-button>
            <el-button type="danger" size="small" text :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
import { Plus, Edit, Delete, Refresh, Key, Minus } from '@element-plus/icons-vue'
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

function goFlows() {
  router.push('/flows')
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
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
}
.username {
  font-size: 14px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.tag-gap {
  margin: 2px 2px;
}
.uri-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
