package com.demo.authserver.controller;

import com.demo.authserver.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final String JWT_AUTH_CLIENT_ID = "jwt-auth-client";
  private static final String JWT_AUTH_CLIENT_SECRET = "jwt-secret-demo-key-0123456789abcdef";
  private static final String BACKCHANNEL_LOGOUT_SECRET = "backchannel-logout-demo-key-0123456789abcdef";

  private final AuthenticationManager authenticationManager;
  private final ObjectMapper objectMapper;

  @Value("${server.port}")
  private int serverPort;

  @Value("${app.par-demo.client-id}")
  private String parClientId;

  @Value("${app.par-demo.client-secret}")
  private String parClientSecret;
  
  @Value("${app.base-url}")
  private String appBaseUrl;

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
      );
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      HttpSession session = httpRequest.getSession(true);
      session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      return ResponseEntity.ok(Map.of("username", request.getUsername()));
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户名或密码错误"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "已退出登录"));
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> status() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean loggedIn = auth != null && auth.isAuthenticated()
        && !"anonymousUser".equals(auth.getPrincipal().toString());
    if (loggedIn) {
      return ResponseEntity.ok(Map.of("loggedIn", true, "username", auth.getName()));
    }
    return ResponseEntity.ok(Map.of("loggedIn", false));
  }

  @PostMapping(value = "/par", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Map<String, Object>> createPushedAuthorizationRequest(@RequestParam Map<String, String> request) {
    try {
      Map<String, String> form = new LinkedHashMap<>(request);
      form.put("client_id", parClientId);
      form.put("client_secret", parClientSecret);

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create("http://127.0.0.1:" + serverPort + "/oauth2/par"))
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
          .POST(HttpRequest.BodyPublishers.ofString(toFormBody(form)))
          .build();

      HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      @SuppressWarnings("unchecked")
      Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
      return ResponseEntity.status(response.statusCode()).body(body);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of(
              "error", "par_relay_failed",
              "error_description", ex.getMessage()
          ));
    }
  }

  @PostMapping(value = "/jwt-client-assertion", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Map<String, Object>> generateJwtClientAssertion(@RequestParam Map<String, String> request) {
    try {
      String clientId = request.get("client_id");
      if (clientId == null || clientId.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_request",
            "error_description", "client_id is required"
        ));
      }

      String clientSecret;
      String tokenEndpoint = trimTrailingSlash(appBaseUrl) + "/oauth2/token";

      if (JWT_AUTH_CLIENT_ID.equals(clientId)) {
        clientSecret = JWT_AUTH_CLIENT_SECRET;
      } else {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_client",
            "error_description", "Unknown client_id. Use 'jwt-auth-client' for demo."
        ));
      }

      long now = System.currentTimeMillis();
      long exp = now + (5 * 60 * 1000);

      Map<String, Object> header = Map.of(
          "alg", "HS256",
          "typ", "JWT"
      );

      Map<String, Object> payload = Map.of(
          "iss", clientId,
          "sub", clientId,
          "aud", tokenEndpoint,
          "jti", UUID.randomUUID().toString(),
          "exp", exp / 1000,
          "iat", now / 1000
      );

      String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
      String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));
      String content = encodedHeader + "." + encodedPayload;

      String signature = hmacSha256(content, clientSecret);
      String clientAssertion = content + "." + signature;

      return ResponseEntity.ok(Map.of(
          "client_assertion", clientAssertion,
          "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
          "token_endpoint", tokenEndpoint,
          "expires_in", 300
      ));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", "server_error",
              "error_description", ex.getMessage()
          ));
    }
  }

  @PostMapping(value = "/token-jwt-auth", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Map<String, Object>> tokenWithJwtAuth(@RequestParam Map<String, String> request) {
    try {
      String clientId = request.get("client_id");
      if (clientId == null || clientId.isEmpty()) {
        clientId = JWT_AUTH_CLIENT_ID;
      }

      if (!JWT_AUTH_CLIENT_ID.equals(clientId)) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_client",
            "error_description", "Unknown client_id. Use 'jwt-auth-client' for demo."
        ));
      }

      String grantType = request.get("grant_type");
      if (grantType == null || grantType.isEmpty()) {
        grantType = "client_credentials";
      }

      Map<String, String> form = new LinkedHashMap<>(request);
      form.put("grant_type", grantType);
      form.put("client_id", clientId);

      String clientSecret = JWT_AUTH_CLIENT_SECRET;
      long now = System.currentTimeMillis();
      long exp = now + (5 * 60 * 1000);
      String localTokenEndpoint = "http://127.0.0.1:" + serverPort + "/oauth2/token";
      String tokenEndpoint = trimTrailingSlash(appBaseUrl) + "/oauth2/token";

      Map<String, Object> jwtHeader = Map.of("alg", "HS256", "typ", "JWT");
      Map<String, Object> jwtPayload = Map.of(
          "iss", clientId,
          "sub", clientId,
          "aud", tokenEndpoint,
          "jti", UUID.randomUUID().toString(),
          "exp", exp / 1000,
          "iat", now / 1000
      );

      String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(jwtHeader));
      String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(jwtPayload));
      String content = encodedHeader + "." + encodedPayload;
      String signature = hmacSha256(content, clientSecret);
      String clientAssertion = content + "." + signature;

      form.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
      form.put("client_assertion", clientAssertion);

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(localTokenEndpoint))
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
          .POST(HttpRequest.BodyPublishers.ofString(toFormBody(form)))
          .build();

      HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      @SuppressWarnings("unchecked")
      Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        body.put("client_assertion", clientAssertion);
        body.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
      }
      return ResponseEntity.status(response.statusCode()).body(body);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of(
              "error", "jwt_auth_failed",
              "error_description", ex.getMessage()
          ));
    }
  }

  @PostMapping(value = "/backchannel-logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<Map<String, Object>> handleBackchannelLogout(@RequestParam Map<String, String> request,
                                                                     HttpServletRequest httpRequest) {
    try {
      String logoutToken = request.get("logout_token");
      if (logoutToken != null && !logoutToken.isEmpty()) {
        return ResponseEntity.ok(processBackchannelLogout(logoutToken, httpRequest, false));
      }

      String clientId = request.get("client_id");
      if (clientId == null || clientId.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_request",
            "error_description", "client_id is required when simulating back-channel logout"
        ));
      }

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String principalName = authentication != null && authentication.isAuthenticated()
          && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))
          ? authentication.getName()
          : null;

      String idTokenHint = request.get("id_token_hint");
      String sessionId = null;
      HttpSession session = httpRequest.getSession(false);
      if (session != null) {
        sessionId = session.getId();
      }

      if (idTokenHint != null && !idTokenHint.isEmpty()) {
        Map<String, Object> idTokenClaims = parseJwtClaims(idTokenHint);
        Object subject = idTokenClaims.get("sub");
        if (subject != null) {
          principalName = String.valueOf(subject);
        }
        Object sid = idTokenClaims.get("sid");
        if (sid != null) {
          sessionId = String.valueOf(sid);
        }
      }

      if (principalName == null || principalName.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "invalid_request",
            "error_description", "No authenticated user or id_token_hint available to build logout_token"
        ));
      }

      logoutToken = buildBackchannelLogoutToken(clientId, principalName, sessionId);
      return ResponseEntity.ok(processBackchannelLogout(logoutToken, httpRequest, true));
    } catch (Exception ex) {
      return ResponseEntity.ok(Map.of(
          "status", "error",
          "error", ex.getMessage()
      ));
    }
  }

  private Map<String, Object> processBackchannelLogout(String logoutToken,
                                                       HttpServletRequest httpRequest,
                                                       boolean generatedByAuthorizationServer) throws Exception {
    Map<String, Object> tokenClaims = parseJwtClaims(logoutToken);
    HttpSession session = httpRequest.getSession(false);
    boolean sessionCleared = false;
    if (session != null) {
      session.invalidate();
      sessionCleared = true;
    }
    SecurityContextHolder.clearContext();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "ok");
    result.put("message", generatedByAuthorizationServer
        ? "Back-channel logout simulated and delivered to client endpoint"
        : "Back-channel logout received");
    result.put("generatedByAuthorizationServer", generatedByAuthorizationServer);
    result.put("clientSessionCleared", sessionCleared);
    result.put("logout_token", logoutToken);
    result.put("tokenClaims", tokenClaims);
    result.put("timestamp", System.currentTimeMillis());
    return result;
  }

  private String buildBackchannelLogoutToken(String clientId,
                                             String principalName,
                                             String sessionId) throws Exception {
    Map<String, Object> header = Map.of(
        "alg", "HS256",
        "typ", "JWT"
    );

    Map<String, Object> events = Map.of(
        "http://schemas.openid.net/event/backchannel-logout", Map.of()
    );

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("iss", trimTrailingSlash(appBaseUrl));
    payload.put("aud", clientId);
    payload.put("iat", Instant.now().getEpochSecond());
    payload.put("jti", UUID.randomUUID().toString());
    payload.put("events", events);
    payload.put("sub", principalName);
    if (sessionId != null && !sessionId.isEmpty()) {
      payload.put("sid", sessionId);
    }

    String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
    String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));
    String content = encodedHeader + "." + encodedPayload;
    String signature = hmacSha256(content, BACKCHANNEL_LOGOUT_SECRET);
    return content + "." + signature;
  }

  private Map<String, Object> parseJwtClaims(String jwtToken) throws Exception {
    String[] parts = jwtToken.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT token");
    }

    String payload = parts[1];
    String decoded = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
    @SuppressWarnings("unchecked")
    Map<String, Object> claims = objectMapper.readValue(decoded, Map.class);
    return claims;
  }

  private String base64UrlEncode(String data) throws Exception {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(data.getBytes(StandardCharsets.UTF_8));
  }

  private String hmacSha256(String data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKey);
    byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }

  private String toFormBody(Map<String, String> form) {
    return form.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
            + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }
  
  private String trimTrailingSlash(String value) {
    return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
