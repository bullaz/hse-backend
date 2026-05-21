package com.stellarix.hse.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.PpeMatrixResponse;
import com.stellarix.hse.dto.PpeRequirementRequest;
import com.stellarix.hse.entity.PpeRequirement;
import com.stellarix.hse.service.PpeRequirementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/ppe-matrix")
@RequiredArgsConstructor
public class PpeRequirementController {

    private final PpeRequirementService service;

    /** Mobile calls this on startup to cache the full PPE matrix across all sites. */
    @GetMapping("/mobile-cache")
    public List<Map<String, Object>> getMobileCache() {
        return service.getMobileFlatCache();
    }

    /** Mobile calls this on startup to cache the full PPE matrix for a site. */
    @GetMapping
    public PpeMatrixResponse getMatrix(@RequestParam Integer siteId) {
        return service.getMatrixForSite(siteId);
    }

    @GetMapping("/all")
    public PageResponse<PpeRequirement> getAll(
            @RequestParam(name = "zoneTypeId", required = false) Integer zoneTypeId,
            @RequestParam(name = "intent", required = false) String intent,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return new PageResponse<>(service.getAllFiltered(zoneTypeId, intent, page, size));
    }

    @PostMapping
    public ResponseEntity<PpeRequirement> add(@Valid @RequestBody PpeRequirementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
