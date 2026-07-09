package com.stellarix.hse.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import com.stellarix.hse.service.TotpService;

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
    private final TotpService totpService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @GetMapping("/test")
    public String healthCheck() {
        return "Stellarix HSE API is running";
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody AuthRequest request) {
        // 1. Resolve user — generic 401 to prevent user enumeration
        Hse user;
        try {
            user = accountService.findByUsernameOrEmail(request.getUsername());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants incorrects"));
        }

        // 2. Disabled account
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ce compte a été désactivé. Contactez l'administrateur."));
        }

        // 3. Brute force lockout
        if (accountService.isAccountLocked(user)) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("error", "Compte verrouillé. Réessayez dans "
                            + accountService.minutesUntilUnlock(user) + " minute(s)."));
        }

        // 4. Password verification
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException | LockedException | DisabledException e) {
            accountService.recordFailedAttempt(user);
            if (accountService.isAccountLocked(user)) {
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(Map.of("error", "Trop de tentatives. Compte verrouillé 15 minutes."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants incorrects"));
        }

        // 5. TOTP check (only for users who have already set it up).
        // Failed attempts are only reset once the login is fully complete — a wrong
        // TOTP code is just as much a failed login as a wrong password and must count
        // toward the same lockout, otherwise the password check's lockout is moot for
        // anyone who already has (or guessed) the password.
        if (user.isTotpEnabled()) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(Map.of("requiresTotp", true));
            }
            long step = totpService.verifyCodeStep(user.getTotpSecret(), request.getTotpCode(), user.getTotpLastUsedStep());
            if (step < 0) {
                accountService.recordFailedAttempt(user);
                if (accountService.isAccountLocked(user)) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body(Map.of("error", "Trop de tentatives. Compte verrouillé 15 minutes."));
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Code authentificateur invalide"));
            }
            user.setTotpLastUsedStep(step);
        }

        accountService.resetFailedAttempts(user);
        accountService.saveUser(user);

        // 6. Issue tokens
        UUID jti = refreshTokenService.issue(user.getEmail());
        Map<String, String> tokens = jwtService.generateToken(user.getEmail(), user.getNom(), user.getPrenom(), jti);
        String refreshToken = tokens.remove("refresh_token");

        Map<String, Object> body = new HashMap<>(tokens);
        body.put("must_change_password", user.isMustChangePassword());
        body.put("totp_enabled", user.isTotpEnabled());
        body.put("is_admin", user.isAdmin());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, 7L * 24 * 60 * 60))
                .body(body);
    }

    @PostMapping("/verify_token")
    public Map<String, Boolean> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            Hse user = accountService.findByUsernameOrEmailOptional(email)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token invalid");
            }

            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account disabled");
            }

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
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build()
                .toString();
    }
}
