package com.stellarix.hse.controller;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.EmailService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/hse/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HSE_ADMIN')")
public class AdminController {

    private final AccountService accountService;
    private final EmailService emailService;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<Map<String, Object>> users = accountService.findAllNonAdmin().stream()
                .map(u -> Map.<String, Object>of(
                        "id",               u.getHseId(),
                        "username",         u.getUsername(),
                        "email",            u.getEmail(),
                        "nom",              u.getNom(),
                        "prenom",           u.getPrenom(),
                        "isActive",         u.isActive(),
                        "totpEnabled",      u.isTotpEnabled(),
                        "mustChangePassword", u.isMustChangePassword()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (accountService.emailExists(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email déjà utilisé"));
        }
        if (accountService.usernameExists(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Identifiant déjà utilisé"));
        }

        String tempPassword = generateTempPassword();

        Hse user = new Hse();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setNom(req.getNom());
        user.setPrenom(req.getPrenom());
        user.setPassword(tempPassword);
        user.setMustChangePassword(true);
        user.setActive(true);

        try {
            accountService.addUser(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }

        emailService.sendWelcomeEmail(req.getEmail(), req.getNom(), req.getPrenom(), req.getUsername(), tempPassword);
        log.info("HSE account created for {}", req.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Compte créé. Email envoyé à " + req.getEmail()));
    }

    @PostMapping("/users/{id}/reset-totp")
    public ResponseEntity<?> resetTotp(@PathVariable Integer id) {
        try {
            Hse user = accountService.findById(id);
            if (user.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Impossible de modifier un compte administrateur"));
            }
            accountService.resetTotp(user);
            log.info("TOTP reset for user {}", user.getEmail());
            return ResponseEntity.ok(Map.of("message", "TOTP réinitialisé. L'utilisateur devra reconfigurer son application."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Integer id) {
        try {
            Hse user = accountService.findById(id);
            if (user.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Impossible de désactiver un compte administrateur"));
            }
            accountService.disableUser(user);
            log.info("Account disabled: {}", user.getEmail());
            return ResponseEntity.ok(Map.of("message", "Compte désactivé"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Integer id) {
        try {
            Hse user = accountService.findById(id);
            accountService.enableUser(user);
            log.info("Account re-enabled: {}", user.getEmail());
            return ResponseEntity.ok(Map.of("message", "Compte réactivé"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private String generateTempPassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "!@#$%&*";
        String all     = upper + lower + digits + special;
        SecureRandom rng = new SecureRandom();
        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(rng.nextInt(upper.length())));
        chars.add(lower.charAt(rng.nextInt(lower.length())));
        chars.add(digits.charAt(rng.nextInt(digits.length())));
        chars.add(special.charAt(rng.nextInt(special.length())));
        for (int i = 4; i < 14; i++) chars.add(all.charAt(rng.nextInt(all.length())));
        Collections.shuffle(chars, rng);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank private String username;
        @NotBlank @Email private String email;
        @NotBlank private String nom;
        @NotBlank private String prenom;
    }
}
