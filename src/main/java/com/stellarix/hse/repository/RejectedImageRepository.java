package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.RejectedImage;

@Repository
public interface RejectedImageRepository extends JpaRepository<RejectedImage, UUID> {
    Optional<RejectedImage> findByVerificationLog_LogId(UUID logId);
    List<RejectedImage> findByExpiresAtBefore(LocalDateTime now);
}
