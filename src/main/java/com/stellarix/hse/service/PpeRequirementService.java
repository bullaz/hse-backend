package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.PpeMatrixResponse;
import com.stellarix.hse.dto.PpeRequirementRequest;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PpeRequirementService {

    private final PpeRequirementRepository requirementRepository;
    private final SiteRepository siteRepository;
    private final ZoneTypeRepository zoneTypeRepository;
    private final PpeItemRepository ppeItemRepository;

    /** Mobile calls this on startup to cache the full PPE matrix across all sites (by zone type). */
    public List<Map<String, Object>> getMobileFlatCache() {
        record Key(int zoneTypeId, String intent) {}
        return requirementRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> new Key(r.getZoneType().getZoneTypeId(), r.getIntent()),
                        LinkedHashMap::new,
                        Collectors.mapping(r -> r.getPpeItem().getCode(), Collectors.toCollection(ArrayList::new))
                ))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "zoneTypeId", e.getKey().zoneTypeId(),
                        "intent", e.getKey().intent(),
                        "ppeCodes", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Returns the full PPE matrix for a site grouped by intent — used by mobile on startup.
     * Looks up the zone type from the site, then returns requirements for that zone type.
     */
    public PpeMatrixResponse getMatrixForSite(Integer siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + siteId));

        if (site.getZoneType() == null) {
            return new PpeMatrixResponse(siteId, site.getName(), Map.of());
        }

        List<PpeRequirement> zoneReqs = requirementRepository
                .findByZoneType_ZoneTypeId(site.getZoneType().getZoneTypeId());

        Map<String, List<String>> requirements = zoneReqs.stream()
                .collect(Collectors.groupingBy(
                    PpeRequirement::getIntent,
                    Collectors.mapping(r -> r.getPpeItem().getCode(), Collectors.toList())
                ));

        return new PpeMatrixResponse(siteId, site.getName(), requirements);
    }

    /** Backoffice: paginated listing with optional zone type and intent filters. */
    public Page<PpeRequirement> getAllFiltered(Integer zoneTypeId, String intent, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("requirementId").ascending());
        if (zoneTypeId != null && intent != null) {
            return requirementRepository.findByZoneType_ZoneTypeIdAndIntent(zoneTypeId, intent, pageable);
        } else if (zoneTypeId != null) {
            return requirementRepository.findByZoneType_ZoneTypeId(zoneTypeId, pageable);
        } else if (intent != null) {
            return requirementRepository.findByIntent(intent, pageable);
        }
        return requirementRepository.findAll(pageable);
    }

    @Transactional
    public PpeRequirement add(PpeRequirementRequest request) {
        ZoneType zoneType = zoneTypeRepository.findById(request.getZoneTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Zone type not found: " + request.getZoneTypeId()));
        PpeItem item = ppeItemRepository.findById(request.getPpeItemId())
                .orElseThrow(() -> new EntityNotFoundException("PPE item not found: " + request.getPpeItemId()));

        if (requirementRepository.existsByZoneType_ZoneTypeIdAndIntentAndPpeItem_PpeItemId(
                request.getZoneTypeId(), request.getIntent(), request.getPpeItemId())) {
            throw new IllegalStateException("Duplicate PPE requirement");
        }

        PpeRequirement requirement = new PpeRequirement();
        requirement.setZoneType(zoneType);
        requirement.setIntent(request.getIntent());
        requirement.setPpeItem(item);
        return requirementRepository.save(requirement);
    }

    @Transactional
    public void delete(Integer id) {
        requirementRepository.deleteById(id);
    }
}
