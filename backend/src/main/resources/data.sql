-- ============================================================
-- 示例 OAuth2 Client 数据（涵盖各种授权模式与认证方式）
-- 密码格式：{noop}明文，仅供演示，生产环境请使用 {bcrypt}
-- ============================================================

-- 复用 JSON 变量
SET @cs_default        = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_consent        = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_pkce           = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';
SET @cs_pkce_consent   = '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":true,"settings.client.jwk-set-url":null,"settings.client.token-endpoint-authentication-signing-algorithm":null}';

-- token 有效期：AT=5min  RT=1h  Code=5min  Device=5min
SET @ts_default  = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';

-- token 有效期：AT=1h  RT=7d  不复用 RefreshToken
SET @ts_long_ttl = '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.x509-certificate-bound-access-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}';


-- ============================================================
-- 1. 传统 Web 应用
--    认证：client_secret_basic
--    授权：authorization_code + refresh_token
--    特点：需授权确认页，适合服务端渲染应用（Spring MVC / Thymeleaf）
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-webapp-001',
    'webapp-client',
    NOW(),
    '{noop}webapp-secret',
    '传统 Web 应用',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://localhost:8080/login/oauth2/code/webapp',
    'http://localhost:8080',
    'openid,profile,email',
    @cs_consent,
    @ts_default
);


-- ============================================================
-- 2. 单页应用（SPA）/ 公开客户端
--    认证：none（无 client_secret）
--    授权：authorization_code
--    特点：强制 PKCE，无密钥，适合 Vue / React / Angular
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-spa-002',
    'spa-public-client',
    NOW(),
    NULL,
    '单页应用（SPA）',
    'none',
    'authorization_code',
    'http://localhost:5173/callback',
    'http://localhost:5173',
    'openid,profile,email,read',
    @cs_pkce,
    @ts_default
);


-- ============================================================
-- 3. 后端服务 / Machine-to-Machine（M2M）
--    认证：client_secret_basic
--    授权：client_credentials
--    特点：无用户参与，服务间调用，无 redirect_uri / openid
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-m2m-003',
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
-- 4. 移动端应用（iOS / Android）
--    认证：none（公开客户端）
--    授权：authorization_code + refresh_token
--    特点：PKCE 必须，支持自定义 scheme 深链，长 Token 有效期
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-mobile-004',
    'mobile-app-client',
    NOW(),
    NULL,
    '移动端应用',
    'none',
    'authorization_code,refresh_token',
    'com.example.app://oauth2/callback,http://localhost:8100/callback',
    'com.example.app://logout',
    'openid,profile,email,read,write',
    @cs_pkce,
    @ts_long_ttl
);


-- ============================================================
-- 5. POST 方式认证的 Web 应用
--    认证：client_secret_post（密钥放在请求 body 而非 Header）
--    授权：authorization_code + refresh_token
--    特点：适合不支持 Basic Auth Header 的老旧框架
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-post-005',
    'post-auth-client',
    NOW(),
    '{noop}post-secret',
    'POST 认证 Web 应用',
    'client_secret_post',
    'authorization_code,refresh_token',
    'http://localhost:9090/callback',
    'http://localhost:9090',
    'openid,profile',
    @cs_consent,
    @ts_default
);


-- ============================================================
-- 6. IoT 设备 / 智能电视（设备码流程）
--    认证：client_secret_basic
--    授权：urn:ietf:params:oauth:grant-type:device_code + refresh_token
--    特点：无浏览器环境，用户在手机上完成授权
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-device-006',
    'device-flow-client',
    NOW(),
    '{noop}device-secret',
    'IoT 设备（设备码流程）',
    'client_secret_basic',
    'urn:ietf:params:oauth:grant-type:device_code,refresh_token',
    NULL,
    NULL,
    'openid,profile,read',
    @cs_default,
    @ts_long_ttl
);


-- ============================================================
-- 7. 综合测试客户端
--    认证：client_secret_basic + client_secret_post（双方式）
--    授权：全部授权类型
--    特点：仅用于开发联调，覆盖所有流程
-- ============================================================
INSERT IGNORE INTO oauth2_registered_client
(id, client_id, client_id_issued_at, client_secret, client_name,
 client_authentication_methods, authorization_grant_types,
 redirect_uris, post_logout_redirect_uris, scopes,
 client_settings, token_settings)
VALUES (
    'client-all-007',
    'all-in-one-client',
    NOW(),
    '{noop}all-secret',
    '综合测试客户端',
    'client_secret_basic,client_secret_post',
    'authorization_code,refresh_token,client_credentials,urn:ietf:params:oauth:grant-type:device_code',
    'http://localhost:8080/callback,http://localhost:5173/callback,http://127.0.0.1/callback',
    'http://localhost:8080,http://localhost:5173',
    'openid,profile,email,read,write',
    @cs_pkce_consent,
    @ts_default
);
