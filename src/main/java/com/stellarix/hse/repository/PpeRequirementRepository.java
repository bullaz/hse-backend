package com.stellarix.hse.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.entity.Site;

@Repository
public interface PpeRequirementRepository extends JpaRepository<PpeRequirement, Integer> {

    List<PpeRequirement> findBySiteAndIntent(Site site, String intent);

    // Non-paginated — used by mobile matrix cache lookup
    List<PpeRequirement> findBySite_SiteId(Integer siteId);

    // Paginated + filtered — used by backoffice table
    Page<PpeRequirement> findBySite_SiteId(Integer siteId, Pageable pageable);
    Page<PpeRequirement> findBySite_SiteIdAndIntent(Integer siteId, String intent, Pageable pageable);
    Page<PpeRequirement> findByIntent(String intent, Pageable pageable);
}
