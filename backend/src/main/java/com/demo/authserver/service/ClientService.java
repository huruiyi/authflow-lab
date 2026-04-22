package com.demo.authserver.service;

import com.demo.authserver.dto.ClientRequest;
import com.demo.authserver.dto.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

  private final RegisteredClientRepository registeredClientRepository;
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  private static final String SELECT_ALL =
      "SELECT id, client_id, client_name, client_authentication_methods, " +
          "authorization_grant_types, redirect_uris, post_logout_redirect_uris, " +
          "scopes, client_id_issued_at, client_settings, token_settings " +
          "FROM oauth2_registered_client ORDER BY client_id_issued_at DESC";

  public List<ClientResponse> findAll() {
    return jdbcTemplate.query(SELECT_ALL, this::mapRowToClientResponse);
  }

  public ClientResponse findById(String id) {
    RegisteredClient client = registeredClientRepository.findById(id);
    if (client == null) {
      throw new IllegalArgumentException("Client not found: " + id);
    }
    return toClientResponse(client);
  }

  public ClientResponse create(ClientRequest request) {
    RegisteredClient.Builder builder = RegisteredClient
        .withId(UUID.randomUUID().toString())
        .clientId(request.getClientId())
        .clientName(request.getClientName());

    applySecret(builder, request.getClientSecret(), null);
    applyRequestToBuilder(builder, request);

    RegisteredClient client = builder.build();
    registeredClientRepository.save(client);
    return toClientResponse(client);
  }

  public ClientResponse update(String id, ClientRequest request) {
    RegisteredClient existing = registeredClientRepository.findById(id);
    if (existing == null) {
      throw new IllegalArgumentException("Client not found: " + id);
    }

    // Delete old record and re-insert with same id (client_id stays immutable)
    jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);

    RegisteredClient.Builder builder = RegisteredClient.withId(id)
        .clientId(existing.getClientId())
        .clientName(request.getClientName());

    applySecret(builder, request.getClientSecret(), existing.getClientSecret());
    applyRequestToBuilder(builder, request);

    RegisteredClient updated = builder.build();
    registeredClientRepository.save(updated);
    return toClientResponse(updated);
  }

  public void delete(String id) {
    jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
    jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
  }

  private void applySecret(RegisteredClient.Builder builder, String newSecret, String existingSecret) {
    if (StringUtils.hasText(newSecret)) {
      builder.clientSecret(passwordEncoder.encode(newSecret));
    } else if (StringUtils.hasText(existingSecret)) {
      builder.clientSecret(existingSecret);
    }
  }

  private void applyRequestToBuilder(RegisteredClient.Builder builder, ClientRequest request) {
    if (request.getClientAuthenticationMethods() != null) {
      request.getClientAuthenticationMethods().stream()
          .filter(StringUtils::hasText)
          .map(ClientAuthenticationMethod::new)
          .forEach(builder::clientAuthenticationMethod);
    }

    if (request.getAuthorizationGrantTypes() != null) {
      request.getAuthorizationGrantTypes().stream()
          .filter(StringUtils::hasText)
          .map(AuthorizationGrantType::new)
          .forEach(builder::authorizationGrantType);
    }

    if (request.getRedirectUris() != null) {
      request.getRedirectUris().stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .forEach(builder::redirectUri);
    }

    if (request.getPostLogoutRedirectUris() != null) {
      request.getPostLogoutRedirectUris().stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .forEach(builder::postLogoutRedirectUri);
    }

    if (request.getScopes() != null) {
      request.getScopes().stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .forEach(builder::scope);
    }

    builder.clientSettings(ClientSettings.builder()
        .requireProofKey(request.isRequireProofKey())
        .requireAuthorizationConsent(request.isRequireAuthorizationConsent())
        .build());

    builder.tokenSettings(TokenSettings.builder().build());
  }

  private ClientResponse mapRowToClientResponse(ResultSet rs, int rowNum) throws SQLException {
    String authMethods = rs.getString("client_authentication_methods");
    String grantTypes = rs.getString("authorization_grant_types");
    String redirectUris = rs.getString("redirect_uris");
    String postLogoutUris = rs.getString("post_logout_redirect_uris");
    String scopes = rs.getString("scopes");

    return ClientResponse.builder()
        .id(rs.getString("id"))
        .clientId(rs.getString("client_id"))
        .clientName(rs.getString("client_name"))
        .clientAuthenticationMethods(splitToList(authMethods))
        .authorizationGrantTypes(splitToList(grantTypes))
        .redirectUris(splitToList(redirectUris))
        .postLogoutRedirectUris(splitToList(postLogoutUris))
        .scopes(splitToList(scopes))
        .clientIdIssuedAt(rs.getTimestamp("client_id_issued_at") != null
            ? rs.getTimestamp("client_id_issued_at").toLocalDateTime().toString() : null)
        .build();
  }

  private ClientResponse toClientResponse(RegisteredClient client) {
    return ClientResponse.builder()
        .id(client.getId())
        .clientId(client.getClientId())
        .clientName(client.getClientName())
        .clientAuthenticationMethods(client.getClientAuthenticationMethods().stream()
            .map(ClientAuthenticationMethod::getValue)
            .collect(Collectors.toList()))
        .authorizationGrantTypes(client.getAuthorizationGrantTypes().stream()
            .map(AuthorizationGrantType::getValue)
            .collect(Collectors.toList()))
        .redirectUris(new ArrayList<>(client.getRedirectUris()))
        .postLogoutRedirectUris(new ArrayList<>(client.getPostLogoutRedirectUris()))
        .scopes(new ArrayList<>(client.getScopes()))
        .requireProofKey(client.getClientSettings().isRequireProofKey())
        .requireAuthorizationConsent(client.getClientSettings().isRequireAuthorizationConsent())
        .clientIdIssuedAt(client.getClientIdIssuedAt() != null
            ? client.getClientIdIssuedAt().toString() : null)
        .build();
  }

  private List<String> splitToList(String value) {
    if (!StringUtils.hasText(value)) return new ArrayList<>();
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toList());
  }
}
