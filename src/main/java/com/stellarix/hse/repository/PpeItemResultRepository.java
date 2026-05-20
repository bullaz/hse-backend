package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PpeItemResult;

@Repository
public interface PpeItemResultRepository extends JpaRepository<PpeItemResult, UUID> {

    @Query(value =
           "SELECT i.code, i.label, COUNT(r.result_id) AS cnt " +
           "FROM hse_schema.ppe_item_result r " +
           "JOIN hse_schema.ppe_verification_log l ON l.log_id = r.log_id " +
           "JOIN hse_schema.ppe_item i ON i.ppe_item_id = r.ppe_item_id " +
           "WHERE r.detected = false " +
           "AND l.status = 'REJECTED' " +
           "AND l.captured_at BETWEEN :from AND :to " +
           "AND (:siteId IS NULL OR l.site_id = :siteId) " +
           "GROUP BY i.code, i.label " +
           "ORDER BY cnt DESC",
           nativeQuery = true)
    List<Object[]> findTopMissingPpe(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("siteId") Integer siteId
    );
}
