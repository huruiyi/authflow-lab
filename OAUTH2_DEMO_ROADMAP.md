# OAuth2 Demo 实现现状与路线图

## 1. 已实现能力

### 核心授权流程

- [x] Authorization Code + PKCE
- [x] Scope 缩减授权 Demo
- [x] Client Credentials
- [x] Device Code
- [x] Refresh Token
- [x] OIDC UserInfo

### 配套能力

- [x] 资源接口按 scope 鉴权（read / write / profile）
- [x] Client 管理界面
- [x] Device Code 验证页自定义跳转
- [x] verification_uri 与 verification_uri_complete 局域网可访问
- [x] Device Code 页面倒计时
- [x] Device Code 通过 verification_uri 手动输入 user_code
- [x] Device Code 输入 user_code 后识别 client_id / scope

### Token 生命周期能力

- [x] Token Introspection Demo（基于 Client Credentials token）
- [x] Token Revocation Demo（基于 Client Credentials token）
- [x] OIDC Logout / RP-Initiated Logout Demo

### Claims / 身份差异能力

- [x] JWT Claims 差异演示（用户 access_token / 用户 id_token / 机器 access_token）

### Client 身份认证能力

- [x] Client 认证方式差异 Demo（client_secret_basic / client_secret_post / none）

### 安全对比能力

- [x] Authorization Code without PKCE 对比 Demo

## 2. 当前演示覆盖说明

项目目前已经覆盖以下典型 OAuth2 / OIDC 学习路径：

1. 前后端分离登录：Authorization Code + PKCE
2. 授权范围缩减：客户端申请 read write，用户只同意部分 scope
3. 服务到服务访问：Client Credentials
4. 设备端授权：Device Code
5. Token 续期：Refresh Token
6. 用户声明读取：UserInfo
7. 资源访问鉴权：按 scope 访问不同资源
8. Token 生命周期管理：Introspection / Revocation
9. OIDC 会话退出：RP-Initiated Logout
10. JWT Claims 差异对比：用户 token vs 机器 token vs id_token
11. Client 认证方式对比：client_secret_basic vs client_secret_post vs none
12. Authorization Code 安全对比：with PKCE vs without PKCE
13. Access Token 过期与 Refresh Token 轮换策略对比
14. Pushed Authorization Request（PAR）

## 3. OIDC Logout 与正常退出的区别

### 正常退出

项目里的“正常退出”指前端调用本地业务接口 `/api/auth/logout`，作用范围主要是当前前端应用自己的登录态。

典型行为：

1. 清理当前浏览器里的本地 session / 安全上下文
2. 清理前端缓存的 access_token / refresh_token / id_token
3. 返回登录页

局限性：

1. 它不一定会结束授权服务器上的 OIDC 会话
2. 如果浏览器里授权服务器登录态还在，下次再次发起授权时，可能无需重新输入用户名密码

### OIDC Logout

项目里的 OIDC Logout 指 RP-Initiated Logout，也就是客户端引导浏览器访问授权服务器的 `end_session_endpoint`，由授权服务器结束自己的登录会话。

典型行为：

1. 客户端携带 `id_token_hint` 跳转到授权服务器的退出端点
2. 授权服务器校验会话并执行退出
3. 授权服务器再跳回已注册的 `post_logout_redirect_uri`

价值：

1. 结束的不只是前端页面状态，还包括授权服务器侧的登录会话
2. 更符合 OIDC 单点登录 / 单点退出的语义
3. 更适合演示“同一个身份提供者下多个客户端共享登录态”的场景

### 这个项目里如何理解

可以简单理解为：

1. 正常退出：退出当前前端应用
2. OIDC Logout：退出授权中心会话

如果只做正常退出，用户再次点“发起授权登录”时，往往还会因为授权服务器 session 仍然存在而直接通过。
如果做 OIDC Logout，再次发起授权登录时，更符合“需要重新建立授权中心登录态”的预期。

## 4. 待实现 Demo 路线

建议按下面顺序继续补齐：

1. [x] Access Token 过期与 Refresh Token 轮换策略演示
2. [x] Dynamic Client Registration
3. [x] Pushed Authorization Request（PAR）

## 5. 每项待实现的目的

### 5.1 JWT Claims 差异演示

目标：对比不同 grant type 下 token claims 的差异，帮助理解 subject、authorities、scope、OIDC claims 的来源。

### 5.2 Client 认证方式差异 Demo

目标：帮助理解公开客户端与机密客户端的差异，以及 token endpoint 如何做客户端认证。

### 5.3 Authorization Code without PKCE

目标：作为对比教学，用来说明为什么现代 SPA / 移动端必须使用 PKCE。

### 5.4 Token 过期与轮换策略演示

目标：展示 access token 过期、refresh token 刷新、refresh token 轮换后的行为差异。

### 5.5 Dynamic Client Registration

目标：演示客户端动态注册，而不是后台手工预置。

### 5.6 PAR

目标：演示更高安全性的授权请求提交方式，适合企业级和合规场景。

## 6. 实施策略

后续建议遵循下面策略继续扩展：

1. 先补协议生命周期能力，再补高级协议能力。
2. 优先复用现有 FlowsDemo 页面，避免过早拆分过多页面。
3. 每补完一个 Demo，都同步补充文档、前端按钮入口和成功 / 失败结果展示。