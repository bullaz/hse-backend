package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.SiteRequest;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository repository;
    private final ZoneTypeRepository zoneTypeRepository;
    private final HabilitationRepository habilitationRepository;

    public List<Site> getAll() {
        return repository.findAll();
    }

    public Site findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + id));
    }

    @Transactional
    public Site create(SiteRequest request) {
        Site site = new Site();
        applyRequest(site, request);
        return repository.save(site);
    }

    @Transactional
    public Site update(Integer id, SiteRequest request) {
        Site site = findById(id);
        applyRequest(site, request);
        return repository.save(site);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    private void applyRequest(Site site, SiteRequest request) {
        site.setName(request.getName());
        site.setLatitude(request.getLatitude());
        site.setLongitude(request.getLongitude());
        if (request.getZoneTypeId() != null) {
            site.setZoneType(zoneTypeRepository.findById(request.getZoneTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("ZoneType not found: " + request.getZoneTypeId())));
        } else {
            site.setZoneType(null);
        }
        site.setHabilitations(request.getHabilitationIds() != null && !request.getHabilitationIds().isEmpty()
                ? habilitationRepository.findAllById(request.getHabilitationIds())
                : new ArrayList<>());
    }
}
