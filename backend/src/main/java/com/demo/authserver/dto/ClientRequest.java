package com.demo.authserver.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClientRequest {
  private String clientId;
  private String clientSecret;
  private String clientName;
  private List<String> clientAuthenticationMethods;
  private List<String> authorizationGrantTypes;
  private List<String> redirectUris;
  private List<String> postLogoutRedirectUris;
  private List<String> scopes;
  private boolean requireProofKey;
  private boolean requireAuthorizationConsent;
}
