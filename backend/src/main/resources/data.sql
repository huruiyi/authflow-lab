-- ============================================================
-- 示例 OAuth2 Client 数据（涵盖各种授权模式与认证方式）
-- 密码格式：{noop}明文，仅供演示，生产环境请使用 {bcrypt}
-- ============================================================

-- 复用 JSON 变量
SET @cs_default        = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_consent        = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_pkce           = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_pkce_consent   = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":true,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_pkce_par       = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.require-pushed-authorization-requests":true,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';

-- token 有效期：AT=5min  RT=1h  Code=5min  Device=5min
SET @ts_default  = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';

-- token 有效期：AT=1h  RT=7d  不复用 RefreshToken
SET @ts_long_ttl = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';

-- token 有效期：AT=30s  RT=10min  不复用 RefreshToken（用于前端演示短过期 + 轮换）
SET @ts_short_rotation = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",30.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';

-- token 有效期：AT=5min  RT=1h  不透明 Token 格式（用于演示与自包含 JWT Token 的区别）
SET @ts_opaque = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';

-- client settings: 配置 JWT 客户端认证（client_secret_jwt）
SET @cs_jwt_auth = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":["org.springframework.security.oauth2.jose.jws.MacAlgorithm","HS256"]}';

-- client settings: 配置 Back-channel Logout
SET @cs_backchannel_logout = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null,"settings.client.settings.token.endpoint.auth.signing.algorithm":null}';


-- ============================================================
-- 1. 传统 Web 应用
--    认证：client_secret_basic
--    授权：authorization_code + refresh_token
--    作用：模拟有后端会话的传统服务端渲染应用，演示标准授权码换 token
--    使用场景：Spring MVC / Thymeleaf / JSP 等服务端应用接入统一登录
--    特点：保留传统 Web Client 语义；当前仓库演示统一回跳到前端 /callback
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'webapp-client',
    NOW(),
    '{noop}webapp-secret',
    '传统 Web 应用',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email',
    @cs_consent,
    @ts_default
);


-- ============================================================
-- 2. 单页应用（SPA）/ 公开客户端
--    认证：none（无 client_secret）
--    授权：authorization_code
--    作用：演示公开客户端在浏览器环境下如何安全完成授权码流程
--    使用场景：Vue / React / Angular 等前后端分离单页应用
--    特点：强制 PKCE，无密钥，适合 Vue / React / Angular
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-public-client',
    NOW(),
    NULL,
    '单页应用（SPA）',
    'none',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_default
);


-- ============================================================
-- 3. 后端服务 / Machine-to-Machine（M2M）
--    认证：client_secret_basic
--    授权：client_credentials
--    作用：演示“无用户上下文”的服务账号获取访问令牌
--    使用场景：网关调内部 API、定时任务调用资源服务、微服务间鉴权
--    特点：无用户参与，服务间调用，无 redirect_uri / openid
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'm2m-service-client',
    NOW(),
    '{noop}m2m-secret',
    '后端服务（M2M）',
    'client_secret_basic',
    'client_credentials',
    NULL,
    NULL,
    'read,write',
    @cs_default,
    @ts_default
);


-- ============================================================
-- 3.1 单页应用（SPA）+ 授权确认页
--    认证：none（无 client_secret）
--    授权：authorization_code + refresh_token
--    作用：演示用户可见的授权确认页与 scope 逐项授权
--    使用场景：高敏感权限申请（如写权限、管理权限）需显式用户确认
--    特点：强制 PKCE + 强制 consent，用于演示 scope 缩减授权
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-consent-client',
    NOW(),
    NULL,
    '单页应用（SPA + Consent）',
    'none',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce_consent,
    @ts_default
);


-- ============================================================
-- 3.2 单页应用（SPA + 短过期 + Refresh Token 轮换）
--    认证：none（无 client_secret）
--    授权：authorization_code + refresh_token
--    作用：演示短时访问令牌与刷新令牌轮换带来的风险收敛策略
--    使用场景：安全要求较高的前端系统（金融、管理后台、运营系统）
--    特点：强制 PKCE + AT 短有效期 + Refresh Token 每次刷新后轮换
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-rotation-client',
    NOW(),
    NULL,
    '单页应用（短过期 + Refresh 轮换）',
    'none',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_short_rotation
);


-- ============================================================
-- 3.2.1 Token 生命周期演示（默认策略，机密客户端）
--    认证：client_secret_post
--    授权：authorization_code + refresh_token
--    作用：为 Demo5 提供稳定可刷新的默认策略客户端（refresh_token 复用）
--    使用场景：教学演示 refresh_token 刷新成功路径、与轮换策略做对比
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'lifecycle-default-client',
    NOW(),
    '{noop}lifecycle-default-secret',
    'Token 生命周期（默认策略）',
    'client_secret_post',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_default
);


-- ============================================================
-- 3.2.2 Token 生命周期演示（轮换策略，机密客户端）
--    认证：client_secret_post
--    授权：authorization_code + refresh_token
--    作用：为 Demo5 提供稳定可刷新的轮换策略客户端（refresh_token 刷新后换新）
--    使用场景：教学演示 refresh_token 轮换与重放风险收敛
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'lifecycle-rotation-client',
    NOW(),
    '{noop}lifecycle-rotation-secret',
    'Token 生命周期（轮换策略）',
    'client_secret_post',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_short_rotation
);


