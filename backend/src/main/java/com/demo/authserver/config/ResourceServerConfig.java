package com.demo.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ResourceServerConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http,
                                                                  JwtDecoder jwtDecoder) throws Exception {
        http
                .securityMatcher("/resource/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/resource/public").permitAll()
                        .requestMatchers("/resource/profile").hasAuthority("SCOPE_profile")
                        .requestMatchers("/resource/read").hasAuthority("SCOPE_read")
                        .requestMatchers("/resource/write").hasAuthority("SCOPE_write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"未携带有效的 Bearer Token\"}");
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Token 中缺少所需 Scope 权限\"}");
                        })
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
