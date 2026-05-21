package com.stellarix.hse.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.WorkPermit;

@Repository
public interface WorkPermitRepository extends JpaRepository<WorkPermit, String> {

    @Query(value = "SELECT * FROM hse_schema.work_permit w " +
                   "JOIN hse_schema.hse_induction i ON i.induction_id = w.induction_id " +
                   "WHERE (CAST(:name AS text) IS NULL OR LOWER(i.first_name || ' ' || i.last_name) LIKE LOWER('%' || :name || '%')) " +
                   "AND (CAST(:siteId AS integer) IS NULL OR w.site_id = :siteId) " +
                   "AND (CAST(:status AS text) IS NULL OR w.status = :status) " +
                   "ORDER BY w.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM hse_schema.work_permit w " +
                        "JOIN hse_schema.hse_induction i ON i.induction_id = w.induction_id " +
                        "WHERE (CAST(:name AS text) IS NULL OR LOWER(i.first_name || ' ' || i.last_name) LIKE LOWER('%' || :name || '%')) " +
                        "AND (CAST(:siteId AS integer) IS NULL OR w.site_id = :siteId) " +
                        "AND (CAST(:status AS text) IS NULL OR w.status = :status)",
           nativeQuery = true)
    Page<WorkPermit> findWithFilters(@Param("name") String name,
                                     @Param("siteId") Integer siteId,
                                     @Param("status") String status,
                                     Pageable pageable);
}