-- ============================================================
-- 3.3 单页应用（SPA + PAR）
--    认证：none + client_secret_post（由后端代发 PAR，SPA 不直接持有密钥）
--    授权：authorization_code + refresh_token
--    作用：演示通过 PAR 将授权参数后置到服务端，减少前端 URL 暴露风险
--    使用场景：参数复杂或合规要求高的授权请求（如企业级 SSO）
--    特点：要求先提交 /oauth2/par，再使用 request_uri 发起授权
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-par-client',
    NOW(),
    '{noop}par-secret',
    '单页应用（PAR）',
    'none,client_secret_post',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce_par,
    @ts_default
);


-- ============================================================
-- 4. 移动端应用（iOS / Android）
--    认证：none（公开客户端）
--    授权：authorization_code + refresh_token
--    作用：模拟原生 App 的 OAuth 登录与 token 刷新
--    使用场景：iOS / Android 客户端通过自定义 scheme 或 universal link 回调
--    特点：PKCE 必须；同时保留原生 App deep link 和当前仓库前端演示回调
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'mobile-app-client',
    NOW(),
    NULL,
    '移动端应用',
    'none',
    'authorization_code,refresh_token',
    'com.example.app://oauth2/callback,http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_long_ttl
);


-- ============================================================
-- 5. POST 方式认证的 Web 应用
--    认证：client_secret_post（密钥放在请求 body 而非 Header）
--    授权：authorization_code + refresh_token
--    作用：对比不同客户端认证方式在 token 端点的行为差异
--    使用场景：遗留系统或受限环境无法方便设置 Authorization Header
--    特点：演示 client_secret_post；当前仓库使用 30000 端口回调与登出回跳
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'post-auth-client',
    NOW(),
    '{noop}post-secret',
    'POST 认证 Web 应用',
    'client_secret_post',
    'authorization_code,refresh_token',
    'http://authlab.test:30000/callback',
    'http://authlab.test:30000',
    'openid,profile',
    @cs_consent,
    @ts_default
);


-- ============================================================
-- 6. IoT 设备 / 智能电视（设备码流程）
--    认证：client_secret_basic
--    授权：urn:ietf:params:oauth:grant-type:device_code + refresh_token
--    作用：演示输入受限设备如何通过“设备码 + 二次设备确认”完成授权
--    使用场景：电视、机顶盒、打印机、车机等无完整浏览器或输入能力弱的终端
--    特点：无浏览器环境，用户在手机上完成授权；当前演示默认申请 profile + read
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'device-flow-client',
    NOW(),
    '{noop}device-secret',
    'IoT 设备（设备码流程）',
    'client_secret_basic',
    'urn:ietf:params:oauth:grant-type:device_code,refresh_token',
    NULL,
    NULL,
    'profile,read',
    @cs_default,
    @ts_long_ttl
);


-- ============================================================
-- 7. 综合测试客户端
--    认证：client_secret_basic + client_secret_post（双方式）
--    授权：全部授权类型
--    作用：作为一站式调试客户端，覆盖常见认证方式与授权模式组合
--    使用场景：本地联调、接口回归测试、Postman/curl 脚本一次性验证
--    特点：仅用于开发联调，覆盖当前仓库前后端两组回调地址
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'all-in-one-client',
    NOW(),
    '{noop}all-secret',
    '综合测试客户端',
    'client_secret_basic,client_secret_post',
    'authorization_code,refresh_token,client_credentials,urn:ietf:params:oauth:grant-type:device_code',
    'http://authlab.test:30000/callback,http://authlab.test:5173/callback',
    'http://authlab.test:30000,http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce_consent,
    @ts_default
);


-- ============================================================
-- 8. 不透明 Token 演示客户端
--    认证：none（公开客户端）
--    授权：authorization_code + refresh_token
--    作用：演示 reference token 需要依赖 introspection 才能识别声明信息
--    使用场景：希望在授权服务器侧可即时失效、集中管控 token 状态的系统
--    特点：使用不透明 Token 格式，与自包含 JWT Token 进行对比演示
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-opaque-client',
    NOW(),
    NULL,
    '单页应用（不透明 Token）',
    'none',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_opaque
);


-- ============================================================
-- 9. JWT 客户端认证演示
--    认证：client_secret_jwt（使用 JWT 进行客户端认证）
--    授权：authorization_code + client_credentials
--    作用：演示客户端通过 JWT 断言进行 token 端点认证的标准做法
--    使用场景：B2B 服务对接、需要更强审计与签名认证能力的服务调用
--    特点：客户端使用签名的 JWT 断言（client_assertion）代替直接传递 client_secret
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'jwt-auth-client',
    NOW(),
    'jwt-secret-demo-key-0123456789abcdef',
    'JWT 客户端认证演示',
    'client_secret_jwt',
    'authorization_code,refresh_token,client_credentials',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_jwt_auth,
    @ts_default
);


-- ============================================================
-- 10. Back-channel Logout 演示
--    认证：none（公开客户端）
--    授权：authorization_code + refresh_token
--    作用：演示 OIDC Back-channel Logout 在无前端参与情况下的会话退出通知
--    使用场景：多系统统一登录后，要求跨系统同步登出、降低残留会话风险
--    特点：强制 PKCE；授权服务器通过后端通道直接向客户端发送退出请求
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris,
 scopes,
 client_settings, token_settings)
VALUES (
    UUID(),
    'spa-backchannel-logout-client',
    NOW(),
    NULL,
    'SPA（Back-channel Logout）',
    'none',
    'authorization_code,refresh_token',
    'http://authlab.test:5173/callback',
    'http://authlab.test:5173',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_default
);
