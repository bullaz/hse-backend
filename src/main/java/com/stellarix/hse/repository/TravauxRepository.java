package com.stellarix.hse.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.Travaux;

public interface TravauxRepository extends JpaRepository<Travaux, UUID> {

    Optional<Travaux> findByClosureToken(String token);

    Optional<Travaux> findByTicketNoIgnoreCaseAndStatus(String ticketNo, String status);

    @Query("""
            SELECT t FROM Travaux t
            WHERE (:status IS NULL OR t.status = :status)
            AND (:siteId IS NULL OR t.site.siteId = :siteId)
            AND (:ticketNo IS NULL OR LOWER(t.ticketNo) LIKE LOWER(CONCAT('%', CAST(:ticketNo AS String), '%')))
            ORDER BY t.createdAt DESC
            """)
    Page<Travaux> findWithFilters(
            @Param("status") String status,
            @Param("siteId") Integer siteId,
            @Param("ticketNo") String ticketNo,
            Pageable pageable);
}
