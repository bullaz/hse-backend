package com.stellarix.hse.controller;

import java.util.Map;
import java.util.UUID;

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

    /** Mobile calls this before the PPE check to verify the worker's induction status. */
    @GetMapping("/verify")
    public Map<String, Boolean> verify(@RequestParam String firstName, @RequestParam String lastName) {
        return Map.of("inducted", service.isInducted(firstName, lastName));
    }

    @PostMapping
    public ResponseEntity<HseInduction> add(@Valid @RequestBody HseInductionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(request.getFirstName(), request.getLastName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
