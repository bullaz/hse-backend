package com.stellarix.hse.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stellarix.hse.entity.TravauxClosureForm;

public interface TravauxClosureFormRepository extends JpaRepository<TravauxClosureForm, UUID> {
}
