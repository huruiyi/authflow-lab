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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  private final AuthenticationManager authenticationManager;
  private final ObjectMapper objectMapper;

  @Value("${server.port}")
  private int serverPort;

  @Value("${app.par-demo.client-id}")
  private String parClientId;

  @Value("${app.par-demo.client-secret}")
  private String parClientSecret;

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

  private String toFormBody(Map<String, String> form) {
    return form.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
            + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }
}
