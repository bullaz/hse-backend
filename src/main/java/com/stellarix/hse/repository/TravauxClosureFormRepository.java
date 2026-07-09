package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stellarix.hse.entity.TravauxClosureForm;

public interface TravauxClosureFormRepository extends JpaRepository<TravauxClosureForm, UUID> {
    Optional<TravauxClosureForm> findByTravaux_TravauxIdAndStatus(UUID travauxId, String status);

    List<TravauxClosureForm> findByTravaux_TravauxIdOrderBySubmittedAtDesc(UUID travauxId);
}
