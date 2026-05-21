package com.stellarix.hse.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PpeRequirement;

@Repository
public interface PpeRequirementRepository extends JpaRepository<PpeRequirement, Integer> {

    // Non-paginated — used by mobile matrix cache lookup (via zone type derived from site)
    List<PpeRequirement> findByZoneType_ZoneTypeId(Integer zoneTypeId);

    // Paginated + filtered — used by backoffice table
    Page<PpeRequirement> findByZoneType_ZoneTypeId(Integer zoneTypeId, Pageable pageable);
    Page<PpeRequirement> findByZoneType_ZoneTypeIdAndIntent(Integer zoneTypeId, String intent, Pageable pageable);
    Page<PpeRequirement> findByIntent(String intent, Pageable pageable);
}
