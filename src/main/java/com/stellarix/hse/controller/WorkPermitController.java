package com.stellarix.hse.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.WorkPermitRequest;
import com.stellarix.hse.dto.WorkPermitResponse;
import com.stellarix.hse.dto.WorkPermitVerifyResponse;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.service.EmailService;
import com.stellarix.hse.service.PdfService;
import com.stellarix.hse.service.WorkPermitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/permits")
@RequiredArgsConstructor
public class WorkPermitController {

    private final WorkPermitService service;
    private final PdfService pdfService;
    private final EmailService emailService;

    /** Mobile endpoint — validates permit before allowing entry. */
    @GetMapping("/verify")
    public WorkPermitVerifyResponse verify(
            @RequestParam String permitId,
            @RequestParam Integer siteId) {
        return service.verify(permitId, siteId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<WorkPermitResponse> create(@Valid @RequestBody WorkPermitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HSE')")
    public PageResponse<WorkPermitResponse> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getAll(name, siteId, status, page, size);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<Void> revoke(@PathVariable String id) {
        service.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/qr.png")
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String id) throws Exception {
        byte[] qrBytes = emailService.generateQrPng(id, 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + "-qr.png\"")
                .body(qrBytes);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('HSE')")
    public ResponseEntity<byte[]> getPermitPdf(@PathVariable String id) throws Exception {
        WorkPermit permit = service.findById(id);
        byte[] pdf = pdfService.generatePermitPdf(permit);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"permit-" + id + ".pdf\"")
                .body(pdf);
    }
}
