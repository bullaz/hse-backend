package com.stellarix.hse.controller;

import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

import com.stellarix.hse.dto.PermitTypeRequest;
import com.stellarix.hse.dto.PermitTypeResponse;
import com.stellarix.hse.entity.PermitTemplate;
import com.stellarix.hse.service.PermitTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hse/permit-types")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HSE')")
public class PermitTypeController {

    private final PermitTypeService service;

    @GetMapping
    public List<PermitTypeResponse> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<PermitTypeResponse> create(@Valid @RequestBody PermitTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/template")
    public ResponseEntity<Void> uploadTemplate(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws Exception {
        service.uploadTemplate(id, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Integer id) {
        PermitTemplate template = service.getTemplate(id);
        String contentType = template.getContentType() != null
                ? template.getContentType() : "application/octet-stream";
        String disposition = "attachment; filename=\"" + template.getFileName() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(template.getFileData());
    }
}
