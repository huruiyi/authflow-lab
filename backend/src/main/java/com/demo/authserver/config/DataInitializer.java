package com.demo.authserver.config;

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

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public DataInitializer(JdbcUserDetailsManager userDetailsManager,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
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
        syncClientRedirectUris("all-in-one-client", frontendRedirectUris(), frontendPostLogoutRedirectUris());
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
}
