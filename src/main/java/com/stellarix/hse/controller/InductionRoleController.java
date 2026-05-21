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

import com.stellarix.hse.dto.InductionRoleRequest;
import com.stellarix.hse.entity.InductionRole;
import com.stellarix.hse.service.InductionRoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/induction-roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HSE')")
public class InductionRoleController {

    private final InductionRoleService service;

    @GetMapping
    public List<InductionRole> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<InductionRole> create(@Valid @RequestBody InductionRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
