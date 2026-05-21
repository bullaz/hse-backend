package com.stellarix.hse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PermitType;

@Repository
public interface PermitTypeRepository extends JpaRepository<PermitType, Integer> {
    boolean existsByCodeIgnoreCase(String code);
    List<PermitType> findAllByOrderByLabelAsc();
}
