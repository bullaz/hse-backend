package com.stellarix.hse.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
import com.stellarix.hse.entity.VerificationCompositeImage;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PpeItemRepository;
import com.stellarix.hse.repository.PpeItemResultRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;
import com.stellarix.hse.repository.RejectedImageRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.VerificationCompositeImageRepository;
import com.stellarix.hse.repository.TravauxRepository;
import com.stellarix.hse.repository.WorkPermitRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
    private final VerificationCompositeImageRepository compositeImageRepository;
    private final SiteRepository siteRepository;
    private final HseInductionRepository inductionRepository;
    private final WorkPermitRepository workPermitRepository;
    private final TravauxRepository travauxRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

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
        log.setDescription(request.getDescription());

        // Link to Travaux dossier if provided (takes priority over auto-linking a personal permit)
        if (request.getTravauxId() != null) {
            travauxRepository.findById(request.getTravauxId()).ifPresent(log::setTravaux);
        } else if ("WORK".equals(request.getIntent())) {
            // Legacy: auto-link active personal work permit for WORK intent submissions
            List<WorkPermit> activePermits = workPermitRepository.findActiveByInductionAndSite(
                    induction.getInductionId(), site.getSiteId(), LocalDateTime.now());
            if (!activePermits.isEmpty()) {
                log.setPermit(activePermits.get(0));
            }
        }

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
        saved.setItemResults(new java.util.ArrayList<>(results)); // populate in-memory so WebSocket broadcast has full data

        if ("REJECTED".equals(request.getStatus())) {
            List<String> causes = request.getItemResults().stream()
                    .filter(r -> !r.isDetected())
                    .map(r -> (r.isWrongType() ? "WRONG_TYPE:" : "MISSING_PPE:") + r.getPpeItemCode())
                    .toList();
            saved.setRejectionCauses(causes);
            logRepository.save(saved);
        }

        // Broadcast new log to all subscribed frontend clients (real-time update)
        messagingTemplate.convertAndSend("/topic/verifications", toResponse(saved));

        return saved;
    }

    /**
     * Saves composite image for any verification log.
     * GDPR: VALIDATED logs store annotated-only (original never persisted).
     * REJECTED logs store side-by-side composite (original | annotated) for supervisor review.
     */
    @Transactional
    public void saveCompositeImage(UUID logId, byte[] originalBytes, byte[] annotatedBytes) throws IOException {
        PpeVerificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("Verification log not found: " + logId));

        byte[] imageData = "VALIDATED".equals(log.getStatus())
                ? annotatedBytes
                : buildSideBySide(originalBytes, annotatedBytes);

        VerificationCompositeImage img = new VerificationCompositeImage();
        img.setLogId(logId);
        img.setVerificationLog(log);
        img.setImageData(imageData);
        img.setExpiresAt(log.getCapturedAt().plusHours(48));
        entityManager.persist(img);
        entityManager.flush();

        // Re-broadcast so frontend updates hasCompositeImage in real-time
        messagingTemplate.convertAndSend("/topic/verifications", toResponse(log, Set.of(logId)));
    }

    /** Returns the composite image bytes for a log, or empty if not yet uploaded. */
    public byte[] getCompositeImage(UUID logId) {
        return compositeImageRepository.findById(logId)
                .map(VerificationCompositeImage::getImageData)
                .orElseThrow(() -> new EntityNotFoundException("No composite image for log: " + logId));
    }

    public boolean hasCompositeImage(UUID logId) {
        return compositeImageRepository.existsByVerificationLog_LogId(logId);
    }

    /** Purges expired composite images every hour. */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void purgeExpiredComposites() {
        int deleted = compositeImageRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) log.info("Purged {} expired composite images", deleted);
    }

    private byte[] buildSideBySide(byte[] leftBytes, byte[] rightBytes) throws IOException {
        System.setProperty("java.awt.headless", "true");
        BufferedImage left = ImageIO.read(new ByteArrayInputStream(leftBytes));
        BufferedImage right = ImageIO.read(new ByteArrayInputStream(rightBytes));
        if (left == null || right == null) throw new IOException("Could not decode one or both images");

        // Normalize right image height to match left
        int h = left.getHeight();
        int rw = (int) ((double) right.getWidth() / right.getHeight() * h);
        BufferedImage scaledRight = new BufferedImage(rw, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = scaledRight.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gr.drawImage(right, 0, 0, rw, h, null);
        gr.dispose();

        final int gap = 4;
        BufferedImage composite = new BufferedImage(left.getWidth() + gap + rw, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gc = composite.createGraphics();
        gc.setColor(java.awt.Color.DARK_GRAY);
        gc.fillRect(0, 0, composite.getWidth(), h);
        gc.drawImage(left, 0, 0, null);
        gc.drawImage(scaledRight, left.getWidth() + gap, 0, null);
        gc.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(composite, "jpg", out);
        return out.toByteArray();
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
        // Single query for composite image flags (avoids N+1)
        java.util.Set<UUID> withImage = compositeImageRepository.findExistingLogIds(
                logs.stream().map(PpeVerificationLog::getLogId).toList());
        return logs.map(l -> toResponse(l, withImage));
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

    /** Used for single-log responses (submit, WebSocket broadcast). */
    public VerificationLogResponse toResponse(PpeVerificationLog log) {
        return toResponse(log, null);
    }

    /** Batch-aware variant: pass a pre-fetched set to avoid N+1 queries. */
    public VerificationLogResponse toResponse(PpeVerificationLog log, java.util.Set<UUID> withImageIds) {
        List<VerificationLogResponse.ItemResultDto> items = log.getItemResults() == null ? List.of() :
                log.getItemResults().stream()
                    .map(r -> new VerificationLogResponse.ItemResultDto(
                        r.getPpeItem().getCode(),
                        r.getPpeItem().getLabel(),
                        r.isDetected(),
                        r.getConfidence()))
                    .toList();

        String companyName = log.getInduction().getCompany() != null
                ? log.getInduction().getCompany().getName() : null;
        VerificationLogResponse.InductionDto inductionDto = new VerificationLogResponse.InductionDto(
                log.getInduction().getInductionId(),
                log.getInduction().getFirstName(),
                log.getInduction().getLastName(),
                companyName,
                log.getInduction().getWork());

        String permitId = log.getPermit() != null ? log.getPermit().getPermitId() : null;
        String permitTypeLabel = (log.getPermit() != null && log.getPermit().getPermitType() != null)
                ? log.getPermit().getPermitType().getLabel() : null;

        boolean hasImage = withImageIds != null
                ? withImageIds.contains(log.getLogId())
                : compositeImageRepository.existsByVerificationLog_LogId(log.getLogId());

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
                .permitId(permitId)
                .permitTypeLabel(permitTypeLabel)
                .description(log.getDescription())
                .hasCompositeImage(hasImage)
                .build();
    }
}
