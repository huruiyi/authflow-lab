<template>
  <OAuth2Layout>
    <div class="scenarios-container">
      <el-card shadow="never" class="intro-card">
        <template #header>
          <div class="card-header">
            <el-icon :size="24" color="#409eff">
              <InfoFilled/>
            </el-icon>
            <span>OAuth2 真实使用场景说明</span>
          </div>
        </template>
        <p class="intro-text">
          本项目演示了 OAuth2 2.0 和 OIDC 的核心流程以及在真实业务场景中的应用。
          通过不同的授权模式和Token管理策略，满足各种应用场景的安全需求。
        </p>
      </el-card>

      <el-card shadow="never" class="timeline-card">
        <el-timeline class="compact-timeline">
          <el-timeline-item timestamp="前后端分离登录" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>前后端分离应用登录</h4>
              <p>用户在 Vue 前端点击登录，跳转授权服务器，登录并授权后回调前端，前端使用 code + code_verifier 换取 token。</p>
              <el-tag size="small" type="success">Authorization Code + PKCE</el-tag>
            </div>
          </el-timeline-item>

          <el-timeline-item timestamp="UserInfo" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>获取用户信息</h4>
              <p>前端拿到 access_token 后，调用 OIDC UserInfo 端点，读取标准用户声明，如 sub、preferred_username、email。</p>
              <el-tag size="small" type="primary">OIDC UserInfo</el-tag>
            </div>
          </el-timeline-item>

          <el-timeline-item timestamp="Refresh Token" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>Token 刷新机制</h4>
              <p>access_token 过期前后，客户端可通过 refresh_token 向 token 端点换取新的 access_token，避免用户频繁重新登录。</p>
              <el-tag size="small" type="warning">Token Lifecycle</el-tag>
            </div>
          </el-timeline-item>

          <el-timeline-item timestamp="业务 API 权限控制" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>资源服务器权限控制</h4>
              <p>资源服务器根据 scope 控制 read / write 权限，确保只有具备相应权限的 token 才能访问对应的资源。</p>
              <el-tag size="small" type="danger">Scope-based Access Control</el-tag>
            </div>
          </el-timeline-item>

          <el-timeline-item timestamp="设备码流程" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>无输入设备授权</h4>
              <p>设备获取 device_code 和 user_code，提示用户去浏览器输入 user_code 完成授权，设备再轮询 token 端点获取结果。</p>
              <el-tag size="small" type="info">Device Code Flow</el-tag>
            </div>
          </el-timeline-item>

          <el-timeline-item timestamp="服务间调用" placement="top" :hollow="true">
            <div class="timeline-content">
              <h4>后端服务间认证</h4>
              <p>微服务架构中，服务间通过 Client Credentials 模式获取 token，实现安全的 服务到服务 调用。</p>
              <el-tag size="small" type="primary">Client Credentials</el-tag>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-row :gutter="12" class="feature-row">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32" color="#409eff">
                <Lock/>
              </el-icon>
            </div>
            <h3>安全性</h3>
            <p>采用 PKCE、Token 轮换等安全机制，保护授权流程和用户数据安全。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32" color="#67c23a">
                <CircleCheck/>
              </el-icon>
            </div>
            <h3>标准化</h3>
            <p>完全遵循 OAuth2 2.0 和 OIDC 标准，确保与各类服务的兼容性。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <el-icon :size="32" color="#e6a23c">
                <Star/>
              </el-icon>
            </div>
            <h3>灵活性</h3>
            <p>支持多种授权模式和客户端类型，满足不同场景的认证授权需求。</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </OAuth2Layout>
</template>

<script setup>
import OAuth2Layout from '../components/OAuth2Layout.vue'
</script>

<style scoped>
.scenarios-container {
  padding: 6px 4px;
  overflow-x: hidden;
}

.intro-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.intro-text {
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
  margin: 0;
}

.timeline-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.timeline-content {
  padding: 6px 0;
}

.timeline-content h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.timeline-content p {
  margin: 0 0 8px 0;
  font-size: 13px;
  line-height: 1.5;
  color: #606266;
}

.feature-card {
  text-align: center;
  border-radius: 8px;
  transition: all 0.3s ease;
  height: 100%;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.feature-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.feature-card h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.feature-card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #909399;
}

.feature-row {
  margin-left: 0 !important;
  margin-right: 0 !important;
}

:deep(.el-card__body) {
  padding: 14px;
}

:deep(.el-card__header) {
  padding: 12px 14px;
}

:deep(.el-timeline-item__timestamp) {
  font-size: 12px;
}

:deep(.compact-timeline) {
  margin-left: 0;
  padding-left: 0;
}

:deep(.compact-timeline .el-timeline-item__tail) {
  left: 5px;
}

:deep(.compact-timeline .el-timeline-item__node) {
  left: 0;
}

:deep(.compact-timeline .el-timeline-item__wrapper) {
  padding-left: 18px;
}

@media (max-width: 768px) {
  .intro-card,
  .timeline-card {
    margin-bottom: 10px;
  }

  .feature-icon {
    width: 46px;
    height: 46px;
  }
}
</style>