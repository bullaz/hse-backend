package com.stellarix.hse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.ZoneType;

@Repository
public interface ZoneTypeRepository extends JpaRepository<ZoneType, Integer> {
}
