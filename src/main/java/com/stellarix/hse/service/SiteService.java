package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.SiteRequest;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.repository.SiteRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository repository;

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
        site.setName(request.getName());
        site.setLatitude(request.getLatitude());
        site.setLongitude(request.getLongitude());
        return repository.save(site);
    }

    @Transactional
    public Site update(Integer id, SiteRequest request) {
        Site site = findById(id);
        site.setName(request.getName());
        site.setLatitude(request.getLatitude());
        site.setLongitude(request.getLongitude());
        return repository.save(site);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
