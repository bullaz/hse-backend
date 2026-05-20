package com.stellarix.hse.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.entity.AuthRequest;
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.JwtService;
import com.stellarix.hse.service.RefreshTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/hse")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/test")
    public String healthCheck() {
        return "Stellarix HSE API is running";
    }

    @PostMapping("/signin")
    public ResponseEntity<Map<String, String>> signin(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        Hse user = accountService.findByUsernameOrEmail(request.getUsername());

        UUID jti = refreshTokenService.issue(user.getEmail());
        Map<String, String> tokens = jwtService.generateToken(user.getEmail(), user.getNom(), user.getPrenom(), jti);
        String refreshToken = tokens.remove("refresh_token");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, 7L * 24 * 60 * 60))
                .body(tokens);
    }

    @PostMapping("/verify_token")
    public Map<String, Boolean> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Boolean> result = new HashMap<>();
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            result.put("token_state", false);
            return result;
        }
        result.put("token_state", jwtService.validateToken(authHeader.substring(7)));
        return result;
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token missing");
        }
        try {
            if (!jwtService.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }
            UUID oldJti = jwtService.extractJti(refreshToken);
            if (!refreshTokenService.isValid(oldJti)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token revoked");
            }
            String email = jwtService.extractUsername(refreshToken);
            Hse user = accountService.findByUsernameOrEmail(email);

            UUID newJti = refreshTokenService.rotate(oldJti, user.getEmail());
            String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getNom(), user.getPrenom());
            String newRefreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getNom(), user.getPrenom(), newJti);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(newRefreshToken, 7L * 24 * 60 * 60))
                    .body(Map.of("access_token", newAccessToken));
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                UUID jti = jwtService.extractJti(refreshToken);
                refreshTokenService.revoke(jti);
            } catch (Exception e) {
                log.warn("Could not revoke refresh token on logout: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0))
                .body("Logged out successfully");
    }

    private String buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build()
                .toString();
    }
}
