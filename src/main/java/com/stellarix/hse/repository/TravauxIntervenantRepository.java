package com.stellarix.hse.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.TravauxIntervenant;

@Repository
public interface TravauxIntervenantRepository extends JpaRepository<TravauxIntervenant, UUID> {
    long countByInduction_InductionId(UUID inductionId);
}
