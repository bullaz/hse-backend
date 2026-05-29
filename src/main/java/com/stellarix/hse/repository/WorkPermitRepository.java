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

import com.stellarix.hse.entity.WorkPermit;

@Repository
public interface WorkPermitRepository extends JpaRepository<WorkPermit, String> {

    @Query(value = "SELECT w.* FROM hse_schema.work_permit w " +
                   "LEFT JOIN hse_schema.hse_induction i ON i.induction_id = w.induction_id " +
                   "WHERE (CAST(:name AS text) IS NULL OR LOWER(i.first_name || ' ' || i.last_name) LIKE LOWER('%' || :name || '%')) " +
                   "AND (CAST(:siteId AS integer) IS NULL OR w.site_id = :siteId) " +
                   "AND (CAST(:status AS text) IS NULL " +
                   "  OR (:status = 'ACTIVE'  AND w.status = 'ACTIVE'  AND w.end_datetime >= NOW()) " +
                   "  OR (:status = 'EXPIRED' AND w.status = 'ACTIVE'  AND w.end_datetime <  NOW()) " +
                   "  OR (:status = 'REVOKED' AND w.status = 'REVOKED')) " +
                   "AND (CAST(:dateFrom AS timestamp) IS NULL OR w.created_at >= CAST(:dateFrom AS timestamp)) " +
                   "AND (CAST(:dateTo AS timestamp) IS NULL OR w.created_at <= CAST(:dateTo AS timestamp)) " +
                   "ORDER BY w.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM hse_schema.work_permit w " +
                        "LEFT JOIN hse_schema.hse_induction i ON i.induction_id = w.induction_id " +
                        "WHERE (CAST(:name AS text) IS NULL OR LOWER(i.first_name || ' ' || i.last_name) LIKE LOWER('%' || :name || '%')) " +
                        "AND (CAST(:siteId AS integer) IS NULL OR w.site_id = :siteId) " +
                        "AND (CAST(:status AS text) IS NULL " +
                        "  OR (:status = 'ACTIVE'  AND w.status = 'ACTIVE'  AND w.end_datetime >= NOW()) " +
                        "  OR (:status = 'EXPIRED' AND w.status = 'ACTIVE'  AND w.end_datetime <  NOW()) " +
                        "  OR (:status = 'REVOKED' AND w.status = 'REVOKED')) " +
                        "AND (CAST(:dateFrom AS timestamp) IS NULL OR w.created_at >= CAST(:dateFrom AS timestamp)) " +
                        "AND (CAST(:dateTo AS timestamp) IS NULL OR w.created_at <= CAST(:dateTo AS timestamp))",
           nativeQuery = true)
    Page<WorkPermit> findWithFilters(@Param("name") String name,
                                     @Param("siteId") Integer siteId,
                                     @Param("status") String status,
                                     @Param("dateFrom") String dateFrom,
                                     @Param("dateTo") String dateTo,
                                     Pageable pageable);

    List<WorkPermit> findByTravauxTravauxId(UUID travauxId);

    long countByTravauxTravauxId(UUID travauxId);

    @Query("SELECT w FROM WorkPermit w WHERE w.induction.inductionId = :inductionId " +
           "AND w.site.siteId = :siteId AND w.status = 'ACTIVE' AND w.endDatetime >= :now " +
           "ORDER BY w.createdAt DESC")
    List<WorkPermit> findActiveByInductionAndSite(@Param("inductionId") UUID inductionId,
                                                   @Param("siteId") Integer siteId,
                                                   @Param("now") LocalDateTime now);
}
