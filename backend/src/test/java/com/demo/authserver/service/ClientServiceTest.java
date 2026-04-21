package com.demo.authserver.service;

import com.demo.authserver.dto.ClientRequest;
import com.demo.authserver.dto.ClientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    @Captor
    private ArgumentCaptor<RegisteredClient> clientCaptor;

    @Test
    void createShouldSaveClientAndReturnResponse() {
        ClientRequest request = new ClientRequest();
        request.setClientId("new-client");
        request.setClientName("New Client");
        request.setClientSecret("secret");
        request.setClientAuthenticationMethods(List.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(List.of("client_credentials"));
        request.setScopes(List.of("read", "write"));
        request.setRequireProofKey(true);
        request.setRequireAuthorizationConsent(false);

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        ClientResponse response = clientService.create(request);

        verify(registeredClientRepository).save(clientCaptor.capture());
        RegisteredClient saved = clientCaptor.getValue();

        assertEquals("new-client", saved.getClientId());
        assertEquals("New Client", saved.getClientName());
        assertEquals("encoded-secret", saved.getClientSecret());
        assertTrue(saved.getClientSettings().isRequireProofKey());
        assertFalse(saved.getClientSettings().isRequireAuthorizationConsent());

        assertEquals(saved.getId(), response.getId());
        assertEquals("new-client", response.getClientId());
        assertEquals("New Client", response.getClientName());
        assertEquals(List.of("read", "write"), response.getScopes());
    }

    @Test
    void updateShouldThrowWhenClientDoesNotExist() {
        when(registeredClientRepository.findById("missing")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> clientService.update("missing", new ClientRequest())
        );

        assertEquals("Client not found: missing", ex.getMessage());
    }

    @Test
    void updateShouldKeepExistingClientIdAndSecretWhenSecretNotProvided() {
        RegisteredClient existing = RegisteredClient.withId("id-1")
                .clientId("immutable-client-id")
                .clientName("Old Name")
                .clientSecret("existing-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .clientIdIssuedAt(Instant.now())
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .tokenSettings(TokenSettings.builder().build())
                .build();

        ClientRequest request = new ClientRequest();
        request.setClientName("Updated Name");
        request.setClientSecret("  ");
        request.setClientAuthenticationMethods(List.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(List.of("client_credentials"));
        request.setScopes(List.of("read"));

        when(registeredClientRepository.findById("id-1")).thenReturn(existing);

        ClientResponse updatedResponse = clientService.update("id-1", request);

        verify(jdbcTemplate).update("DELETE FROM oauth2_registered_client WHERE id = ?", "id-1");
        verify(registeredClientRepository).save(clientCaptor.capture());

        RegisteredClient saved = clientCaptor.getValue();
        assertEquals("immutable-client-id", saved.getClientId());
        assertEquals("existing-secret", saved.getClientSecret());
        assertEquals("Updated Name", saved.getClientName());

        assertEquals("id-1", updatedResponse.getId());
        assertEquals("immutable-client-id", updatedResponse.getClientId());
        assertEquals("Updated Name", updatedResponse.getClientName());
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        RegisteredClient existing = RegisteredClient.withId("id-2")
                .clientId("client-2")
                .clientName("Client Two")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().build())
                .build();

        when(registeredClientRepository.findById("id-2")).thenReturn(existing);

        ClientResponse response = clientService.findById("id-2");

        assertEquals("id-2", response.getId());
        assertEquals("client-2", response.getClientId());
        assertEquals("Client Two", response.getClientName());
        assertTrue(response.isRequireAuthorizationConsent());
    }

    @Test
    void deleteShouldRemoveClientAndRelatedRecords() {
        clientService.delete("client-id");

        verify(jdbcTemplate, times(1))
                .update(eq("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?"), eq("client-id"));
        verify(jdbcTemplate, times(1))
                .update(eq("DELETE FROM oauth2_authorization WHERE registered_client_id = ?"), eq("client-id"));
        verify(jdbcTemplate, times(1))
                .update(eq("DELETE FROM oauth2_registered_client WHERE id = ?"), eq("client-id"));
    }
}
