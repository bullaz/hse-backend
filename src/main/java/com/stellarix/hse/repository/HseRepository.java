package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Hse;

@Repository
public interface HseRepository extends JpaRepository<Hse, Integer> {

    Optional<Hse> findByUsernameOrEmail(String nameOrEmail, String nameOrEmail2);

    List<Hse> findByIsAdminFalseOrderByNomAscPrenomAsc();

    List<Hse> findByIsAdminFalseAndIsActiveTrue();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByIsAdminTrue();

    // Atomic increment — a read-then-write in Java code would lose updates under
    // concurrent failed logins, letting an attacker outrun the lockout threshold.
    @Modifying
    @Query("UPDATE Hse h SET h.failedAttempts = h.failedAttempts + 1 WHERE h.hseId = :id")
    int incrementFailedAttempts(@Param("id") Integer id);
}
