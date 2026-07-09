package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PpeVerificationLog;

@Repository
public interface PpeVerificationLogRepository extends JpaRepository<PpeVerificationLog, UUID> {

    // Sync / scheduler operations
    List<PpeVerificationLog> findByStatusAndSyncedAtIsNull(String status);

    boolean existsByInduction_InductionId(java.util.UUID inductionId);

    @Query("SELECT l FROM PpeVerificationLog l WHERE l.status = 'REJECTED' AND l.rejectedImage.expiresAt <= :now")
    List<PpeVerificationLog> findExpiredRejectedLogs(@Param("now") LocalDateTime now);

    // Paginated log listing
    Page<PpeVerificationLog> findByCapturedAtBetween(
        LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Page<PpeVerificationLog> findByIntentAndCapturedAtBetween(
        String intent, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Page<PpeVerificationLog> findBySite_SiteIdAndCapturedAtBetween(
        Integer siteId, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    Page<PpeVerificationLog> findBySite_SiteIdAndIntentAndCapturedAtBetween(
        Integer siteId, String intent, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    // Top workers/visitors stat
    @Query(value =
           "SELECT hi.first_name, hi.last_name, COUNT(l.log_id) AS cnt " +
           "FROM hse_schema.ppe_verification_log l " +
           "JOIN hse_schema.hse_induction hi ON hi.induction_id = l.hse_induction_id " +
           "WHERE l.status = 'VALIDATED' " +
           "AND l.captured_at BETWEEN :from AND :to " +
           "AND (:siteId IS NULL OR l.site_id = :siteId) " +
           "AND (:intent IS NULL OR l.intent = :intent) " +
           "GROUP BY hi.first_name, hi.last_name " +
           "ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findTopWorkers(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("siteId") Integer siteId,
        @Param("intent") String intent
    );
}
