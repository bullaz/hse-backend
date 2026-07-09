package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    int purgeExpiredAndRevoked(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.email = :email AND t.revoked = false")
    int revokeAllForEmail(@Param("email") String email);
}
