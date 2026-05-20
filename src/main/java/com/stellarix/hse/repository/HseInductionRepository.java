package com.stellarix.hse.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.HseInduction;

@Repository
public interface HseInductionRepository extends JpaRepository<HseInduction, UUID> {

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    Optional<HseInduction> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}
