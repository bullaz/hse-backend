package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Company> findByNameIgnoreCase(String name);
    List<Company> findAllByOrderByNameAsc();
}
