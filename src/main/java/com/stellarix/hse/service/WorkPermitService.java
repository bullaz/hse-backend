package com.stellarix.hse.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.WorkPermitRequest;
import com.stellarix.hse.dto.WorkPermitResponse;
import com.stellarix.hse.dto.WorkPermitVerifyResponse;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.WorkPermitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkPermitService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkPermitRepository repository;
    private final HseInductionRepository inductionRepository;
    private final SiteRepository siteRepository;
    private final EmailService emailService;

    @Transactional
    public WorkPermitResponse create(WorkPermitRequest request) {
        HseInduction induction = inductionRepository.findById(request.getInductionId())
                .orElseThrow(() -> new EntityNotFoundException("Induction not found: " + request.getInductionId()));

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        List<String> missingHabilitations = site.getHabilitations().stream()
                .filter(h -> induction.getHabilitations().stream()
                        .noneMatch(ih -> ih.getHabilitationId().equals(h.getHabilitationId())))
                .map(h -> h.getCode())
                .toList();

        WorkPermit permit = new WorkPermit();
        permit.setPermitId(generateUniquePermitId());
        permit.setInduction(induction);
        permit.setSite(site);
        permit.setDescription(request.getDescription());
        permit.setStartDatetime(request.getStartDatetime());
        permit.setEndDatetime(request.getEndDatetime());
        WorkPermit saved = repository.save(permit);

        emailService.sendPermitEmail(saved);

        return toResponse(saved, missingHabilitations);
    }

    public PageResponse<WorkPermitResponse> getAll(String name, Integer siteId, String status, int page, int size) {
        Page<WorkPermit> results = repository.findWithFilters(
                name != null && !name.isBlank() ? name : null,
                siteId,
                status,
                PageRequest.of(page, size));
        return new PageResponse<>(results.map(w -> toResponse(w, null)));
    }

    @Transactional
    public void revoke(String permitId) {
        WorkPermit permit = repository.findById(permitId)
                .orElseThrow(() -> new EntityNotFoundException("Work permit not found: " + permitId));
        permit.setStatus("REVOKED");
        repository.save(permit);
    }

    /** Mobile endpoint — validates permit ID, site match, and time window (start−1h ≤ now ≤ end). */
    public WorkPermitVerifyResponse verify(String permitId, Integer siteId) {
        WorkPermit permit = repository.findById(permitId).orElse(null);
        if (permit == null) {
            return WorkPermitVerifyResponse.builder().valid(false).reason("INVALID_ID").build();
        }

        if ("REVOKED".equals(permit.getStatus())) {
            return fromPermit(permit, false, "REVOKED");
        }

        if (!permit.getSite().getSiteId().equals(siteId)) {
            return fromPermit(permit, false, "WRONG_SITE");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean withinWindow = !now.isBefore(permit.getStartDatetime().minusHours(1))
                && !now.isAfter(permit.getEndDatetime());

        if (!withinWindow) {
            String reason = now.isAfter(permit.getEndDatetime()) ? "EXPIRED" : "OUTSIDE_WINDOW";
            return fromPermit(permit, false, reason);
        }

        return fromPermit(permit, true, null);
    }

    public WorkPermit findById(String permitId) {
        return repository.findById(permitId)
                .orElseThrow(() -> new EntityNotFoundException("Work permit not found: " + permitId));
    }

    // --- Mapping helpers ---

    public WorkPermitResponse toResponse(WorkPermit permit, List<String> missingHabilitations) {
        String computedStatus = "ACTIVE".equals(permit.getStatus())
                && LocalDateTime.now().isAfter(permit.getEndDatetime())
                ? "EXPIRED" : permit.getStatus();

        WorkPermitResponse.PersonDto person = new WorkPermitResponse.PersonDto(
                permit.getInduction().getInductionId(),
                permit.getInduction().getFirstName(),
                permit.getInduction().getLastName(),
                permit.getInduction().getEmail());

        String zoneTypeLabel = permit.getSite().getZoneType() != null
                ? permit.getSite().getZoneType().getLabel() : null;
        WorkPermitResponse.SiteDto site = new WorkPermitResponse.SiteDto(
                permit.getSite().getSiteId(),
                permit.getSite().getName(),
                zoneTypeLabel);

        return WorkPermitResponse.builder()
                .permitId(permit.getPermitId())
                .person(person)
                .site(site)
                .description(permit.getDescription())
                .startDatetime(permit.getStartDatetime())
                .endDatetime(permit.getEndDatetime())
                .status(computedStatus)
                .createdAt(permit.getCreatedAt())
                .missingHabilitations(missingHabilitations)
                .build();
    }

    private WorkPermitVerifyResponse fromPermit(WorkPermit permit, boolean valid, String reason) {
        return WorkPermitVerifyResponse.builder()
                .valid(valid)
                .reason(reason)
                .permitId(permit.getPermitId())
                .inductionId(permit.getInduction().getInductionId())
                .firstName(permit.getInduction().getFirstName())
                .lastName(permit.getInduction().getLastName())
                .siteName(permit.getSite().getName())
                .startDatetime(permit.getStartDatetime())
                .endDatetime(permit.getEndDatetime())
                .build();
    }

    private String generateUniquePermitId() {
        String id;
        do { id = generatePermitId(); } while (repository.existsById(id));
        return id;
    }

    private String generatePermitId() {
        return "STX-" + rand(3) + "-" + rand(3) + "-" + rand(4) + "-" + rand(3);
    }

    private String rand(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
