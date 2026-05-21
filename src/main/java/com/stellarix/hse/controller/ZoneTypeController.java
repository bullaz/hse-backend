package com.stellarix.hse.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.dto.ZoneTypeRequest;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.service.ZoneTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/zone-types")
@RequiredArgsConstructor
public class ZoneTypeController {

    private final ZoneTypeService service;

    @GetMapping
    public List<ZoneType> getAll() {
        return service.getAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<ZoneType> create(@Valid @RequestBody ZoneTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
