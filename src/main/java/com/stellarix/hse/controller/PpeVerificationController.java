package com.stellarix.hse.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stellarix.hse.dto.MissingPpeStatDto;
import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.PpeVerificationRequest;
import com.stellarix.hse.dto.TopWorkerStatDto;
import com.stellarix.hse.dto.VerificationLogResponse;
import com.stellarix.hse.service.PpeVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/hse/verifications")
@RequiredArgsConstructor
public class PpeVerificationController {

    private final PpeVerificationService service;

    /** Mobile submits a completed PPE verification result. */
    @PostMapping
    public ResponseEntity<VerificationLogResponse> submit(@Valid @RequestBody PpeVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.toResponse(service.submit(request)));
    }

    /**
     * Mobile uploads the encrypted image for a REJECTED log.
     * Only called after /verifications returns status=REJECTED.
     * VALIDATED logs must never call this endpoint.
     */
    @PostMapping("/{logId}/image")
    public ResponseEntity<Void> attachImage(
            @PathVariable UUID logId,
            @RequestPart("file") MultipartFile file) throws IOException {
        service.attachRejectedImage(logId, file.getInputStream());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Mobile batch-syncs offline verification results when connectivity is restored. */
    @PostMapping("/sync")
    public ResponseEntity<Void> sync(@Valid @RequestBody List<@Valid PpeVerificationRequest> pending) {
        service.syncOffline(pending);
        return ResponseEntity.ok().build();
    }

    // --- Backoffice endpoints (HSE role required) ---

    @GetMapping("/logs")
    public PageResponse<VerificationLogResponse> getLogs(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "siteId", required = false) Integer siteId,
            @RequestParam(name = "intent", required = false) String intent,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return new PageResponse<>(service.getLogs(from, to, siteId, intent, page, size));
    }

    @GetMapping("/stats/missing-ppe")
    public List<MissingPpeStatDto> missingPpeStats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "siteId", required = false) Integer siteId) {
        return service.getMissingPpeStats(from, to, siteId);
    }

    @GetMapping("/stats/top-workers")
    public List<TopWorkerStatDto> topWorkers(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "siteId", required = false) Integer siteId,
            @RequestParam(name = "intent", required = false) String intent,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return service.getTopWorkers(from, to, siteId, intent, limit);
    }
}
