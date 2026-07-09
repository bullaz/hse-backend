package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.ZoneTypeRequest;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.PpeRequirementRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZoneTypeService {

    private final ZoneTypeRepository repository;
    private final HabilitationRepository habilitationRepository;
    private final SiteRepository siteRepository;
    private final PpeRequirementRepository requirementRepository;

    public List<ZoneType> getAll() {
        return repository.findAll();
    }

    @Transactional
    public ZoneType create(ZoneTypeRequest request) {
        ZoneType zoneType = new ZoneType();
        applyRequest(zoneType, request);
        return repository.save(zoneType);
    }

    @Transactional
    public ZoneType update(Integer id, ZoneTypeRequest request) {
        ZoneType zoneType = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ZoneType not found: " + id));
        applyRequest(zoneType, request);
        return repository.save(zoneType);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ZoneType not found: " + id);
        }

        // Check first rather than let the FK constraint reject the DELETE — a raw
        // DataIntegrityViolationException is deliberately reported to the client with
        // no detail (to avoid leaking SQL), which left the webapp showing a generic
        // "couldn't load data" message that's actively misleading for a conflict.
        long siteCount = siteRepository.countByZoneType_ZoneTypeId(id);
        long requirementCount = requirementRepository.countByZoneType_ZoneTypeId(id);
        if (siteCount > 0 || requirementCount > 0) {
            List<String> reasons = new ArrayList<>();
            if (siteCount > 0) reasons.add(siteCount + " site(s)");
            if (requirementCount > 0) reasons.add(requirementCount + " exigence(s) EPI");
            throw new IllegalStateException(
                    "Impossible de supprimer ce type de zone : encore utilisé par "
                            + String.join(" et ", reasons) + ".");
        }

        repository.deleteById(id);
    }

    private void applyRequest(ZoneType zoneType, ZoneTypeRequest request) {
        zoneType.setLabel(request.getLabel());
        zoneType.setHabilitations(request.getHabilitationIds() != null && !request.getHabilitationIds().isEmpty()
                ? habilitationRepository.findAllById(request.getHabilitationIds())
                : new ArrayList<>());
    }
}
