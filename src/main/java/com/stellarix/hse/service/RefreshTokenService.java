package com.stellarix.hse.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.entity.RefreshToken;
import com.stellarix.hse.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Transactional
    public UUID issue(String email) {
        UUID jti = UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setJti(jti);
        token.setEmail(email);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        repository.save(token);
        return jti;
    }

    public boolean isValid(UUID jti) {
        return repository.findById(jti)
                .map(t -> !t.isRevoked() && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void revoke(UUID jti) {
        repository.findById(jti).ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);
        });
    }

    @Transactional
    public UUID rotate(UUID oldJti, String email) {
        revoke(oldJti);
        return issue(email);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredAndRevoked() {
        int count = repository.purgeExpiredAndRevoked(LocalDateTime.now());
        log.info("Purged {} expired/revoked refresh tokens", count);
    }
}
