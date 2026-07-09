package com.stellarix.hse.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.WorkPermitResponse;
import com.stellarix.hse.dto.WorkPermitVerifyResponse;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.WorkPermitRepository;
import com.stellarix.hse.utils.Utils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkPermitService {

    private final WorkPermitRepository repository;
    private final EmailService emailService;

    public PageResponse<WorkPermitResponse> getAll(String name, Integer siteId, String status,
                                                    String dateFrom, String dateTo, int page, int size) {
        Page<WorkPermit> results = repository.findWithFilters(
                name != null && !name.isBlank() ? name : null,
                siteId,
                status,
                dateFrom,
                dateTo,
                PageRequest.of(page, size));
        return new PageResponse<>(results.map(this::toResponse));
    }

    @Transactional
    public void revoke(String permitId) {
        WorkPermit permit = repository.findById(permitId)
                .orElseThrow(() -> new EntityNotFoundException("Permis de travail introuvable : " + permitId));
        permit.setStatus("REVOKED");
        repository.save(permit);
    }

    @Transactional
    public void attachPermitFile(String permitId, String fileName, String contentType, byte[] data) {
        WorkPermit permit = findById(permitId);
        permit.setPermitFileName(fileName);
        permit.setPermitFileData(data);
        permit.setPermitFileContentType(contentType != null ? contentType : "application/octet-stream");
        repository.save(permit);
        emailService.sendPermitEmailToAll(permit, Utils.currentUserEmail());
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
                .orElseThrow(() -> new EntityNotFoundException("Permis de travail introuvable : " + permitId));
    }

    // --- Mapping helpers ---

    public WorkPermitResponse toResponse(WorkPermit permit) {
        String computedStatus = "ACTIVE".equals(permit.getStatus())
                && LocalDateTime.now().isAfter(permit.getEndDatetime())
                ? "EXPIRED" : permit.getStatus();

        WorkPermitResponse.PersonDto person = permit.getInduction() != null
                ? new WorkPermitResponse.PersonDto(
                        permit.getInduction().getInductionId(),
                        permit.getInduction().getFirstName(),
                        permit.getInduction().getLastName(),
                        permit.getInduction().getEmail(),
                        permit.getInduction().getPhone())
                : null;

        List<WorkPermitResponse.PersonDto> intervenants = permit.getIntervenants().stream()
                .map(i -> new WorkPermitResponse.PersonDto(i.getInductionId(), i.getFirstName(),
                        i.getLastName(), i.getEmail(), i.getPhone()))
                .toList();

        String zoneTypeLabel = permit.getSite().getZoneType() != null
                ? permit.getSite().getZoneType().getLabel() : null;
        WorkPermitResponse.SiteDto site = new WorkPermitResponse.SiteDto(
                permit.getSite().getSiteId(),
                permit.getSite().getName(),
                zoneTypeLabel);

        WorkPermitResponse.PermitTypeDto permitTypeDto = null;
        if (permit.getPermitType() != null) {
            permitTypeDto = new WorkPermitResponse.PermitTypeDto(
                    permit.getPermitType().getPermitTypeId(),
                    permit.getPermitType().getCode(),
                    permit.getPermitType().getLabel());
        }

        return WorkPermitResponse.builder()
                .permitId(permit.getPermitId())
                .person(person)
                .intervenants(intervenants)
                .site(site)
                .description(permit.getDescription())
                .permitType(permitTypeDto)
                .startDatetime(permit.getStartDatetime())
                .endDatetime(permit.getEndDatetime())
                .status(computedStatus)
                .hasFile(permit.getPermitFileData() != null)
                .createdAt(permit.getCreatedAt())
                .build();
    }

    private WorkPermitVerifyResponse fromPermit(WorkPermit permit, boolean valid, String reason) {
        List<WorkPermitVerifyResponse.IntervenantDto> intervenants = permit.getIntervenants().stream()
                .map(i -> new WorkPermitVerifyResponse.IntervenantDto(i.getInductionId(), i.getFirstName(), i.getLastName()))
                .toList();
        return WorkPermitVerifyResponse.builder()
                .valid(valid)
                .reason(reason)
                .permitId(permit.getPermitId())
                .siteName(permit.getSite().getName())
                .startDatetime(permit.getStartDatetime())
                .endDatetime(permit.getEndDatetime())
                .intervenants(intervenants)
                .build();
    }

}
