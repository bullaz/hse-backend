package com.stellarix.hse.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.entity.RejectedImage;
import com.stellarix.hse.repository.RejectedImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GdprCleanupService {

    private final RejectedImageRepository repository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRejectedImages() {
        List<RejectedImage> expired = repository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expired.isEmpty()) {
            repository.deleteAll(expired);
            log.info("GDPR cleanup: purged {} expired rejected images", expired.size());
        }
    }
}
