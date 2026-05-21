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
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.SiteRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PpeRequirementService {

    private final PpeRequirementRepository requirementRepository;
    private final SiteRepository siteRepository;
    private final PpeItemRepository ppeItemRepository;

    /** Mobile calls this on startup to cache the full PPE matrix across all sites. */
    public List<Map<String, Object>> getMobileFlatCache() {
        record Key(int siteId, String intent) {}
        return requirementRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> new Key(r.getSite().getSiteId(), r.getIntent()),
                        LinkedHashMap::new,
                        Collectors.mapping(r -> r.getPpeItem().getCode(), Collectors.toCollection(ArrayList::new))
                ))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "siteId", e.getKey().siteId(),
                        "intent", e.getKey().intent(),
                        "ppeCodes", e.getValue()
                ))
                .collect(Collectors.toList());
    }

    /** Returns the full PPE matrix for a site grouped by intent — used by mobile on startup. */
    public PpeMatrixResponse getMatrixForSite(Integer siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + siteId));

        List<PpeRequirement> siteReqs = requirementRepository.findBySite_SiteId(siteId);

        Map<String, List<String>> requirements = siteReqs.stream()
                .collect(Collectors.groupingBy(
                    PpeRequirement::getIntent,
                    Collectors.mapping(r -> r.getPpeItem().getCode(), Collectors.toList())
                ));

        return new PpeMatrixResponse(siteId, site.getName(), requirements);
    }

    /** Backoffice: paginated listing with optional site and intent filters. */
    public Page<PpeRequirement> getAllFiltered(Integer siteId, String intent, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("requirementId").ascending());
        if (siteId != null && intent != null) {
            return requirementRepository.findBySite_SiteIdAndIntent(siteId, intent, pageable);
        } else if (siteId != null) {
            return requirementRepository.findBySite_SiteId(siteId, pageable);
        } else if (intent != null) {
            return requirementRepository.findByIntent(intent, pageable);
        }
        return requirementRepository.findAll(pageable);
    }

    @Transactional
    public PpeRequirement add(PpeRequirementRequest request) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));
        PpeItem item = ppeItemRepository.findById(request.getPpeItemId())
                .orElseThrow(() -> new EntityNotFoundException("PPE item not found: " + request.getPpeItemId()));

        PpeRequirement requirement = new PpeRequirement();
        requirement.setSite(site);
        requirement.setIntent(request.getIntent());
        requirement.setPpeItem(item);
        return requirementRepository.save(requirement);
    }

    @Transactional
    public void delete(Integer id) {
        requirementRepository.deleteById(id);
    }
}
