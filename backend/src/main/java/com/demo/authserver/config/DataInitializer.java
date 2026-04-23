package com.demo.authserver.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import java.time.Duration;
import java.time.Instant;

@Configuration
public class DataInitializer implements ApplicationRunner {

  private static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
  private static final String JWT_AUTH_CLIENT_SECRET = "jwt-secret-demo-key-0123456789abcdef";
  private static final String URL_FRONTEND_BASE = "http://authlab.test:5173";
  private static final String URL_FRONTEND_CALLBACK = URL_FRONTEND_BASE + "/callback";
  private static final String URL_BACKEND_BASE = "http://authlab.test:30000";
  private static final String URL_BACKEND_CALLBACK = URL_BACKEND_BASE + "/callback";

  private final JdbcUserDetailsManager userDetailsManager;
  private final PasswordEncoder passwordEncoder;
  private final RegisteredClientRepository registeredClientRepository;

  public DataInitializer(
      JdbcUserDetailsManager userDetailsManager,
      PasswordEncoder passwordEncoder,
      RegisteredClientRepository registeredClientRepository) {
    this.userDetailsManager = userDetailsManager;
    this.passwordEncoder = passwordEncoder;
    this.registeredClientRepository = registeredClientRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureAdminUser();
    ensureRepositoryRegisteredClients();
  }

  private void ensureAdminUser() {
    if (!userDetailsManager.userExists("admin")) {
      UserDetails admin = User.builder()
          .username("admin")
          .password(passwordEncoder.encode("admin123"))
          .roles("ADMIN")
          .build();
      userDetailsManager.createUser(admin);
    }
  }

  /**
   *spa-par-client 客户端以data.sql中的为主，DataInitializer中不做兜底新增
   */
  private void ensureRepositoryRegisteredClients() {
    ensureWebappClientExists();
    ensureSpaPublicClientExists();
    ensureM2mClientExists();
    ensureSpaConsentClientExists();
    ensureSpaRotationClientExists();
    ensureMobileClientExists();
    ensurePostAuthClientExists();
    ensureDeviceFlowClientExists();
    ensureAllInOneClientExists();
    ensureBackchannelLogoutClientExists();
    ensureOpaqueClientExists();
    ensureJwtAuthClientExists();
  }

  private void ensureWebappClientExists() {
    if (registeredClientRepository.findByClientId("webapp-client") == null) {
      registeredClientRepository.save(webappClient());
    }
  }

  private void ensureSpaPublicClientExists() {
    if (registeredClientRepository.findByClientId("spa-public-client") == null) {
      registeredClientRepository.save(spaPublicClient());
    }
  }

  private void ensureM2mClientExists() {
    if (registeredClientRepository.findByClientId("m2m-service-client") == null) {
      registeredClientRepository.save(m2mClient());
    }
  }

  private void ensureSpaConsentClientExists() {
    if (registeredClientRepository.findByClientId("spa-consent-client") == null) {
      registeredClientRepository.save(spaConsentClient());
    }
  }

  private void ensureSpaRotationClientExists() {
    if (registeredClientRepository.findByClientId("spa-rotation-client") == null) {
      registeredClientRepository.save(spaRotationClient());
    }
  }

  private void ensureMobileClientExists() {
    if (registeredClientRepository.findByClientId("mobile-app-client") == null) {
      registeredClientRepository.save(mobileClient());
    }
  }

  private void ensurePostAuthClientExists() {
    if (registeredClientRepository.findByClientId("post-auth-client") == null) {
      registeredClientRepository.save(postAuthClient());
    }
  }

  private void ensureDeviceFlowClientExists() {
    if (registeredClientRepository.findByClientId("device-flow-client") == null) {
      registeredClientRepository.save(deviceFlowClient());
    }
  }

  private void ensureAllInOneClientExists() {
    if (registeredClientRepository.findByClientId("all-in-one-client") == null) {
      registeredClientRepository.save(allInOneClient());
    }
  }

  private void ensureBackchannelLogoutClientExists() {
    if (registeredClientRepository.findByClientId("spa-backchannel-logout-client") == null) {
      registeredClientRepository.save(backchannelLogoutClient());
    }
  }

