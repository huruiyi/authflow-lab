package com.demo.authserver.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClientResponse {
  private String id;
  private String clientId;
  private String clientName;
  private List<String> clientAuthenticationMethods;
  private List<String> authorizationGrantTypes;
  private List<String> redirectUris;
  private List<String> postLogoutRedirectUris;
  private List<String> scopes;
  private boolean requireProofKey;
  private boolean requireAuthorizationConsent;
  private String clientIdIssuedAt;
}
