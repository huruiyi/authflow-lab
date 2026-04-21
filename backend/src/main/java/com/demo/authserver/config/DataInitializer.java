package com.demo.authserver.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer implements ApplicationRunner {

    private final JdbcUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${app.par-demo.client-secret}")
    private String parClientSecret;

    public DataInitializer(JdbcUserDetailsManager userDetailsManager,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate,
                           ObjectMapper objectMapper) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userDetailsManager.userExists("admin")) {
            UserDetails admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles("ADMIN")
                    .build();
            userDetailsManager.createUser(admin);
        }

        syncClientRedirectUris("spa-public-client", frontendRedirectUris(), frontendBaseUrl);
        syncClientRedirectUris("spa-consent-client", frontendRedirectUris(), frontendBaseUrl);
        syncClientRedirectUris("spa-rotation-client", frontendRedirectUris(), frontendBaseUrl);
        syncClientRedirectUris("spa-par-client", frontendRedirectUris(), frontendBaseUrl);
        syncClientRedirectUris("all-in-one-client", frontendRedirectUris(), frontendPostLogoutRedirectUris());
        enforceParClientSettings("spa-par-client");
    }

    private void syncClientRedirectUris(String clientId, String redirectUris, String postLogoutRedirectUris) {
        jdbcTemplate.update(
                "UPDATE oauth2_registered_client SET redirect_uris = ?, post_logout_redirect_uris = ? WHERE client_id = ?",
                redirectUris,
                postLogoutRedirectUris,
                clientId
        );
    }

    private String frontendRedirectUris() {
        Set<String> uris = new LinkedHashSet<>();
        uris.add("http://localhost:5173/callback");
        uris.add("http://127.0.0.1:5173/callback");

        if (StringUtils.hasText(frontendBaseUrl)) {
            uris.add(trimTrailingSlash(frontendBaseUrl) + "/callback");
        }

        return uris.stream().collect(Collectors.joining(","));
    }

    private String frontendPostLogoutRedirectUris() {
        Set<String> uris = new LinkedHashSet<>(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        if (StringUtils.hasText(frontendBaseUrl)) {
            uris.add(trimTrailingSlash(frontendBaseUrl));
        }

        return uris.stream().collect(Collectors.joining(","));
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void enforceParClientSettings(String clientId) {
        String raw = jdbcTemplate.query(
                "SELECT client_settings FROM oauth2_registered_client WHERE client_id = ?",
                ps -> ps.setString(1, clientId),
                rs -> rs.next() ? rs.getString(1) : null
        );
        if (!StringUtils.hasText(raw)) {
            return;
        }

        try {
            TypeReference<java.util.Map<String, Object>> mapType = new TypeReference<>() {};
            java.util.Map<String, Object> settings = objectMapper.readValue(raw, mapType);
            settings.put("settings.client.require-pushed-authorization-requests", true);
            settings.put("settings.client.require-proof-key", true);

            jdbcTemplate.update(
                    "UPDATE oauth2_registered_client SET client_settings = ? WHERE client_id = ?",
                    objectMapper.writeValueAsString(settings),
                    clientId
            );
            jdbcTemplate.update(
                    "UPDATE oauth2_registered_client SET client_authentication_methods = ?, client_secret = ? WHERE client_id = ?",
                    "none,client_secret_post",
                    "{noop}" + parClientSecret,
                    clientId
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to enforce PAR client settings for " + clientId, ex);
        }
    }
}
