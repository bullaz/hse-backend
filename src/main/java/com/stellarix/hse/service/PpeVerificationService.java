package com.stellarix.hse.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.MissingPpeStatDto;
import com.stellarix.hse.dto.PpeVerificationRequest;
import com.stellarix.hse.dto.TopWorkerStatDto;
import com.stellarix.hse.dto.VerificationLogResponse;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.PpeItemResult;
import com.stellarix.hse.entity.PpeVerificationLog;
import com.stellarix.hse.entity.RejectedImage;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeItemResultRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;
import com.stellarix.hse.repository.RejectedImageRepository;
import com.stellarix.hse.repository.SiteRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PpeVerificationService {

    private final PpeVerificationLogRepository logRepository;
    private final PpeItemRepository ppeItemRepository;
    private final PpeItemResultRepository resultRepository;
    private final RejectedImageRepository imageRepository;
    private final SiteRepository siteRepository;
    private final HseInductionRepository inductionRepository;

    @Transactional
    public PpeVerificationLog submit(PpeVerificationRequest request) {
        if (logRepository.existsById(request.getLogId())) {
            return logRepository.findById(request.getLogId())
                    .orElseThrow(() -> new EntityNotFoundException("Verification log not found: " + request.getLogId()));
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        HseInduction induction = inductionRepository.findById(request.getInductionId())
                .orElseThrow(() -> new EntityNotFoundException("Induction not found: " + request.getInductionId()));

        PpeVerificationLog log = new PpeVerificationLog();
        log.setLogId(request.getLogId());
        log.setInduction(induction);
        log.setIntent(request.getIntent());
        log.setSite(site);
        log.setStatus(request.getStatus());
        log.setCapturedAt(request.getCapturedAt());
        log.setOffline(request.isOffline());
        log.setSyncedAt(request.isOffline() ? LocalDateTime.now() : null);

        PpeVerificationLog saved = logRepository.save(log);

        List<PpeItemResult> results = request.getItemResults().stream().map(r -> {
            PpeItemResult result = new PpeItemResult();
            result.setVerificationLog(saved);
            result.setPpeItem(ppeItemRepository.findByCode(r.getPpeItemCode())
                    .orElseThrow(() -> new EntityNotFoundException("PPE item not found: " + r.getPpeItemCode())));
            result.setDetected(r.isDetected());
            result.setConfidence(r.getConfidence());
            return result;
        }).toList();

        resultRepository.saveAll(results);

        if ("REJECTED".equals(request.getStatus())) {
            List<String> causes = request.getItemResults().stream()
                    .filter(r -> !r.isDetected())
                    .map(r -> "MISSING_PPE:" + r.getPpeItemCode())
                    .toList();
            saved.setRejectionCauses(causes);
            logRepository.save(saved);
        }

        return saved;
    }

    /**
     * Attaches an encrypted image to a REJECTED log for the 48-hour supervisor review window.
     * Never called for VALIDATED logs — enforced here as a safety guard.
     */
    @Transactional
    public void attachRejectedImage(UUID logId, InputStream encryptedStream) throws IOException {
        PpeVerificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("Verification log not found: " + logId));

        if (!"REJECTED".equals(log.getStatus())) {
            throw new IllegalArgumentException("Images may only be attached to REJECTED logs");
        }
        RejectedImage image = new RejectedImage();
        image.setVerificationLog(log);
        image.setEncryptedData(encryptedStream.readAllBytes());
        image.setExpiresAt(log.getCapturedAt().plusHours(48));
        imageRepository.save(image);
    }

    /** Supervisor certifies a VALIDATED log — stamps certifiedAt. */
    @Transactional
    public void certify(UUID logId) {
        PpeVerificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("Verification log not found: " + logId));
        if (!"VALIDATED".equals(log.getStatus())) {
            throw new IllegalArgumentException("Only VALIDATED logs can be certified");
        }
        log.setCertifiedAt(LocalDateTime.now());
        logRepository.save(log);
    }

    /** Batch sync for offline results — idempotent (duplicate logIds are ignored). */
    @Transactional
    public void syncOffline(List<PpeVerificationRequest> pending) {
        pending.forEach(this::submit);
    }

    // --- Backoffice queries ---

    public Page<VerificationLogResponse> getLogs(LocalDateTime from, LocalDateTime to, Integer siteId, String intent, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("capturedAt").ascending());
        Page<PpeVerificationLog> logs;
        if (siteId != null && intent != null) {
            logs = logRepository.findBySite_SiteIdAndIntentAndCapturedAtBetween(siteId, intent, from, to, pageable);
        } else if (siteId != null) {
            logs = logRepository.findBySite_SiteIdAndCapturedAtBetween(siteId, from, to, pageable);
        } else if (intent != null) {
            logs = logRepository.findByIntentAndCapturedAtBetween(intent, from, to, pageable);
        } else {
            logs = logRepository.findByCapturedAtBetween(from, to, pageable);
        }
        return logs.map(this::toResponse);
    }

    /** Returns all logs matching the filters as a flat list — used for PDF export. */
    public List<VerificationLogResponse> getAllLogsForExport(LocalDateTime from, LocalDateTime to,
                                                              Integer siteId, String intent) {
        int totalPages = 1;
        int page = 0;
        java.util.List<VerificationLogResponse> all = new java.util.ArrayList<>();
        do {
            org.springframework.data.domain.Page<VerificationLogResponse> batch =
                    getLogs(from, to, siteId, intent, page, 200);
            all.addAll(batch.getContent());
            totalPages = batch.getTotalPages();
            page++;
        } while (page < totalPages);
        return all;
    }

    public List<MissingPpeStatDto> getMissingPpeStats(LocalDateTime from, LocalDateTime to, Integer siteId) {
        return resultRepository.findTopMissingPpe(from, to, siteId).stream()
                .map(row -> new MissingPpeStatDto((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
    }

    public List<TopWorkerStatDto> getTopWorkers(LocalDateTime from, LocalDateTime to,
                                                  Integer siteId, String intent, int limit) {
        return logRepository.findTopWorkers(from, to, siteId, intent)
                .stream()
                .limit(limit)
                .map(row -> new TopWorkerStatDto((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
    }

    public VerificationLogResponse toResponse(PpeVerificationLog log) {
        List<VerificationLogResponse.ItemResultDto> items = log.getItemResults() == null ? List.of() :
                log.getItemResults().stream()
                    .map(r -> new VerificationLogResponse.ItemResultDto(
                        r.getPpeItem().getCode(),
                        r.getPpeItem().getLabel(),
                        r.isDetected(),
                        r.getConfidence()))
                    .toList();

        VerificationLogResponse.InductionDto inductionDto = new VerificationLogResponse.InductionDto(
                log.getInduction().getInductionId(),
                log.getInduction().getFirstName(),
                log.getInduction().getLastName());

        return VerificationLogResponse.builder()
                .logId(log.getLogId())
                .induction(inductionDto)
                .intent(log.getIntent())
                .siteId(log.getSite().getSiteId())
                .siteName(log.getSite().getName())
                .status(log.getStatus())
                .capturedAt(log.getCapturedAt())
                .offline(log.isOffline())
                .syncedAt(log.getSyncedAt())
                .itemResults(items)
                .rejectionCauses(log.getRejectionCauses())
                .certifiedAt(log.getCertifiedAt())
                .build();
    }
}