  private void ensureOpaqueClientExists() {
    if (registeredClientRepository.findByClientId("spa-opaque-client") == null) {
      registeredClientRepository.save(opaqueClient());
    }
  }

  private void ensureJwtAuthClientExists() {
    if (registeredClientRepository.findByClientId("jwt-auth-client") == null) {
      registeredClientRepository.save(jwtAuthClient());
    }
  }

  private RegisteredClient webappClient() {
    return RegisteredClient.withId("client-webapp-001")
        .clientId("webapp-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("传统 Web 应用")
        .clientSecret("{noop}webapp-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(true)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient spaPublicClient() {
    return RegisteredClient.withId("client-spa-002")
        .clientId("spa-public-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("单页应用（SPA）")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient m2mClient() {
    return RegisteredClient.withId("client-m2m-003")
        .clientId("m2m-service-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("后端服务（M2M）")
        .clientSecret("{noop}m2m-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient spaConsentClient() {
    return RegisteredClient.withId("client-spa-consent-0031")
        .clientId("spa-consent-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("单页应用（SPA + Consent）")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(true)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient spaRotationClient() {
    return RegisteredClient.withId("client-spa-rotation-0032")
        .clientId("spa-rotation-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("单页应用（短过期 + Refresh 轮换）")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(false)
            .accessTokenTimeToLive(Duration.ofSeconds(30))
            .refreshTokenTimeToLive(Duration.ofMinutes(10))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient mobileClient() {
    return RegisteredClient.withId("client-mobile-004")
        .clientId("mobile-app-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("移动端应用")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("com.example.app://oauth2/callback")
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(false)
            .accessTokenTimeToLive(Duration.ofHours(1))
            .refreshTokenTimeToLive(Duration.ofDays(7))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient postAuthClient() {
    return RegisteredClient.withId("client-post-005")
        .clientId("post-auth-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("POST 认证 Web 应用")
        .clientSecret("{noop}post-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_BACKEND_CALLBACK)
        .postLogoutRedirectUri(URL_BACKEND_BASE)
        .scope("openid")
        .scope("profile")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(true)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient deviceFlowClient() {
    return RegisteredClient.withId("client-device-006")
        .clientId("device-flow-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("IoT 设备（设备码流程）")
        .clientSecret("{noop}device-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(new AuthorizationGrantType(DEVICE_CODE_GRANT))
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .scope("profile")
        .scope("read")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(false)
            .accessTokenTimeToLive(Duration.ofHours(1))
            .refreshTokenTimeToLive(Duration.ofDays(7))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient allInOneClient() {
    return RegisteredClient.withId("client-all-007")
        .clientId("all-in-one-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("综合测试客户端")
        .clientSecret("{noop}all-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .authorizationGrantType(new AuthorizationGrantType(DEVICE_CODE_GRANT))
        .redirectUri(URL_BACKEND_CALLBACK)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_BACKEND_BASE)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(true)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient opaqueClient() {
    return RegisteredClient.withId("client-opaque-008")
        .clientId("spa-opaque-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("单页应用（不透明 Token）")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient backchannelLogoutClient() {
    return RegisteredClient.withId("client-backchannel-logout-010")
        .clientId("spa-backchannel-logout-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("单页应用（Back-channel Logout 演示）")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

  private RegisteredClient jwtAuthClient() {
    return RegisteredClient.withId("client-jwt-auth-009")
        .clientId("jwt-auth-client")
        .clientIdIssuedAt(Instant.now())
        .clientName("JWT 客户端认证演示")
        .clientSecret(JWT_AUTH_CLIENT_SECRET)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .redirectUri(URL_FRONTEND_CALLBACK)
        .postLogoutRedirectUri(URL_FRONTEND_BASE)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(false)
            .tokenEndpointAuthenticationSigningAlgorithm(MacAlgorithm.HS256)
            .build())
        .tokenSettings(TokenSettings.builder()
            .reuseRefreshTokens(true)
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(1))
            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
            .deviceCodeTimeToLive(Duration.ofMinutes(5))
            .build())
        .build();
  }

}
