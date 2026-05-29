package com.stellarix.hse.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.TravauxClosureRequest;
import com.stellarix.hse.dto.TravauxEntryVerifyResponse;
import com.stellarix.hse.dto.TravauxRequest;
import com.stellarix.hse.dto.TravauxResponse;
import com.stellarix.hse.service.TravauxService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/travaux")
@RequiredArgsConstructor
public class TravauxController {

    private final TravauxService service;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<TravauxResponse> create(
            @RequestPart("data") TravauxRequest request,
            @RequestPart(value = "modop", required = false) MultipartFile modop) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, modop));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TravauxResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(status, siteId, ticketNo, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravauxResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getDetail(id));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<TravauxResponse> update(
            @PathVariable UUID id,
            @RequestPart("data") TravauxRequest request,
            @RequestPart(value = "modop", required = false) MultipartFile modop) {
        return ResponseEntity.ok(service.update(id, request, modop));
    }

    @PostMapping("/{id}/send-permits")
    public ResponseEntity<Void> sendPermits(@PathVariable UUID id) {
        service.sendPermits(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        service.activate(id);
        return ResponseEntity.ok().build();
    }

    /** Public endpoint — supervisor page loads travaux details by token. */
    @GetMapping("/by-token")
    public ResponseEntity<?> getByToken(@RequestParam String token) {
        try {
            return ResponseEntity.ok(service.getByToken(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(e.getMessage());
        }
    }

    /** Create a permit directly inside a Travaux (no induction needed). */
    @PostMapping(value = "/{id}/permits", consumes = {"multipart/form-data"})
    public ResponseEntity<Void> createPermit(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer permitTypeId,
            @RequestParam(required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        service.createPermitForTravaux(id, permitTypeId, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Public endpoint — mobile entry ticket verification. */
    @GetMapping("/verify-entry")
    public ResponseEntity<?> verifyEntry(
            @RequestParam String ticketNo,
            @RequestParam String firstName,
            @RequestParam String lastName) {
        try {
            TravauxEntryVerifyResponse response = service.verifyEntry(ticketNo, firstName, lastName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /** Direct closure for VISITE dossiers (no supervisor form needed). */
    @PatchMapping("/{id}/close-visite")
    public ResponseEntity<Void> closeVisite(@PathVariable UUID id) {
        service.closeVisite(id);
        return ResponseEntity.ok().build();
    }

    /** Public endpoint — token-authenticated only, no JWT required. */
    @PostMapping("/close")
    public ResponseEntity<?> supervisorClose(@RequestBody TravauxClosureRequest request) {
        try {
            service.supervisorClose(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/validate-closure")
    public ResponseEntity<Void> validateClosure(@PathVariable UUID id) {
        service.validateClosure(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reject-closure")
    public ResponseEntity<Void> rejectClosure(@PathVariable UUID id) {
        service.rejectClosure(id);
        return ResponseEntity.ok().build();
    }
}
