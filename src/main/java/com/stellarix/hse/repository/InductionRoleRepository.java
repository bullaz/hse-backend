package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.InductionRole;

@Repository
public interface InductionRoleRepository extends JpaRepository<InductionRole, Integer> {
    boolean existsByNameIgnoreCase(String name);
    Optional<InductionRole> findByNameIgnoreCase(String name);
    List<InductionRole> findAllByOrderByNameAsc();
}
