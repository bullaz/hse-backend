package com.stellarix.hse.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.EmailService;
import com.stellarix.hse.service.TotpService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/hse/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HSE')")
public class UserController {

    private final AccountService accountService;
    private final TotpService totpService;
    private final EmailService emailService;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        Hse user = accountService.findByUsernameOrEmail(auth.getName());
        return ResponseEntity.ok(Map.of(
                "email",              user.getEmail(),
                "nom",                user.getNom(),
                "prenom",             user.getPrenom(),
                "username",           user.getUsername(),
                "mustChangePassword", user.isMustChangePassword(),
                "totpEnabled",        user.isTotpEnabled(),
                "isAdmin",            user.isAdmin()
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        Hse user = accountService.findByUsernameOrEmail(auth.getName());
        try {
            accountService.changePassword(user, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // Called during forced onboarding — generates QR code for the authenticated user.
    // Idempotent: if a secret already exists (setup started but not confirmed), reuses it.
    @PostMapping("/totp/setup")
    public ResponseEntity<?> setupTotp(Authentication auth) {
        Hse user = accountService.findByUsernameOrEmail(auth.getName());
        if (user.isTotpEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "TOTP déjà configuré sur ce compte"));
        }
        // Reuse existing secret if already generated (prevents double-call overwriting)
        String secret = user.getTotpSecret() != null
                ? user.getTotpSecret()
                : totpService.generateSecret();
        String label  = user.getUsername(); // use username as label — not internal email
        String qrUri  = totpService.getQrCodeUri(secret, label);
        if (user.getTotpSecret() == null) {
            user.setTotpSecret(secret);
            accountService.saveUser(user);
        }
        try {
            byte[] qrPng    = emailService.generateQrPng(qrUri, 250);
            String qrBase64 = java.util.Base64.getEncoder().encodeToString(qrPng);
            return ResponseEntity.ok(Map.of(
                    "secret",      secret,
                    "qrCodeUri",   qrUri,
                    "qrCodeImage", "data:image/png;base64," + qrBase64
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("secret", secret, "qrCodeUri", qrUri));
        }
    }

    // Confirms the TOTP code and activates 2FA — completes onboarding
    @PostMapping("/totp/enable")
    public ResponseEntity<?> enableTotp(@Valid @RequestBody TotpVerifyRequest req, Authentication auth) {
        Hse user = accountService.findByUsernameOrEmail(auth.getName());
        if (user.getTotpSecret() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Lancez d'abord la configuration TOTP"));
        }
        if (user.isTotpEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "TOTP déjà activé"));
        }
        if (!totpService.verifyCode(user.getTotpSecret(), req.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Code invalide — vérifiez votre application"));
        }
        user.setTotpEnabled(true);
        accountService.saveUser(user);
        return ResponseEntity.ok(Map.of("message", "Authentification à deux facteurs activée"));
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 12) private String newPassword;
    }

    @Data
    public static class TotpVerifyRequest {
        @NotBlank @Size(min = 6, max = 6) private String code;
    }
}
