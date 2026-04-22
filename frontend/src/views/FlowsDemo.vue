<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <el-icon :size="24" color="#fff"><Connection /></el-icon>
        <span class="title">OAuth2 真实使用场景演示</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="goClients">Client 管理</el-button>
        <el-button type="default" size="small" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-main>
      <el-container class="content-shell">
        <el-aside class="side-nav">
          <div class="side-nav-title">功能导航</div>
          <el-menu class="side-menu" :default-active="activeTab" @select="handleNavSelect">
            <el-menu-item v-for="item in flowNavItems" :key="item.key" :index="item.key">
              {{ item.label }}
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-main">
          <el-tabs v-model="activeTab" type="border-card" class="content-tabs">
        <el-tab-pane label="1. Authorization Code + PKCE" name="pkce">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">SPA / Vue / React / 移动端公开客户端</el-descriptions-item>
            <el-descriptions-item label="Client">spa-public-client</el-descriptions-item>
            <el-descriptions-item label="特点">无 client_secret，前端生成 PKCE，跳转授权页，换取 access_token / refresh_token</el-descriptions-item>
          </el-descriptions>

          <div class="actions-row">
            <el-button type="primary" @click="startPkceFlow">发起授权登录</el-button>
            <el-button type="warning" plain @click="startScopeConsentDemo">演示 Scope 缩减授权</el-button>
            <el-button @click="loadDiscovery">查看 Discovery 文档</el-button>
            <el-button @click="loadJwks">查看 JWK Set</el-button>
          </div>

          <TokenDisplay
            :access-token="accessToken"
            :refresh-token="refreshToken"
            :id-token="idToken"
            :scope="currentScope"
            :token-type="'Bearer'"
            :expires-in="tokenExpiresIn"
            :show-expiry="true"
            title="当前 Token"
          />

          <div class="actions-row mt16">
            <el-button @click="callPublic">访问公开资源</el-button>
            <el-button type="success" @click="callProfile" :disabled="!accessToken">读取用户 Profile</el-button>
            <el-button type="success" @click="callUserInfo" :disabled="!accessToken">调用 UserInfo</el-button>
            <el-button type="warning" @click="callRead" :disabled="!accessToken">读取业务资源</el-button>
            <el-button type="danger" @click="callWrite" :disabled="!accessToken">写入业务资源</el-button>
            <el-button @click="callTokenInfo" :disabled="!accessToken">查看 Token Claims</el-button>
            <el-button type="primary" plain @click="doRefreshToken" :disabled="!refreshToken">刷新 Access Token</el-button>
            <el-button type="info" plain @click="startOidcLogout" :disabled="!idToken">OIDC Logout</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="2. Client Credentials" name="m2m">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">微服务 / BFF / 定时任务 / 后端到后端 API</el-descriptions-item>
            <el-descriptions-item label="说明">可直接切换不同客户端，观察 client_credentials 下 read / write 权限是否生效</el-descriptions-item>
            <el-descriptions-item label="资源权限">/resource/read 需要 read，/resource/write 需要 write</el-descriptions-item>
            <el-descriptions-item label="生命周期">支持演示 introspection 与 revocation，观察 token 激活状态变化</el-descriptions-item>
          </el-descriptions>

          <el-form :model="m2mForm" inline class="mt16">
            <el-form-item label="选择客户端">
              <el-select v-model="m2mForm.selectedClientKey" style="width: 260px" @change="handleM2mClientChange">
                <el-option
                  v-for="client in m2mClients"
                  :key="client.key"
                  :label="client.label"
                  :value="client.key"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="client_id">
              <el-input v-model="m2mForm.clientId" style="width: 220px" />
            </el-form-item>
            <el-form-item label="client_secret">
              <el-input v-model="m2mForm.clientSecret" show-password style="width: 220px" />
            </el-form-item>
            <el-form-item label="scope">
              <el-input v-model="m2mForm.scope" style="width: 220px" />
            </el-form-item>
          </el-form>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            :title="m2mSelectedClient.description"
          />

          <div class="actions-row">
            <el-button type="primary" @click="getM2mToken">获取服务 Token</el-button>
            <el-button type="success" :disabled="!m2mToken" @click="callReadWithM2m">调用只读资源</el-button>
            <el-button type="danger" :disabled="!m2mToken" @click="callWriteWithM2m">调用写资源</el-button>
            <el-button :disabled="!m2mLifecycleToken" @click="introspectM2mToken">Introspect Token</el-button>
            <el-button type="warning" :disabled="!m2mToken" @click="revokeM2mToken">Revoke Token</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>M2M Token</span></template>
            <div class="token-box">{{ m2mToken || '暂无' }}</div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="3. Client 认证方式差异" name="client-auth">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">对比 client_secret_basic、client_secret_post、none 三种 token endpoint 客户端认证模型</el-descriptions-item>
            <el-descriptions-item label="重点理解">机密客户端要证明“我是谁”，公开客户端则不保存 secret，而是依赖 PKCE</el-descriptions-item>
            <el-descriptions-item label="当前客户端">all-in-one-client 同时支持 basic/post；spa-public-client 代表公开客户端</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            title="同一个 token endpoint，机密客户端可以通过 Header 或 Body 认证；公开客户端不应持有 client_secret。"
          />

          <el-row :gutter="16" class="mt16">
            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>机密客户端：basic / post</span></template>
                <el-form :model="clientAuthForm" label-position="top">
                  <el-form-item label="client_id">
                    <el-input v-model="clientAuthForm.confidentialClientId" />
                  </el-form-item>
                  <el-form-item label="client_secret">
                    <el-input v-model="clientAuthForm.confidentialClientSecret" show-password />
                  </el-form-item>
                  <el-form-item label="scope">
                    <el-input v-model="clientAuthForm.confidentialScope" />
                  </el-form-item>
                </el-form>

                <div class="actions-row">
                  <el-button type="primary" @click="getClientAuthMethodToken('basic')">用 Basic 获取 Token</el-button>
                  <el-button type="success" @click="getClientAuthMethodToken('post')">用 Post 获取 Token</el-button>
                </div>

                <el-descriptions :column="1" border class="mt16">
                  <el-descriptions-item label="Basic Token">
                    <div class="token-box">{{ maskToken(clientAuthTokens.basic) || '暂无' }}</div>
                  </el-descriptions-item>
                  <el-descriptions-item label="Post Token">
                    <div class="token-box">{{ maskToken(clientAuthTokens.post) || '暂无' }}</div>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>公开客户端：none + PKCE</span></template>
                <el-form :model="clientAuthForm" label-position="top">
                  <el-form-item label="client_id">
                    <el-input v-model="clientAuthForm.publicClientId" />
                  </el-form-item>
                  <el-form-item label="scope">
                    <el-input v-model="clientAuthForm.publicScope" />
                  </el-form-item>
                </el-form>

                <div class="actions-row">
                  <el-button type="warning" plain @click="startPublicClientAuthDemo">公开客户端发起 PKCE 登录</el-button>
                  <el-button type="danger" plain @click="tryPublicClientCredentials">错误示例：public client 用 client_credentials</el-button>
                </div>

                <el-alert
                  class="mt16"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="公开客户端没有 client_secret，因此不能像服务端那样用 client_credentials 直接向 token 端点换服务 Token。"
                />

                <el-descriptions :column="1" border class="mt16">
                  <el-descriptions-item label="Access Token">
                    <div class="token-box">{{ accessToken || '暂无' }}</div>
                  </el-descriptions-item>
                  <el-descriptions-item label="ID Token">
                    <div class="token-box">{{ idToken || '暂无' }}</div>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" class="mt16">
            <template #header><span>三种认证方式对比</span></template>
            <el-table :data="clientAuthComparisonRows" border>
              <el-table-column prop="dimension" label="对比维度" min-width="180" />
              <el-table-column prop="basic" label="client_secret_basic" min-width="220" show-overflow-tooltip />
              <el-table-column prop="post" label="client_secret_post" min-width="220" show-overflow-tooltip />
              <el-table-column prop="none" label="none" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card shadow="never" class="mt16">
            <template #header><span>差异结论</span></template>
            <div class="actions-row">
              <el-button @click="writeClientAuthSummary">输出当前客户端认证总结</el-button>
            </div>
            <el-alert
              v-for="highlight in clientAuthHighlights"
              :key="highlight"
              class="mt16"
              type="success"
              show-icon
              :closable="false"
              :title="highlight"
            />
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="4. Authorization Code without PKCE 对比" name="no-pkce">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">对比同一公开客户端在“带 PKCE / 不带 PKCE”两种请求下的行为差异</el-descriptions-item>
            <el-descriptions-item label="当前客户端">spa-public-client（公开客户端，服务端要求 PKCE）</el-descriptions-item>
            <el-descriptions-item label="预期结果">带 PKCE 能完成授权；不带 PKCE 会在授权阶段被拒绝，体现 PKCE 的安全约束</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt16"
            type="warning"
            show-icon
            :closable="false"
            title="PKCE 的核心价值是把授权码绑定到发起方，防止授权码被截获后由其他客户端换 token。"
          />

          <div class="actions-row">
            <el-button type="primary" @click="startPkceControlFlow">带 PKCE 发起授权（预期成功）</el-button>
            <el-button type="danger" plain @click="startNoPkceFlow">不带 PKCE 发起授权（预期失败）</el-button>
            <el-button @click="writeNoPkceSummary">输出对比总结</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>对比观察点</span></template>
            <el-table :data="noPkceComparisonRows" border>
              <el-table-column prop="dimension" label="维度" min-width="180" />
              <el-table-column prop="withPkce" label="带 PKCE" min-width="260" show-overflow-tooltip />
              <el-table-column prop="withoutPkce" label="不带 PKCE" min-width="260" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card shadow="never" class="mt16">
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
        </el-tab-pane>

        <el-tab-pane label="5. Access Token 过期与 Refresh Token 轮换" name="token-lifecycle">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">观察 access_token 短过期后的刷新行为，并对比 refresh_token 是否轮换</el-descriptions-item>
            <el-descriptions-item label="对比客户端">spa-public-client（默认复用 refresh_token） vs spa-rotation-client（每次刷新轮换）</el-descriptions-item>
            <el-descriptions-item label="观察重点">expires_in 倒计时、刷新后 refresh_token 是否变化、过期后资源访问结果</el-descriptions-item>
          </el-descriptions>

          <el-row :gutter="16" class="mt16">
            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>启动对比会话</span></template>
                <div class="actions-row">
                  <el-button type="primary" @click="startTokenLifecycleLogin('default')">登录默认策略（复用 refresh_token）</el-button>
                  <el-button type="warning" plain @click="startTokenLifecycleLogin('rotation')">登录轮换策略（刷新后换新 refresh_token）</el-button>
                  <el-button @click="loadTokenLifecycleFromCurrentSession">从当前会话加载</el-button>
                </div>

                <el-alert
                  class="mt16"
                  type="info"
                  show-icon
                  :closable="false"
                  :title="tokenLifecycleState.policyHint"
                />
              </el-card>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header><span>当前状态</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="当前 client_id">{{ tokenLifecycleState.clientId || '未指定' }}</el-descriptions-item>
                  <el-descriptions-item label="access_token 剩余秒数">{{ tokenLifecycleRemainingSecondsDisplay }}</el-descriptions-item>
                  <el-descriptions-item label="刷新次数">{{ tokenLifecycleState.refreshCount }}</el-descriptions-item>
                  <el-descriptions-item label="最近一次刷新结果">{{ tokenLifecycleState.lastRefreshOutcome || '暂无' }}</el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" class="mt16">
            <template #header><span>Token 明细</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="access_token">
                <div class="token-box">{{ accessToken || '暂无' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="refresh_token">
                <div class="token-box">{{ refreshToken || '暂无' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="上一次 refresh_token">
                <div class="token-box">{{ tokenLifecycleState.previousRefreshToken || '暂无' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="scope">{{ currentScope || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <div class="actions-row mt16">
            <el-button type="success" :disabled="!refreshToken" @click="refreshForLifecycleDemo">执行 Refresh Token</el-button>
            <el-button type="danger" plain @click="simulateAccessTokenExpired" :disabled="!accessToken">模拟 access_token 已过期</el-button>
            <el-button type="warning" :disabled="!accessToken" @click="verifyLifecycleReadResource">验证读取资源</el-button>
            <el-button @click="writeTokenLifecycleSummary">输出生命周期总结</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="6. Dynamic Client Registration" name="dynamic-client">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">通过 API 动态注册一个公开客户端，然后直接发起 Authorization Code + PKCE</el-descriptions-item>
            <el-descriptions-item label="关键差异">无需提前在 data.sql 固化，运行时创建 client_id、redirect_uri、scope</el-descriptions-item>
            <el-descriptions-item label="本项目实现">复用 /api/clients 管理接口模拟 DCR 过程，便于本地教学演示</el-descriptions-item>
          </el-descriptions>

          <el-card shadow="never" class="mt16">
            <template #header><span>动态注册参数</span></template>
            <el-form label-width="160px" class="inline-form">
              <el-form-item label="client_id">
                <el-input v-model="dynamicClientForm.clientId" placeholder="dynamic-spa-xxx">
                  <template #append>
                    <el-button @click="regenerateDynamicClientId">重生成</el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="client_name">
                <el-input v-model="dynamicClientForm.clientName" placeholder="动态注册 SPA 客户端" />
              </el-form-item>
              <el-form-item label="scope（空格分隔）">
                <el-input v-model="dynamicClientForm.scope" placeholder="openid profile email read write" />
              </el-form-item>
              <el-form-item label="requireAuthorizationConsent">
                <el-switch v-model="dynamicClientForm.requireAuthorizationConsent" />
              </el-form-item>
            </el-form>

            <div class="actions-row">
              <el-button type="primary" @click="registerDynamicClient">1) 动态注册客户端</el-button>
              <el-button type="success" :disabled="!dynamicClientState.id" @click="startDynamicClientAuthorization">2) 使用新客户端发起授权</el-button>
              <el-button type="danger" plain :disabled="!dynamicClientState.id" @click="deleteDynamicClient">3) 删除动态客户端</el-button>
              <el-button @click="writeDynamicClientSummary">输出 DCR 总结</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="mt16">
            <template #header><span>注册结果</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="registered_client_id">{{ dynamicClientState.id || '未注册' }}</el-descriptions-item>
              <el-descriptions-item label="client_id">{{ dynamicClientState.clientId || '未注册' }}</el-descriptions-item>
              <el-descriptions-item label="最近操作">{{ dynamicClientState.lastOperation || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="7. Pushed Authorization Request（PAR）" name="par">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">由后端代发 PAR，前端拿到 request_uri 后再跳转授权端点</el-descriptions-item>
            <el-descriptions-item label="安全价值">SPA 不直接持有 client_secret，敏感客户端认证由后端代办</el-descriptions-item>
            <el-descriptions-item label="演示方式">BFF / 后端代理式 PAR 演示：前端只保留 PKCE 和浏览器跳转</el-descriptions-item>
          </el-descriptions>

          <el-card shadow="never" class="mt16">
            <template #header><span>PAR 请求参数</span></template>
            <el-form label-width="140px" class="inline-form">
              <el-form-item label="client_id">
                <el-input v-model="parForm.clientId" />
              </el-form-item>
              <el-form-item label="scope">
                <el-input v-model="parForm.scope" />
              </el-form-item>
            </el-form>

            <div class="actions-row">
              <el-button type="primary" @click="startParFlow">1) 提交 PAR 并发起授权</el-button>
              <el-button @click="writeParSummary">输出 PAR 总结</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="mt16">
            <template #header><span>PAR 响应状态</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="request_uri">{{ parState.requestUri || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="expires_in">{{ parState.expiresIn ? `${parState.expiresIn} 秒` : '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="最近状态">{{ parState.lastStatus || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="8. Device Code" name="device">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="适用场景">智能电视、IoT 设备、无浏览器输入能力的终端</el-descriptions-item>
            <el-descriptions-item label="Client">device-flow-client</el-descriptions-item>
            <el-descriptions-item label="特点">设备先申请 device_code，用户在另一终端访问 verification_uri 完成授权</el-descriptions-item>
            <el-descriptions-item label="默认 Scope">profile read（当前设备流演示不申请 openid）</el-descriptions-item>
          </el-descriptions>

          <el-form :model="deviceForm" inline class="mt16">
            <el-form-item label="client_id">
              <el-input v-model="deviceForm.clientId" style="width: 220px" />
            </el-form-item>
            <el-form-item label="client_secret">
              <el-input v-model="deviceForm.clientSecret" show-password style="width: 220px" />
            </el-form-item>
            <el-form-item label="scope">
              <el-input v-model="deviceForm.scope" style="width: 220px" />
            </el-form-item>
          </el-form>

          <div class="actions-row">
            <el-button type="primary" @click="startDeviceFlow">申请 Device Code</el-button>
            <el-button
              type="success"
              :disabled="!deviceAuth.device_code || isPollingDeviceToken"
              :loading="isPollingDeviceToken"
              @click="pollDeviceToken"
            >{{ isPollingDeviceToken ? '轮询中...' : '轮询 Token' }}</el-button>
          </div>

          <el-card shadow="never" class="mt16">
            <template #header><span>Device 授权信息</span></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="device_code"><div class="token-box">{{ deviceAuth.device_code || '暂无' }}</div></el-descriptions-item>
              <el-descriptions-item label="user_code">{{ deviceAuth.user_code || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="verification_uri">{{ deviceAuth.verification_uri || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="verification_uri_complete">{{ deviceAuth.verification_uri_complete || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="interval">{{ deviceAuth.interval || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="9. JWT Claims 差异" name="claims">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="演示目标">并排对比用户 access_token、用户 id_token、机器 access_token 的 claims 差异</el-descriptions-item>
            <el-descriptions-item label="重点观察">sub、client_id、scope、aud、preferred_username、email、authorities 等字段</el-descriptions-item>
            <el-descriptions-item label="当前方式">本页可直接生成对比所需 token，无需依赖其他 demo 先完成登录或取 token</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="mt16"
            type="info"
            show-icon
            :closable="false"
            title="id_token 更偏身份声明，access_token 更偏资源访问授权；client_credentials token 通常没有用户身份字段。"
          />

          <div class="actions-row">
            <el-button type="primary" @click="startClaimsUserLogin">获取用户 Token</el-button>
            <el-button type="success" @click="getClaimsMachineToken">获取机器 Token</el-button>
            <el-button @click="writeClaimsComparisonResult" :disabled="!hasAnyJwtForComparison">输出当前 Claims 差异总结</el-button>
          </div>

          <el-row :gutter="16" class="mt16">
            <el-col :xs="24" :md="8">
              <el-card shadow="never">
                <template #header><span>用户 Access Token</span></template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="状态">{{ decodedUserAccessToken ? '已获取' : '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="sub">{{ summarizeClaim(decodedUserAccessToken?.payload?.sub) }}</el-descriptions-item>
                  <el-descriptions-item label="scope">{{ summarizeClaim(decodedUserAccessToken?.payload?.scope) }}</el-descriptions-item>
                  <el-descriptions-item label="client_id">{{ claimsState.userClientId || 'spa-public-client' }}</el-descriptions-item>
                  <el-descriptions-item label="scope">{{ claimsState.userScope || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ claimsState.userAccessToken || '暂无' }}</div> </el-descriptions-item>
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
                  <el-descriptions-item label="scope">{{ claimsState.userScope || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ claimsState.userIdToken || '暂无' }}</div> </el-descriptions-item>
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
                  <el-descriptions-item label="client_id">{{ claimsState.machineClientId || 'm2m-service-client' }}</el-descriptions-item>
                  <el-descriptions-item label="scope">{{ claimsState.machineScope || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="token"> <div class="token-box">{{ claimsState.machineAccessToken || '暂无' }}</div> </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" class="mt16">
            <template #header><span>关键 Claims 对比</span></template>
            <el-table :data="claimsComparisonRows" border empty-text="先获取用户 token 或 M2M token，再查看对比结果。">
              <el-table-column prop="claim" label="Claim" min-width="180" />
              <el-table-column prop="userAccessToken" label="用户 access_token" min-width="220" show-overflow-tooltip />
              <el-table-column prop="idToken" label="用户 id_token" min-width="220" show-overflow-tooltip />
              <el-table-column prop="machineAccessToken" label="机器 access_token" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card shadow="never" class="mt16">
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
        </el-tab-pane>

        <el-tab-pane label="10. 场景说明" name="scenes">
          <div class="scenes-pane">
          <el-timeline>
            <el-timeline-item timestamp="前后端分离登录" placement="top">
              用户在 Vue 前端点击登录，跳转授权服务器，登录并授权后回调前端，前端使用 code + code_verifier 换取 token。
            </el-timeline-item>
            <el-timeline-item timestamp="UserInfo" placement="top">
              前端拿到 access_token 后，调用 OIDC UserInfo 端点，读取标准用户声明，如 sub、preferred_username、email。
            </el-timeline-item>
            <el-timeline-item timestamp="Refresh Token" placement="top">
              access_token 过期前后，客户端可通过 refresh_token 向 token 端点换取新的 access_token。
            </el-timeline-item>
            <el-timeline-item timestamp="业务 API 权限控制" placement="top">
              资源服务器根据 scope 控制 read / write 权限：[ResourceServerConfig.java:19-25](backend/src/main/java/com/demo/authserver/config/ResourceServerConfig.java#L19-L25)。
            </el-timeline-item>
            <el-timeline-item timestamp="设备码流程" placement="top">
              设备获取 device_code 和 user_code，提示用户去浏览器输入 user_code 完成授权，设备再轮询 token 端点。
            </el-timeline-item>
          </el-timeline>
          </div>
        </el-tab-pane>
          </el-tabs>

          <el-card shadow="never" class="mt16">
            <template #header><span>接口响应 / 调试输出</span></template>
            <pre class="result-box">{{ prettyResult }}</pre>
          </el-card>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import { authApi } from '../api/auth'
import { oauth2Api } from '../api/oauth2'
import {
  buildDynamicClientId,
  createOAuth2SyncListener,
  generateBasicAuth,
  getAuthServerOrigin,
  handleOAuth2Error,
  prepareOAuth2Redirect,
  splitScopeString,
  startAuthorizationCodeFlow as startAuthorizationCodeFlowHelper
} from '../utils/oauth2Helper'
import { getTokenState, saveTokens, clearTokens } from '../utils/tokenHelper'
import TokenDisplay from '../components/TokenDisplay.vue'

const oauth2SyncChannel = 'oauth2-token-sync-flows-demo'
const claimsSyncChannel = 'oauth2-token-sync-flows-demo-claims'
let disposeTokenSync = null
let disposeClaimsSync = null

const authServerOrigin = getAuthServerOrigin()

const m2mClients = [
  {
    key: 'm2m-service-client-rw',
    label: 'm2m-service-client（read + write）',
    clientId: 'm2m-service-client',
    clientSecret: 'm2m-secret',
    scope: 'read write',
    description: '这个客户端申请 read write scope。获取 token 后读取和写入资源都应该成功。'
  },
  {
    key: 'm2m-service-client-read',
    label: 'm2m-service-client（仅 read）',
    clientId: 'm2m-service-client',
    clientSecret: 'm2m-secret',
    scope: 'read',
    description: '这个客户端只申请 read scope。获取 token 后读取资源应成功，写入资源应返回 403。'
  },
  {
    key: 'all-in-one-client-rw',
    label: 'all-in-one-client（read + write）',
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    description: '这个综合测试客户端支持 client_credentials，并申请 read write scope，读取和写入资源都应该成功。'
  }
]

const clientAuthDemoClients = {
  basic: {
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    transport: 'HTTP Header'
  },
  post: {
    clientId: 'all-in-one-client',
    clientSecret: 'all-secret',
    scope: 'read write',
    transport: 'Form Body'
  },
  none: {
    clientId: 'spa-public-client',
    clientSecret: '',
    scope: 'openid profile email read write',
    transport: 'No secret'
  }
}

const flowNavItems = [
  { key: 'pkce', label: '1. Authorization Code + PKCE' },
  { key: 'm2m', label: '2. Client Credentials' },
  { key: 'client-auth', label: '3. Client 认证方式差异' },
  { key: 'no-pkce', label: '4. Authorization Code without PKCE 对比' },
  { key: 'token-lifecycle', label: '5. Access Token 过期与 Refresh Token 轮换' },
  { key: 'dynamic-client', label: '6. Dynamic Client Registration' },
  { key: 'par', label: '7. Pushed Authorization Request（PAR）' },
  { key: 'device', label: '8. Device Code' },
  { key: 'claims', label: '9. JWT Claims 差异' },
  { key: 'scenes', label: '10. 场景说明' }
]
const flowTabNames = flowNavItems.map(item => item.key)

const route = useRoute()
const router = useRouter()
const activeTab = ref(flowTabNames.includes(route.query.tab) ? route.query.tab : 'pkce')
const result = ref({ message: '点击上方按钮开始体验 OAuth2 场景。' })
const tokenState = reactive(getTokenState())
const accessToken = computed(() => tokenState.accessToken)
const idToken = computed(() => tokenState.idToken)
const refreshToken = computed(() => tokenState.refreshToken)
const currentScope = computed(() => tokenState.scope)
const m2mToken = ref('')
const m2mLastIssuedToken = ref('')
const deviceAuth = ref({})
const isPollingDeviceToken = ref(false)
let devicePollingAborted = false

const DEVICE_FLOW_TIMEOUT_MS = 2 * 60 * 1000
const DEFAULT_DEVICE_INTERVAL_SECONDS = 5

const m2mForm = reactive({
  selectedClientKey: m2mClients[0].key,
  clientId: m2mClients[0].clientId,
  clientSecret: m2mClients[0].clientSecret,
  scope: m2mClients[0].scope
})

const deviceForm = reactive({
  clientId: 'device-flow-client',
  clientSecret: 'device-secret',
  scope: 'profile read'
})

const clientAuthForm = reactive({
  confidentialClientId: clientAuthDemoClients.basic.clientId,
  confidentialClientSecret: clientAuthDemoClients.basic.clientSecret,
  confidentialScope: clientAuthDemoClients.basic.scope,
  publicClientId: clientAuthDemoClients.none.clientId,
  publicScope: clientAuthDemoClients.none.scope
})

const clientAuthTokens = reactive({
  basic: '',
  post: ''
})

const dynamicClientForm = reactive({
  clientId: buildDynamicClientId(),
  clientName: '动态注册 SPA 客户端',
  scope: 'openid profile email read write',
  requireAuthorizationConsent: false
})

const dynamicClientState = reactive({
  id: '',
  clientId: '',
  lastOperation: ''
})

const parForm = reactive({
  clientId: 'spa-par-client',
  scope: 'openid profile email read write'
})

const parState = reactive({
  requestUri: '',
  expiresIn: 0,
  lastStatus: ''
})

const tokenLifecycleState = reactive({
  clientId: sessionStorage.getItem('oauth2_client_id') || 'spa-public-client',
  refreshCount: 0,
  previousRefreshToken: '',
  lastRefreshOutcome: '',
  policyHint: '默认可直接复用当前会话。若要对比轮换策略，请点“登录轮换策略”。'
})

const claimsState = reactive({
  userAccessToken: '',
  userIdToken: '',
  userRefreshToken: '',
  userScope: '',
  machineAccessToken: '',
  machineScope: '',
  userClientId: 'spa-public-client',
  machineClientId: 'm2m-service-client',
  lastSource: ''
})

const nowTimestamp = ref(Date.now())
let lifecycleTimer = null

const prettyResult = computed(() => JSON.stringify(result.value, null, 2))
const m2mSelectedClient = computed(
  () => m2mClients.find(client => client.key === m2mForm.selectedClientKey) || m2mClients[0]
)
const m2mLifecycleToken = computed(() => m2mToken.value || m2mLastIssuedToken.value)
const clientAuthComparisonRows = computed(() => {
  const basicConfig = clientAuthDemoClients.basic
  const postConfig = clientAuthDemoClients.post
  const noneConfig = clientAuthDemoClients.none

  return [
    {
      dimension: '是否携带 client_secret',
      basic: '是，放在 Basic Header 中',
      post: '是，放在表单 body 中',
      none: '否，公开客户端不保存密钥'
    },
    {
      dimension: '典型 grant_type',
      basic: 'client_credentials / authorization_code',
      post: 'authorization_code / client_credentials（服务端允许时）',
      none: 'authorization_code + PKCE'
    },
    {
      dimension: '当前演示客户端',
      basic: basicConfig.clientId,
      post: postConfig.clientId,
      none: noneConfig.clientId
    },
    {
      dimension: '请求位置',
      basic: basicConfig.transport,
      post: postConfig.transport,
      none: noneConfig.transport
    },
    {
      dimension: '适用对象',
      basic: '机密客户端 / 服务端应用 / 服务间调用',
      post: '机密客户端，但运行环境不便设置 Authorization Header',
      none: 'SPA / 移动端 / 原生 App'
    }
  ]
})
const clientAuthHighlights = computed(() => {
  const highlights = [
    'client_secret_basic 与 client_secret_post 都属于机密客户端认证，只是密钥传输位置不同。',
    'none 不是“更弱的 basic”，而是公开客户端模型：不保存密钥，靠 PKCE 证明是同一发起方。'
  ]

  if (clientAuthTokens.basic && clientAuthTokens.post) {
    highlights.push('当前 Basic 与 Post 都已成功拿到 token，说明服务端同时接受两种机密客户端认证方式。')
  }

  if (!clientAuthTokens.basic && !clientAuthTokens.post) {
    highlights.push('先分别点一次 Basic / Post 获取 token，再观察机密客户端两种认证方式的差异。')
  }

  return highlights
})
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
const tokenLifecycleRemainingSeconds = computed(() => {
  const expiresAtRaw = sessionStorage.getItem('oauth2_access_token_expires_at')
  const expiresAt = Number(expiresAtRaw)
  if (!Number.isFinite(expiresAt) || expiresAt <= 0) {
    return null
  }
  return Math.floor((expiresAt - nowTimestamp.value) / 1000)
})
const tokenLifecycleRemainingSecondsDisplay = computed(() => {
  if (tokenLifecycleRemainingSeconds.value === null) {
    return '未知（请先获取 token）'
  }
  if (tokenLifecycleRemainingSeconds.value < 0) {
    return `${Math.abs(tokenLifecycleRemainingSeconds.value)} 秒前已过期`
  }
  return `${tokenLifecycleRemainingSeconds.value} 秒`
})
const tokenExpiresIn = computed(() => sessionStorage.getItem('oauth2_access_token_expires_in') || '')

watch(activeTab, (tab) => {
  if (tab === 'token-lifecycle') {
    loadTokenLifecycleFromCurrentSession()
  }
  const nextQuery = { ...route.query }
  if (tab !== 'pkce' && flowTabNames.includes(tab)) {
    nextQuery.tab = tab
  } else {
    delete nextQuery.tab
  }
  router.replace({ query: nextQuery })
})

function handleNavSelect(index) {
  if (index === 'clients') {
    router.push('/clients')
    return
  }
  activeTab.value = index
}

const decodedUserAccessToken = computed(() => decodeJwt(claimsState.userAccessToken))
const decodedIdToken = computed(() => decodeJwt(claimsState.userIdToken))
const decodedMachineAccessToken = computed(() => decodeJwt(claimsState.machineAccessToken))
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
function applySyncedTokens(payload) {
  if (payload?.syncTarget === 'claims') {
    claimsState.userAccessToken = payload.access_token || ''
    claimsState.userIdToken = payload.id_token || ''
    claimsState.userRefreshToken = payload.refresh_token || ''
    claimsState.userScope = payload.scope || ''
    claimsState.userClientId = payload.client_id || 'spa-public-client'
    claimsState.lastSource = 'claims-user-login'
    result.value = {
      operation: 'oauth2_callback_sync_claims',
      message: 'Demo 9 已同步新窗口中的用户 Token。',
      scope: payload.scope,
      expires_in: payload.expires_in,
      refresh_token: payload.refresh_token ? '已返回' : '无'
    }
    ElMessage.success('Demo 9 用户 Token 已同步')
    return
  }

  saveTokens(payload)
  Object.assign(tokenState, getTokenState())
  loadTokenLifecycleFromCurrentSession()
  result.value = {
    operation: 'oauth2_callback_sync',
    message: '已同步新窗口授权结果。',
    scope: payload.scope,
    expires_in: payload.expires_in,
    refresh_token: payload.refresh_token ? '已返回' : '无'
  }
}

function syncTokenStateFromStorage() {
  Object.assign(tokenState, getTokenState())
}

onMounted(() => {
  Object.assign(tokenState, getTokenState())
  disposeTokenSync = createOAuth2SyncListener(oauth2SyncChannel, applySyncedTokens)
  disposeClaimsSync = createOAuth2SyncListener(claimsSyncChannel, applySyncedTokens)
  lifecycleTimer = window.setInterval(() => {
    nowTimestamp.value = Date.now()
  }, 1000)
  loadTokenLifecycleFromCurrentSession()
})

onBeforeUnmount(() => {
  devicePollingAborted = true
  disposeTokenSync?.()
  disposeClaimsSync?.()
  if (lifecycleTimer) {
    window.clearInterval(lifecycleTimer)
    lifecycleTimer = null
  }
})

async function startPkceFlow() {
  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'pkce',
    openInNewWindow: true
  })
}

async function startScopeConsentDemo() {
  result.value = {
    operation: 'scope_consent_demo',
    message: '即将使用 spa-consent-client 发起授权。进入授权确认页后，可以取消勾选 write，只保留 read 进行 scope 缩减授权演示。',
    requestedScope: 'openid profile email read write',
    expectedScopeAfterConsent: '例如 openid profile email read'
  }
  await startAuthorizationCodeFlow({
    clientId: 'spa-consent-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'pkce-consent',
    openInNewWindow: true
  })
}

async function startClaimsUserLogin() {
  claimsState.lastSource = 'claims-user-login'
  result.value = {
    operation: 'claims_user_login_prepare',
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    message: '即将为 Demo 9 在新窗口中发起用户授权，完成后会把 access_token 与 id_token 同步回当前页。'
  }

  await startAuthorizationCodeFlow({
    clientId: 'spa-public-client',
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: 'claims-user-login',
    openInNewWindow: true,
    syncChannel: claimsSyncChannel,
    syncTarget: 'claims',
    returnTo: '/flows?tab=claims'
  })
}

async function getClaimsMachineToken() {
  try {
    const clientId = 'm2m-service-client'
    const clientSecret = 'm2m-secret'
    const scope = 'read write'
    const basic = btoa(`${clientId}:${clientSecret}`)
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      scope
    }, {
      Authorization: `Basic ${basic}`
    })

    claimsState.machineAccessToken = data.access_token || ''
    claimsState.machineScope = data.scope || scope
    claimsState.machineClientId = clientId
    claimsState.lastSource = 'claims-machine-token'
    result.value = {
      operation: 'claims_machine_token',
      clientId,
      requestedScope: scope,
      response: data,
      message: 'Demo 9 已单独获取机器 Token。'
    }
    ElMessage.success('Demo 9 机器 Token 获取成功')
  } catch (e) {
    showError(e)
  }
}

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
    scenario: 'with-pkce-control'
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
    scenario: 'without-pkce-control'
  })
}

async function startTokenLifecycleLogin(mode) {
  const useRotationPolicy = mode === 'rotation'
  const clientId = useRotationPolicy ? 'spa-rotation-client' : 'spa-public-client'
  const policyHint = useRotationPolicy
    ? '当前使用 spa-rotation-client：access_token 默认 30 秒过期，refresh_token 每次刷新后应轮换。'
    : '当前使用 spa-public-client：refresh_token 默认可复用，刷新后通常保持不变。'

  tokenLifecycleState.clientId = clientId
  tokenLifecycleState.refreshCount = 0
  tokenLifecycleState.previousRefreshToken = ''
  tokenLifecycleState.lastRefreshOutcome = ''
  tokenLifecycleState.policyHint = policyHint

  result.value = {
    operation: 'token_lifecycle_login_prepare',
    mode,
    clientId,
    policyHint,
    message: '即将发起授权，完成后返回当前标签继续演示。'
  }

  await startAuthorizationCodeFlow({
    clientId,
    scope: 'openid profile email read write',
    usePkce: true,
    scenario: useRotationPolicy ? 'token-lifecycle-rotation' : 'token-lifecycle-default',
    openInNewWindow: true
  })
}

function loadTokenLifecycleFromCurrentSession() {
  const currentClientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  tokenLifecycleState.clientId = currentClientId
  tokenLifecycleState.policyHint = currentClientId === 'spa-rotation-client'
    ? '当前会话是轮换策略客户端，刷新后 refresh_token 应变化。'
    : '当前会话是默认策略客户端，refresh_token 可能保持不变。'
}

function regenerateDynamicClientId() {
  dynamicClientForm.clientId = buildDynamicClientId()
}

async function registerDynamicClient() {
  try {
    const scopeList = splitScopeString(dynamicClientForm.scope)
    const redirectUri = `${window.location.origin}/callback`
    const payload = {
      clientId: dynamicClientForm.clientId,
      clientSecret: '',
      clientName: dynamicClientForm.clientName,
      clientAuthenticationMethods: ['none'],
      authorizationGrantTypes: ['authorization_code', 'refresh_token'],
      redirectUris: [redirectUri],
      postLogoutRedirectUris: [window.location.origin],
      scopes: scopeList,
      requireProofKey: true,
      requireAuthorizationConsent: dynamicClientForm.requireAuthorizationConsent
    }

    const { data } = await oauth2Api.createClient(payload)
    dynamicClientState.id = data.id
    dynamicClientState.clientId = data.clientId
    dynamicClientState.lastOperation = '动态注册成功'
    result.value = {
      operation: 'dynamic_client_registration',
      payload,
      registered: data,
      message: '动态注册成功，可直接点击“使用新客户端发起授权”。'
    }
    ElMessage.success('动态注册成功')
  } catch (e) {
    showError(e)
  }
}

async function startDynamicClientAuthorization() {
  if (!dynamicClientState.clientId) {
    ElMessage.warning('请先完成动态注册')
    return
  }
  await startAuthorizationCodeFlow({
    clientId: dynamicClientState.clientId,
    scope: dynamicClientForm.scope,
    usePkce: true,
    scenario: 'dynamic-client-registration'
  })
}

async function deleteDynamicClient() {
  if (!dynamicClientState.id) {
    return
  }
  try {
    await oauth2Api.deleteClient(dynamicClientState.id)
    result.value = {
      operation: 'dynamic_client_delete',
      id: dynamicClientState.id,
      clientId: dynamicClientState.clientId,
      message: '已删除动态注册客户端。'
    }
    dynamicClientState.id = ''
    dynamicClientState.clientId = ''
    dynamicClientState.lastOperation = '动态客户端已删除'
    ElMessage.success('动态客户端已删除')
  } catch (e) {
    showError(e)
  }
}

async function startParFlow() {
  try {
    const frontendOrigin = window.location.origin
    const redirectUri = `${frontendOrigin}/callback`
    const state = generateRandomString(32)
    const pkcePair = await generatePkcePair()

    prepareOAuth2Redirect({
      clientId: parForm.clientId,
      usePkce: true,
      scenario: 'par',
      returnTo: '/flows?tab=par',
      syncChannel: oauth2SyncChannel
    }, {
      state,
      codeVerifier: pkcePair.codeVerifier
    })

    const { data } = await oauth2Api.pushedAuthorization({
      response_type: 'code',
      client_id: parForm.clientId,
      redirect_uri: redirectUri,
      scope: parForm.scope,
      state,
      code_challenge: pkcePair.codeChallenge,
      code_challenge_method: 'S256'
    })

    parState.requestUri = data.request_uri || ''
    parState.expiresIn = data.expires_in || 0
    parState.lastStatus = 'PAR 请求成功，准备跳转授权端点'

    result.value = {
      operation: 'par_request',
      clientId: parForm.clientId,
      response: data,
      message: '已获取 request_uri，将使用精简参数发起授权。'
    }

    const authorizeParams = new URLSearchParams({
      client_id: parForm.clientId,
      request_uri: data.request_uri
    })
    window.open(`${authServerOrigin}/oauth2/authorize?${authorizeParams.toString()}`, '_blank')
  } catch (e) {
    parState.lastStatus = 'PAR 请求失败'
    showError(e)
  }
}

async function startAuthorizationCodeFlow({
  clientId,
  scope,
  usePkce = true,
  scenario = 'default',
  openInNewWindow = false,
  syncChannel = oauth2SyncChannel,
  syncTarget = '',
  returnTo = `/flows${activeTab.value === 'pkce' ? '' : `?tab=${activeTab.value}`}`
}) {
  await startAuthorizationCodeFlowHelper({
    clientId,
    scope,
    usePkce,
    scenario,
    openInNewWindow,
    syncChannel,
    syncTarget,
    returnTo
  })
}

async function loadDiscovery() {
  try {
    const { data } = await oauth2Api.getDiscovery()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function loadJwks() {
  try {
    const { data } = await oauth2Api.getJwks()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callPublic() {
  try {
    const { data } = await oauth2Api.callPublicResource()
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callProfile() {
  try {
    const { data } = await oauth2Api.callProfileResource(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callUserInfo() {
  try {
    const { data } = await oauth2Api.getUserInfo(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callRead() {
  try {
    const { data } = await oauth2Api.callReadResource(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callWrite() {
  try {
    const { data } = await oauth2Api.callWriteResource(accessToken.value, {
      orderNo: 'ORD-' + Date.now(),
      amount: 299,
      status: 'PAID'
    })
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function callTokenInfo() {
  try {
    const { data } = await oauth2Api.callTokenInfo(accessToken.value)
    result.value = data
  } catch (e) {
    showError(e)
  }
}

async function doRefreshToken() {
  const clientId = sessionStorage.getItem('oauth2_client_id') || 'spa-public-client'
  try {
    const previousRefreshToken = refreshToken.value
    const { data } = await oauth2Api.refreshToken({
      grant_type: 'refresh_token',
      client_id: clientId,
      refresh_token: refreshToken.value
    })
    saveTokens(data)
    syncTokenStateFromStorage()
    result.value = data
    tokenLifecycleState.previousRefreshToken = previousRefreshToken
    tokenLifecycleState.lastRefreshOutcome = data.refresh_token && data.refresh_token !== previousRefreshToken
      ? 'refresh_token 已轮换'
      : 'refresh_token 复用或服务端未返回新值'
    ElMessage.success('refresh_token 刷新成功')
  } catch (e) {
    showError(e)
  }
}

async function refreshForLifecycleDemo() {
  const previousRefreshToken = refreshToken.value
  await doRefreshToken()
  tokenLifecycleState.refreshCount += 1
  tokenLifecycleState.previousRefreshToken = previousRefreshToken
}

function simulateAccessTokenExpired() {
  const expiredAt = Date.now() - 1000
  sessionStorage.setItem('oauth2_access_token_expires_at', String(expiredAt))
  tokenLifecycleState.lastRefreshOutcome = '已手动标记 access_token 过期（仅前端演示）'
  result.value = {
    operation: 'simulate_access_token_expired',
    expiresAt: expiredAt,
    message: '已把 access_token 过期时间改为过去时刻，可继续点“验证读取资源”或“执行 Refresh Token”。'
  }
}

async function verifyLifecycleReadResource() {
  try {
    const { data } = await oauth2Api.callReadResource(accessToken.value)
    result.value = {
      operation: 'token_lifecycle_verify_read',
      status: 'success',
      remainingSeconds: tokenLifecycleRemainingSeconds.value,
      response: data
    }
    ElMessage.success('当前 access_token 仍可访问资源')
  } catch (e) {
    result.value = {
      operation: 'token_lifecycle_verify_read',
      status: 'failed',
      remainingSeconds: tokenLifecycleRemainingSeconds.value,
      error: e.response?.data || { error: e.message },
      hint: '若是 token 过期导致失败，可点“执行 Refresh Token”后再重试。'
    }
    ElMessage.warning('资源访问失败，可能是 token 过期或权限问题')
  }
}

async function startOidcLogout() {
  try {
    const { data } = await oauth2Api.getDiscovery()
    if (!data.end_session_endpoint) {
      result.value = {
        operation: 'oidc_logout',
        message: 'discovery 文档未暴露 end_session_endpoint，当前无法演示 OIDC Logout。'
      }
      ElMessage.warning('当前服务未暴露 end_session_endpoint')
      return
    }

    const endSessionUrl = new URL(data.end_session_endpoint)
    endSessionUrl.searchParams.set('id_token_hint', idToken.value)
    endSessionUrl.searchParams.set('post_logout_redirect_uri', window.location.origin)

    sessionStorage.setItem('oauth2_oidc_logout_message', '1')
    clearTokens()
    syncTokenStateFromStorage()

    window.location.href = endSessionUrl.toString()
  } catch (e) {
    showError(e)
  }
}

async function getM2mToken() {
  try {
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      scope: m2mForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    m2mToken.value = data.access_token
    m2mLastIssuedToken.value = data.access_token
    result.value = {
      clientId: m2mForm.clientId,
      requestedScope: m2mForm.scope,
      selectedClient: m2mSelectedClient.value.label,
      ...data
    }
  } catch (e) {
    showError(e)
  }
}

async function getClientAuthMethodToken(method) {
  try {
    let data

    if (method === 'basic') {
      const basic = btoa(`${clientAuthForm.confidentialClientId}:${clientAuthForm.confidentialClientSecret}`)
      ;({ data } = await oauth2Api.clientCredentials({
        grant_type: 'client_credentials',
        scope: clientAuthForm.confidentialScope
      }, {
        Authorization: `Basic ${basic}`
      }))
    } else {
      ;({ data } = await oauth2Api.clientCredentials({
        grant_type: 'client_credentials',
        client_id: clientAuthForm.confidentialClientId,
        client_secret: clientAuthForm.confidentialClientSecret,
        scope: clientAuthForm.confidentialScope
      }))
    }

    clientAuthTokens[method] = data.access_token
    result.value = {
      operation: `client_auth_${method}`,
      authMethod: method === 'basic' ? 'client_secret_basic' : 'client_secret_post',
      clientId: clientAuthForm.confidentialClientId,
      scope: clientAuthForm.confidentialScope,
      requestPlacement: method === 'basic' ? 'Authorization Header' : 'Form Body',
      tokenPreview: maskToken(data.access_token),
      response: data
    }
    ElMessage.success(`${method === 'basic' ? 'Basic' : 'Post'} 认证成功`)
  } catch (e) {
    showError(e)
  }
}

async function startPublicClientAuthDemo() {
  result.value = {
    operation: 'client_auth_none',
    authMethod: 'none',
    clientId: clientAuthForm.publicClientId,
    message: '公开客户端不会携带 client_secret，而是通过 Authorization Code + PKCE 在新窗口发起授权，完成后会把 token 同步回当前演示页。',
    scope: clientAuthForm.publicScope
  }
  await startAuthorizationCodeFlow({
    clientId: clientAuthForm.publicClientId,
    scope: clientAuthForm.publicScope,
    usePkce: true,
    scenario: 'client-auth-none',
    openInNewWindow: true,
    syncChannel: oauth2SyncChannel
  })
}

async function tryPublicClientCredentials() {
  try {
    const { data } = await oauth2Api.clientCredentials({
      grant_type: 'client_credentials',
      client_id: clientAuthForm.publicClientId,
      scope: 'read'
    })
    result.value = {
      operation: 'public_client_credentials_unexpected_success',
      response: data
    }
  } catch (e) {
    result.value = {
      operation: 'public_client_credentials_failed_as_expected',
      authMethod: 'none',
      clientId: clientAuthForm.publicClientId,
      message: '公开客户端尝试直接使用 client_credentials 时失败，说明它不能像机密客户端那样靠 secret 向 token endpoint 认证。',
      error: e.response?.data || { error: e.message }
    }
    ElMessage.success('公开客户端错误示例已返回预期失败')
  }
}

async function introspectM2mToken() {
  try {
    const token = m2mLifecycleToken.value
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    const { data } = await oauth2Api.introspectToken({
      token,
      token_type_hint: 'access_token'
    }, {
      Authorization: `Basic ${basic}`
    })
    result.value = {
      operation: 'introspection',
      clientId: m2mForm.clientId,
      selectedClient: m2mSelectedClient.value.label,
      inspectedTokenPreview: maskToken(token),
      introspection: data
    }
  } catch (e) {
    showError(e)
  }
}

async function revokeM2mToken() {
  try {
    const token = m2mToken.value
    const basic = btoa(`${m2mForm.clientId}:${m2mForm.clientSecret}`)
    await oauth2Api.revokeToken({
      token,
      token_type_hint: 'access_token'
    }, {
      Authorization: `Basic ${basic}`
    })
    m2mLastIssuedToken.value = token
    m2mToken.value = ''
    result.value = {
      operation: 'revocation',
      clientId: m2mForm.clientId,
      selectedClient: m2mSelectedClient.value.label,
      revokedTokenPreview: maskToken(token),
      message: 'access_token 已撤销。此时再做 introspection，active 应为 false。'
    }
    ElMessage.success('M2M access_token 已撤销')
  } catch (e) {
    showError(e)
  }
}

function handleM2mClientChange(selectedKey) {
  const selectedClient = m2mClients.find(client => client.key === selectedKey)
  if (!selectedClient) return
  m2mForm.clientId = selectedClient.clientId
  m2mForm.clientSecret = selectedClient.clientSecret
  m2mForm.scope = selectedClient.scope
  m2mToken.value = ''
  result.value = {
    message: `已切换到 ${selectedClient.label}`,
    expectedBehavior: selectedClient.description,
    clientId: selectedClient.clientId,
    scope: selectedClient.scope
  }
}

async function callReadWithM2m() {
  try {
    const { data } = await oauth2Api.callReadResource(m2mToken.value)
    result.value = {
      clientId: m2mForm.clientId,
      operation: 'read',
      expected: '需要 read scope',
      actual: data
    }
  } catch (e) {
    showError(e)
  }
}

async function callWriteWithM2m() {
  try {
    const { data } = await oauth2Api.callWriteResource(m2mToken.value, {
      batchNo: 'SYNC-' + Date.now(),
      operation: 'inventory_update',
      source: 'scheduler-job'
    })
    result.value = {
      clientId: m2mForm.clientId,
      operation: 'write',
      expected: '需要 write scope',
      actual: data
    }
  } catch (e) {
    showError(e)
  }
}

async function startDeviceFlow() {
  try {
    devicePollingAborted = false
    isPollingDeviceToken.value = false
    const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
    sessionStorage.setItem('oauth2_device_return_to', '/flows?tab=device')
    const { data } = await oauth2Api.deviceAuthorize({
      client_id: deviceForm.clientId,
      scope: deviceForm.scope
    }, {
      Authorization: `Basic ${basic}`
    })
    deviceAuth.value = decorateDeviceAuth(data)
    result.value = deviceAuth.value
  } catch (e) {
    showError(e)
  }
}

async function pollDeviceToken() {
  if (!deviceAuth.value.device_code || isPollingDeviceToken.value) return

  isPollingDeviceToken.value = true
  devicePollingAborted = false
  const startedAt = getDeviceFlowStartedAt(deviceAuth.value)
  let waitMs = getDevicePollingIntervalMs(deviceAuth.value.interval)

  try {
    while (!devicePollingAborted) {
      const basic = btoa(`${deviceForm.clientId}:${deviceForm.clientSecret}`)
      try {
        const { data } = await oauth2Api.pollDeviceToken({
          grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
          device_code: deviceAuth.value.device_code,
          client_id: deviceForm.clientId
        }, {
          Authorization: `Basic ${basic}`
        })
        result.value = data
        if (data.access_token || data.refresh_token || data.id_token || data.scope || data.expires_in) {
          saveTokens(data)
          syncTokenStateFromStorage()
        }
        ElMessage.success('设备授权成功，已获取 Token')
        return
      } catch (e) {
        const errorCode = e.response?.data?.error
        if (errorCode === 'authorization_pending') {
          result.value = {
            error: errorCode,
            message: '用户还没有在 verification_uri 完成授权，系统会持续轮询直到获取结果或 2 分钟超时。',
            verificationUri: deviceAuth.value.verification_uri,
            verificationUriComplete: deviceAuth.value.verification_uri_complete,
            userCode: deviceAuth.value.user_code,
            interval: deviceAuth.value.interval,
            timeoutSeconds: DEVICE_FLOW_TIMEOUT_MS / 1000,
            expiresAt: deviceAuth.value.expires_at
          }
        } else if (errorCode === 'slow_down') {
          waitMs += 5000
          result.value = {
            error: errorCode,
            message: '服务端要求降低轮询频率，系统会自动放慢重试。',
            interval: Math.ceil(waitMs / 1000)
          }
        } else {
          showError(e)
          return
        }
      }

      if (Date.now() - startedAt >= DEVICE_FLOW_TIMEOUT_MS) {
        result.value = {
          error: 'polling_timeout',
          message: '自申请 Device Code 成功起 2 分钟内仍未获取到设备授权结果，请重新申请。',
          verificationUri: deviceAuth.value.verification_uri,
          verificationUriComplete: deviceAuth.value.verification_uri_complete,
          userCode: deviceAuth.value.user_code,
          interval: Math.ceil(waitMs / 1000),
          expiresAt: deviceAuth.value.expires_at
        }
        ElMessage.warning('设备码已超时，请重新申请 Device Code')
        return
      }

      await delay(waitMs)
    }
  } finally {
    isPollingDeviceToken.value = false
  }
}

function goClients() {
  router.push('/clients')
}

async function handleLogout() {
  await authApi.logout().catch(() => {})
  sessionStorage.removeItem('oauth2_access_token')
  sessionStorage.removeItem('oauth2_id_token')
  sessionStorage.removeItem('oauth2_refresh_token')
  sessionStorage.removeItem('oauth2_scope')
  router.push('/login')
}

function showError(e) {
  result.value = e.response?.data || { error: e.message }
  ElMessage.error(e.response?.data?.error_description || e.response?.data?.error || e.message)
}

function maskToken(token) {
  if (typeof token !== 'string' || token.length < 24) return token || ''
  return `${token.slice(0, 12)}...${token.slice(-12)}`
}

function summarizeClaim(value) {
  if (value === null || value === undefined || value === '') {
    return '暂无'
  }
  if (Array.isArray(value)) {
    return value.join(', ')
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function buildClaimsRow(claim, userAccessTokenValue, idTokenValue, machineAccessTokenValue) {
  return {
    claim,
    userAccessToken: normalizeClaimValue(userAccessTokenValue),
    idToken: normalizeClaimValue(idTokenValue),
    machineAccessToken: normalizeClaimValue(machineAccessTokenValue)
  }
}

function normalizeClaimValue(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  if (Array.isArray(value)) {
    return value.join(', ')
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function decodeJwt(token) {
  if (typeof token !== 'string' || !token) {
    return null
  }

  const segments = token.split('.')
  if (segments.length < 2) {
    return null
  }

  try {
    return {
      header: JSON.parse(base64UrlDecode(segments[0])),
      payload: JSON.parse(base64UrlDecode(segments[1]))
    }
  } catch {
    return null
  }
}

function base64UrlDecode(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return decodeURIComponent(Array.from(atob(padded))
    .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
    .join(''))
}

function buildClaimsHighlights({ userAccessToken, idToken: decodedIdTokenValue, machineAccessToken }) {
  const highlights = []

  if (decodedIdTokenValue?.payload) {
    highlights.push('id_token 面向客户端身份会话，通常携带 preferred_username、email、sid 等 OIDC 身份声明。')
  }
  if (userAccessToken?.payload) {
    highlights.push('用户 access_token 面向资源服务器授权，保留 scope 等访问控制字段；它不一定包含完整的用户资料声明。')
  }
  if (machineAccessToken?.payload) {
    highlights.push('client_credentials access_token 通常没有真实用户身份字段，更常见的是 sub 与 client_id 对应，表达“客户端自己”在访问资源。')
  }

  const userSubject = userAccessToken?.payload?.sub
  const machineSubject = machineAccessToken?.payload?.sub
  if (userSubject && machineSubject && userSubject !== machineSubject) {
    highlights.push(`当前对比中，用户 token 的 sub 是 ${userSubject}，机器 token 的 sub 是 ${machineSubject}，两者主体并不相同。`)
  }

  if (!highlights.length) {
    highlights.push('先在当前 Demo 9 中获取用户 Token 或机器 Token，再观察三类 JWT 的 claims 差异。')
  }

  return highlights
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

function writeClientAuthSummary() {
  result.value = {
    operation: 'client_auth_summary',
    confidentialClient: {
      clientId: clientAuthForm.confidentialClientId,
      basicTokenPreview: maskToken(clientAuthTokens.basic),
      postTokenPreview: maskToken(clientAuthTokens.post),
      scope: clientAuthForm.confidentialScope
    },
    publicClient: {
      clientId: clientAuthForm.publicClientId,
      loginModel: 'authorization_code + PKCE',
      currentUserAccessTokenPreview: maskToken(accessToken.value),
      scope: clientAuthForm.publicScope
    },
    comparisonRows: clientAuthComparisonRows.value,
    highlights: clientAuthHighlights.value
  }
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

function writeTokenLifecycleSummary() {
  result.value = {
    operation: 'token_lifecycle_summary',
    clientId: tokenLifecycleState.clientId,
    refreshCount: tokenLifecycleState.refreshCount,
    remainingSeconds: tokenLifecycleRemainingSeconds.value,
    refreshTokenChanged: tokenLifecycleState.previousRefreshToken
      ? refreshToken.value !== tokenLifecycleState.previousRefreshToken
      : null,
    previousRefreshTokenPreview: maskToken(tokenLifecycleState.previousRefreshToken),
    currentRefreshTokenPreview: maskToken(refreshToken.value),
    hints: [
      '默认策略客户端通常复用 refresh_token。',
      '轮换策略客户端刷新后应返回新的 refresh_token。',
      'access_token 过期后应使用 refresh_token 续期，而不是强制用户重新登录。'
    ]
  }
}

function writeDynamicClientSummary() {
  result.value = {
    operation: 'dynamic_client_summary',
    registeredClientId: dynamicClientState.id || null,
    dynamicClientId: dynamicClientState.clientId || dynamicClientForm.clientId,
    requireProofKey: true,
    requestedScope: dynamicClientForm.scope,
    nextStep: dynamicClientState.id
      ? '可使用该 client_id 再次发起 Authorization Code + PKCE。'
      : '先执行动态注册，再进行授权演示。'
  }
}

function writeParSummary() {
  result.value = {
    operation: 'par_summary',
    clientId: parForm.clientId,
    requestUri: parState.requestUri || null,
    requestUriExpiresIn: parState.expiresIn || null,
    keyPoints: [
      'PAR 先在后端通道提交完整授权参数，返回 request_uri。',
      '浏览器跳转授权端点时可不再携带完整 scope / redirect_uri / code_challenge。',
      '该模式更适合高安全、合规要求高的企业场景。'
    ]
  }
}

function persistAccessTokenExpiryMetadata(expiresInSeconds) {
  if (!expiresInSeconds) {
    return
  }
  const issuedAt = Date.now()
  sessionStorage.setItem('oauth2_access_token_issued_at', String(issuedAt))
  sessionStorage.setItem('oauth2_access_token_expires_at', String(issuedAt + Number(expiresInSeconds) * 1000))
  sessionStorage.setItem('oauth2_access_token_expires_in', String(expiresInSeconds))
}

function decorateDeviceAuth(data) {
  const startedAt = Date.now()
  const expiresAt = startedAt + DEVICE_FLOW_TIMEOUT_MS
  const verificationUri = appendDeviceTimingParams(data.verification_uri, startedAt, expiresAt)
  const verificationUriComplete = appendDeviceTimingParams(data.verification_uri_complete, startedAt, expiresAt)

  persistDeviceFlowTiming(data.user_code, startedAt, expiresAt)

  return {
    ...data,
    started_at: startedAt,
    expires_at: expiresAt,
    verification_uri: verificationUri,
    verification_uri_complete: verificationUriComplete
  }
}

function appendDeviceTimingParams(url, startedAt, expiresAt) {
  if (typeof url !== 'string' || !url) return url
  const parsedUrl = new URL(url)
  parsedUrl.searchParams.set('started_at', String(startedAt))
  parsedUrl.searchParams.set('expires_at', String(expiresAt))
  return parsedUrl.toString()
}

function persistDeviceFlowTiming(userCode, startedAt, expiresAt) {
  if (typeof userCode !== 'string' || !userCode) return
  localStorage.setItem(getDeviceFlowStorageKey(userCode), JSON.stringify({ startedAt, expiresAt }))
}

function getDeviceFlowStartedAt(deviceAuthValue) {
  const startedAt = Number(deviceAuthValue?.started_at)
  if (Number.isFinite(startedAt) && startedAt > 0) {
    return startedAt
  }

  const persistedTiming = getPersistedDeviceFlowTiming(deviceAuthValue?.user_code)
  if (persistedTiming?.startedAt) {
    return persistedTiming.startedAt
  }

  return Date.now()
}

function getPersistedDeviceFlowTiming(userCode) {
  if (typeof userCode !== 'string' || !userCode) return null
  const rawValue = localStorage.getItem(getDeviceFlowStorageKey(userCode))
  if (!rawValue) return null

  try {
    const parsed = JSON.parse(rawValue)
    if (Number.isFinite(parsed?.startedAt) && Number.isFinite(parsed?.expiresAt)) {
      return parsed
    }
  } catch {
    localStorage.removeItem(getDeviceFlowStorageKey(userCode))
  }

  return null
}

function getDeviceFlowStorageKey(userCode) {
  return `oauth2_device_flow_${userCode}`
}

function getDevicePollingIntervalMs(intervalSeconds) {
  const parsedInterval = Number(intervalSeconds)
  const safeInterval = Number.isFinite(parsedInterval) && parsedInterval > 0
    ? parsedInterval
    : DEFAULT_DEVICE_INTERVAL_SECONDS
  return safeInterval * 1000
}
</script>

<style scoped>
.layout {
  min-height: 98.5vh;
  background: #f5f7fa;
}
.content-shell {
  min-height: calc(100vh - 140px);
  gap: 16px;
}
.side-nav {
  width: 320px;
  max-width: 100%;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  overflow: hidden;
}
.side-nav-title {
  padding: 18px 20px 12px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}
.side-menu {
  border-right: none;
  overflow-x: hidden;
  scrollbar-width: none;
}
.side-menu::-webkit-scrollbar {
  display: none;
}
.content-main {
  padding: 0;
}
.content-tabs :deep(.el-tabs__header) {
  display: none;
}
.content-tabs :deep(.el-tabs__content) {
  padding: 0;
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
.header-right {
  display: flex;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}
.mb16 {
  margin-bottom: 16px;
}
.mt16 {
  margin-top: 16px;
}
.actions-row {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.token-box,
.result-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: monospace;
}
.result-box {
  margin: 0;
}
.scenes-pane {
  padding: 60px 0;
}
@media (max-width: 960px) {
  .content-shell {
    display: block;
    min-height: auto;
  }
  .side-nav {
    width: 100%;
    margin-bottom: 16px;
  }
}
</style>
