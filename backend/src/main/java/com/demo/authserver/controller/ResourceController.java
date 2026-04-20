package com.demo.authserver.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resource")
public class ResourceController {

    /** 公开接口，无需 Token */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of(
                "endpoint", "GET /resource/public",
                "auth", "无需认证",
                "message", "欢迎！这是公开接口，无需携带 Token。",
                "serverTime", LocalDateTime.now().toString()
        );
    }

    /** 需要 profile scope，返回当前 Token 的用户信息 */
    @GetMapping("/profile")
    public Map<String, Object> profile(Authentication auth, @AuthenticationPrincipal Jwt jwt) {
        List<String> scopes = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return Map.of(
                "endpoint", "GET /resource/profile",
                "auth", "需要 SCOPE_profile",
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),
                "grantedScopes", scopes,
                "issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : "N/A",
                "expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : "N/A",
                "tokenId", jwt.getId() != null ? jwt.getId() : "N/A",
                "allClaims", jwt.getClaims()
        );
    }

    /** 需要 read scope，返回商品列表（模拟只读资源） */
    @GetMapping("/read")
    public Map<String, Object> readData(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "endpoint", "GET /resource/read",
                "auth", "需要 SCOPE_read",
                "accessedBy", jwt.getSubject(),
                "message", "数据读取成功",
                "products", List.of(
                        Map.of("id", 1, "name", "MacBook Pro 16\"", "price", 19999, "stock", 50),
                        Map.of("id", 2, "name", "iPhone 16 Pro",   "price", 9999,  "stock", 200),
                        Map.of("id", 3, "name", "AirPods Pro",      "price", 1999,  "stock", 500)
                )
        );
    }

    /** 需要 write scope，写入数据（模拟写资源） */
    @PostMapping("/write")
    public Map<String, Object> writeData(@RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "endpoint", "POST /resource/write",
                "auth", "需要 SCOPE_write",
                "message", "数据写入成功",
                "savedBy", jwt.getSubject(),
                "savedAt", LocalDateTime.now().toString(),
                "receivedData", body
        );
    }

    /** 任意有效 Token 均可访问，展示完整 Token 信息 */
    @GetMapping("/token-info")
    public Map<String, Object> tokenInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "endpoint", "GET /resource/token-info",
                "auth", "任意有效 Token",
                "tokenId", jwt.getId() != null ? jwt.getId() : "N/A",
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),
                "issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : "N/A",
                "expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : "N/A",
                "allClaims", jwt.getClaims()
        );
    }
}
