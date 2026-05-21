package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.entity.PermitTemplate;

@Repository
public interface PermitTemplateRepository extends JpaRepository<PermitTemplate, Integer> {
    Optional<PermitTemplate> findByPermitType_PermitTypeId(Integer permitTypeId);
    @Transactional
    void deleteByPermitType_PermitTypeId(Integer permitTypeId);
}
