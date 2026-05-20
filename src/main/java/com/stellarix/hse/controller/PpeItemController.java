package com.stellarix.hse.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.dto.PpeItemRequest;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.service.PpeItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/ppe-items")
@RequiredArgsConstructor
public class PpeItemController {

    private final PpeItemService service;

    @GetMapping
    public List<PpeItem> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<PpeItem> create(@Valid @RequestBody PpeItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
