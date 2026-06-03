package com.stellarix.hse.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.dto.HseInductionRequest;
import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.service.HseInductionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/inductions")
@RequiredArgsConstructor
public class HseInductionController {

    private final HseInductionService service;

    @GetMapping
    public PageResponse<HseInduction> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return new PageResponse<>(service.getAll(page, size));
    }

    /** Mobile calls this on startup to cache all inducted names (with IDs) for offline checks. */
    @GetMapping("/names")
    public List<Map<String, Object>> getAllNames() {
        return service.getAllNames();
    }

    /** Mobile calls this before the PPE check. Returns {@code {inducted, inductionId?}}. */
    @GetMapping("/verify")
    public Map<String, Object> verify(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String cin) {
        return service.verifyInduction(firstName, lastName, cin);
    }

    @PostMapping
    public ResponseEntity<HseInduction> add(@Valid @RequestBody HseInductionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(request));
    }

    @PutMapping("/{id}")
    public HseInduction update(@PathVariable UUID id, @Valid @RequestBody HseInductionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
