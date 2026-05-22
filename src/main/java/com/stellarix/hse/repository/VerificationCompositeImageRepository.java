package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.VerificationCompositeImage;

@Repository
public interface VerificationCompositeImageRepository extends JpaRepository<VerificationCompositeImage, UUID> {

    boolean existsByVerificationLog_LogId(UUID logId);

    /** Single query to resolve which log IDs in a page actually have a composite image. */
    @Query("SELECT i.logId FROM VerificationCompositeImage i WHERE i.logId IN :logIds")
    Set<UUID> findExistingLogIds(Collection<UUID> logIds);

    @Modifying
    @Query("DELETE FROM VerificationCompositeImage i WHERE i.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
