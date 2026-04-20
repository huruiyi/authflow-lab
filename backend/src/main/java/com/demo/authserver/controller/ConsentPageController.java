package com.demo.authserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Objects;

@Controller
public class ConsentPageController {

    private static final OAuth2TokenType USER_CODE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.USER_CODE);

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;

    public ConsentPageController(OAuth2AuthorizationService authorizationService,
            RegisteredClientRepository registeredClientRepository) {
        this.authorizationService = authorizationService;
        this.registeredClientRepository = registeredClientRepository;
    }

    @GetMapping("/oauth2/consent")
    public ResponseEntity<Void> consentPage(@RequestParam MultiValueMap<String, String> queryParams,
            CsrfToken csrfToken) {
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, buildFrontendUrl("/consent", queryParams, csrfToken))
                .build();
    }

    @GetMapping("/activate")
    public ResponseEntity<Void> deviceVerificationPage(@RequestParam MultiValueMap<String, String> queryParams,
            CsrfToken csrfToken) {
        MultiValueMap<String, String> frontendParams = enrichDeviceVerificationParams(queryParams);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, buildFrontendUrl("/device-verification", frontendParams, csrfToken))
                .build();
    }

    private MultiValueMap<String, String> enrichDeviceVerificationParams(MultiValueMap<String, String> queryParams) {
        LinkedMultiValueMap<String, String> enrichedParams = new LinkedMultiValueMap<>(queryParams);
        if (StringUtils.hasText(enrichedParams.getFirst(OAuth2ParameterNames.CLIENT_ID))) {
            return enrichedParams;
        }

        String userCode = enrichedParams.getFirst(OAuth2ParameterNames.USER_CODE);
        if (!StringUtils.hasText(userCode)) {
            return enrichedParams;
        }

        OAuth2Authorization authorization = authorizationService.findByToken(userCode, USER_CODE_TOKEN_TYPE);
        if (authorization == null) {
            return enrichedParams;
        }

        RegisteredClient registeredClient = registeredClientRepository.findById(authorization.getRegisteredClientId());
        if (registeredClient != null) {
            enrichedParams.set(OAuth2ParameterNames.CLIENT_ID, registeredClient.getClientId());
        }

        addRequestedScopes(enrichedParams, authorization);
        return enrichedParams;
    }

    private void addRequestedScopes(LinkedMultiValueMap<String, String> enrichedParams,
            OAuth2Authorization authorization) {
        if (StringUtils.hasText(enrichedParams.getFirst(OAuth2ParameterNames.SCOPE))) {
            return;
        }

        Collection<String> scopes = authorization.getAuthorizedScopes();
        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        enrichedParams.set(OAuth2ParameterNames.SCOPE, String.join(" ", scopes));
    }

    private String buildFrontendUrl(String path, MultiValueMap<String, String> queryParams, CsrfToken csrfToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(trimTrailingSlash(Objects.requireNonNull(frontendBaseUrl)) + path);
        queryParams.forEach((key, values) -> values.forEach(value -> builder.queryParam(key, value)));
        if (csrfToken != null) {
            builder.queryParam("_csrf", csrfToken.getToken());
            builder.queryParam("_csrf_parameter", csrfToken.getParameterName());
        }
        return builder.build().encode().toUriString();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
