package com.demo.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${app.allowed-origins}")
  private String allowedOrigins;

  @Value("${app.base-url}")
  private String appBaseUrl;

  private static final String DEVICE_VERIFICATION_PAGE = "/activate";

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, JdbcUserDetailsManager userDetailsManager) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();
    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, server -> server
            .authorizationEndpoint(authorization -> authorization
                .consentPage("/oauth2/consent"))
            .pushedAuthorizationRequestEndpoint(par -> {
              // Keep default PAR processing and expose explicit hook for future validation customization.
            })
            .deviceAuthorizationEndpoint(deviceAuthorization ->
                deviceAuthorization.verificationUri(trimTrailingSlash(appBaseUrl) + DEVICE_VERIFICATION_PAGE))
            .deviceVerificationEndpoint(deviceVerification -> deviceVerification
                .consentPage(DEVICE_VERIFICATION_PAGE))
            .oidc(oidc -> oidc.userInfoEndpoint(userInfo -> userInfo.userInfoMapper(context -> {
              String username = context.getAuthorization().getPrincipalName();
              UserDetails user = userDetailsManager.loadUserByUsername(username);
              Set<String> scopes = context.getAuthorization().getAuthorizedScopes();

              Map<String, Object> claims = new HashMap<>();
              if (scopes.contains(OidcScopes.PROFILE)) {
                claims.put("preferred_username", username);
                claims.put("name", "Demo User " + username);
                claims.put("locale", "zh-CN");
                claims.put("zoneinfo", "Asia/Shanghai");
                if (user.getAuthorities() != null) {
                  claims.put("authorities", user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
                }
              }
              claims.put("updated_at", java.time.Instant.now().getEpochSecond());
              return new OidcUserInfo(claims);
            })))
        )
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        )
        .cors(cors -> cors.configurationSource(corsConfigurationSource()));

    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/auth/login", "/api/auth/status", "/api/auth/backchannel-logout").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll()
        )
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll()
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins.split(","))
        .stream()
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
    return new JdbcUserDetailsManager(dataSource);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  private String trimTrailingSlash(String value) {
    return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
