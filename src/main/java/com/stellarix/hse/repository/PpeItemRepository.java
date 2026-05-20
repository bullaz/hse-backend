package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PpeItem;

@Repository
public interface PpeItemRepository extends JpaRepository<PpeItem, Integer> {
    Optional<PpeItem> findByCode(String code);
}
