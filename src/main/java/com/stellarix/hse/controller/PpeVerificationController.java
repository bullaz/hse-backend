package com.stellarix.hse.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.stellarix.hse.service.PdfService;
import com.stellarix.hse.service.PpeInferenceService;
import com.stellarix.hse.service.PpeVerificationService;
import com.stellarix.hse.service.SiteService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/hse/verifications")
@RequiredArgsConstructor
public class PpeVerificationController {

    private final PpeVerificationService service;
    private final PdfService pdfService;
    private final SiteService siteService;
    private final PpeInferenceService inferenceService;
    private final ObjectMapper mapper;

    /**
     * Mobile posts the captured photo for cloud-side PPE inference.
     * Returns detected items + base64 annotated JPEG.
     * Public endpoint — no auth required (mobile uses it before auth flow).
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestPart("image") MultipartFile image,
            @RequestPart("items") String itemsJson,
            @RequestParam(name = "fr", defaultValue = "false") boolean isFrench) throws Exception {

        List<PpeInferenceService.RequiredItem> items = mapper.readValue(
            itemsJson, new TypeReference<List<PpeInferenceService.RequiredItem>>() {});

        PpeInferenceService.AnalysisResult result =
            inferenceService.analyze(image.getBytes(), items, isFrench);

        List<Map<String, Object>> itemsOut = result.items().stream()
            .map(i -> Map.<String, Object>of(
                "ppeCode", i.ppeCode(),
                "ppeLabel", i.ppeLabel(),
                "detected", i.detected(),
                "confidence", i.confidence(),
                "pairIncomplete", i.pairIncomplete(),
                "wrongType", i.wrongType()
            )).toList();

        return ResponseEntity.ok(Map.of(
            "items", itemsOut,
            "annotatedImageBase64", Base64.getEncoder().encodeToString(result.annotatedJpeg()),
            "personDetected", result.personDetected()
        ));
    }

    /** Mobile submits a completed PPE verification result. */
    @PostMapping
    public ResponseEntity<VerificationLogResponse> submit(@Valid @RequestBody PpeVerificationRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.toResponse(service.submit(request)));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent submission of the same logId — that request's
            // insert already committed, so retrying hits the idempotent existsById fast
            // path and returns its result instead of erroring this one out.
            return ResponseEntity.status(HttpStatus.CREATED).body(service.toResponse(service.submit(request)));
        }
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

    /**
     * Mobile uploads original + annotated images for ANY verification.
     * Server creates side-by-side composite and stores it with 48h TTL.
     */
    @PostMapping("/{logId}/images")
    public ResponseEntity<Void> uploadImages(
            @PathVariable UUID logId,
            @RequestPart("original") MultipartFile original,
            @RequestPart("annotated") MultipartFile annotated) throws IOException {
        service.saveCompositeImage(logId, original.getBytes(), annotated.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Returns the composite image (JPEG) for display in the backoffice log viewer. */
    @GetMapping(value = "/{logId}/composite-image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getCompositeImage(@PathVariable UUID logId) {
        byte[] data = service.getCompositeImage(logId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(data);
    }

    /**
     * Mobile batch-syncs offline verification results when connectivity is restored.
     * Each item is submitted independently (each call is its own transaction, since it
     * goes through the service proxy) so one bad item is skipped and logged instead of
     * rolling back — and therefore permanently blocking — every other item in the batch.
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> sync(@Valid @RequestBody List<@Valid PpeVerificationRequest> pending) {
        for (PpeVerificationRequest item : pending) {
            try {
                service.submit(item);
            } catch (DataIntegrityViolationException e) {
                try {
                    service.submit(item); // lost a race on a concurrent identical logId — retry once
                } catch (Exception retryEx) {
                    log.warn("Offline sync item {} failed after retry, skipping: {}", item.getLogId(), retryEx.getMessage());
                }
            } catch (Exception e) {
                log.warn("Offline sync item {} failed, skipping (rest of batch unaffected): {}", item.getLogId(), e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }

    /** Supervisor certifies a VALIDATED log via the mobile app. */
    @org.springframework.web.bind.annotation.PatchMapping("/{logId}/certify")
    public ResponseEntity<Void> certify(@PathVariable UUID logId) {
        service.certify(logId);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "siteId", required = false) Integer siteId,
            @RequestParam(name = "intent", required = false) String intent) throws Exception {
        List<VerificationLogResponse> logs = service.getAllLogsForExport(from, to, siteId, intent);
        String siteName = siteId != null ? siteService.findById(siteId).getName() : null;
        byte[] pdf = pdfService.generateLogsExportPdf(logs, from, to, siteName, intent);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hse-logs.pdf\"")
                .body(pdf);
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
