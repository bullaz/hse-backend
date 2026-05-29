package com.stellarix.hse.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stellarix.hse.dto.PageResponse;
import com.stellarix.hse.dto.TravauxClosureRequest;
import com.stellarix.hse.dto.TravauxEntryVerifyResponse;
import com.stellarix.hse.dto.TravauxRequest;
import com.stellarix.hse.dto.TravauxResponse;
import com.stellarix.hse.entity.Company;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.PermitType;
import com.stellarix.hse.entity.Site;
import com.stellarix.hse.entity.Travaux;
import com.stellarix.hse.entity.TravauxClosureForm;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.CompanyRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PermitTypeRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.TravauxClosureFormRepository;
import com.stellarix.hse.repository.TravauxRepository;
import com.stellarix.hse.repository.WorkPermitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravauxService {

    private final TravauxRepository travauxRepository;
    private final TravauxClosureFormRepository closureFormRepository;
    private final SiteRepository siteRepository;
    private final CompanyRepository companyRepository;
    private final HseInductionRepository inductionRepository;
    private final WorkPermitRepository workPermitRepository;
    private final PermitTypeRepository permitTypeRepository;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TravauxResponse create(TravauxRequest request, MultipartFile modop) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("Company not found: " + request.getCompanyId()));
        }

        List<HseInduction> intervenants = List.of();
        if (request.getIntervenantIds() != null && !request.getIntervenantIds().isEmpty()) {
            intervenants = inductionRepository.findAllById(request.getIntervenantIds());
            if (intervenants.size() != request.getIntervenantIds().size()) {
                throw new IllegalArgumentException("One or more induction IDs not found");
            }
        }

        Travaux travaux = new Travaux();
        travaux.setAccessType(request.getAccessType() != null ? request.getAccessType() : "TRAVAUX");
        travaux.setTicketNo(request.getTicketNo());
        travaux.setCompany(company);
        travaux.setSite(site);
        travaux.setObjetVisite(request.getObjetVisite());
        travaux.setDescription(request.getDescription());
        travaux.setDateDebut(request.getDateDebut());
        travaux.setDateFin(request.getDateFin());
        travaux.setSuperviseurNom(request.getSuperviseurNom());
        travaux.setSuperviseurEmail(request.getSuperviseurEmail());
        travaux.setImpactServices(request.isImpactServices());
        travaux.setImpactTypes(request.getImpactTypes() != null ? request.getImpactTypes() : List.of());
        travaux.setIntervenants(new java.util.ArrayList<>(intervenants));

        if (modop != null && !modop.isEmpty()) {
            try {
                travaux.setModopFileName(modop.getOriginalFilename());
                travaux.setModopFileData(modop.getBytes());
                travaux.setModopContentType(modop.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read MODOP file", e);
            }
        }

        return toSummaryResponse(travauxRepository.save(travaux));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public PageResponse<TravauxResponse> getAll(String status, Integer siteId, String ticketNo, int page, int size) {
        Page<Travaux> results = travauxRepository.findWithFilters(
                status != null && !status.isBlank() ? status : null,
                siteId,
                ticketNo != null && !ticketNo.isBlank() ? ticketNo : null,
                PageRequest.of(page, size));
        return new PageResponse<>(results.map(this::toSummaryResponse));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TravauxResponse getDetail(UUID id) {
        Travaux t = findById(id);
        return toDetailResponse(t);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TravauxResponse update(UUID id, TravauxRequest request, MultipartFile modop) {
        Travaux travaux = findById(id);
        if (!"DRAFT".equals(travaux.getStatus())) {
            throw new IllegalStateException("Only DRAFT travaux can be edited");
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("Company not found: " + request.getCompanyId()));
        }

        List<HseInduction> intervenants = List.of();
        if (request.getIntervenantIds() != null && !request.getIntervenantIds().isEmpty()) {
            intervenants = inductionRepository.findAllById(request.getIntervenantIds());
        }

        travaux.setAccessType(request.getAccessType() != null ? request.getAccessType() : travaux.getAccessType());
        travaux.setTicketNo(request.getTicketNo());
        travaux.setCompany(company);
        travaux.setSite(site);
        travaux.setObjetVisite(request.getObjetVisite());
        travaux.setDescription(request.getDescription());
        travaux.setDateDebut(request.getDateDebut());
        travaux.setDateFin(request.getDateFin());
        travaux.setSuperviseurNom(request.getSuperviseurNom());
        travaux.setSuperviseurEmail(request.getSuperviseurEmail());
        travaux.setImpactServices(request.isImpactServices());
        travaux.setImpactTypes(request.getImpactTypes() != null ? request.getImpactTypes() : List.of());
        travaux.setIntervenants(new java.util.ArrayList<>(intervenants));

        if (modop != null && !modop.isEmpty()) {
            try {
                travaux.setModopFileName(modop.getOriginalFilename());
                travaux.setModopFileData(modop.getBytes());
                travaux.setModopContentType(modop.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read MODOP file", e);
            }
        }

        return toSummaryResponse(travauxRepository.save(travaux));
    }

    // ── Add permit to travaux ─────────────────────────────────────────────────

    @Transactional
    public void addPermit(UUID travauxId, String permitId) {
        Travaux travaux = findById(travauxId);
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Cannot add permit to a closed travaux");
        }
        WorkPermit permit = workPermitRepository.findById(permitId)
                .orElseThrow(() -> new EntityNotFoundException("Work permit not found: " + permitId));
        permit.setTravaux(travaux);
        workPermitRepository.save(permit);
    }

    // ── Send permits to supervisor ────────────────────────────────────────────

    @Transactional
    public void sendPermits(UUID id) {
        Travaux travaux = findById(id);
        if ("VISITE".equals(travaux.getAccessType())) {
            throw new IllegalStateException("VISITE dossiers do not require permits");
        }
        if (!"DRAFT".equals(travaux.getStatus()) && !"PERMITS_READY".equals(travaux.getStatus())) {
            throw new IllegalStateException("Cannot send permits for travaux in status: " + travaux.getStatus());
        }

        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(id);
        if (permits.isEmpty()) {
            throw new IllegalStateException("No permits attached to this travaux");
        }

        String token = UUID.randomUUID().toString();
        travaux.setClosureToken(token);
        travaux.setClosureTokenUsed(false);
        travaux.setStatus("PERMITS_READY");
        travauxRepository.save(travaux);

        String closureUrl = frontendUrl.replaceAll("/$", "") + "/close-work?token=" + token;
        emailService.sendTravauxPermitsEmail(travaux, permits, closureUrl);
        log.info("Permits sent to {} for travaux {}", travaux.getSuperviseurEmail(), id);
    }

    // ── Activate ──────────────────────────────────────────────────────────────

    @Transactional
    public void activate(UUID id) {
        Travaux travaux = findById(id);
        if (!"PERMITS_READY".equals(travaux.getStatus())) {
            throw new IllegalStateException("Travaux must be in PERMITS_READY status to activate");
        }
        travaux.setStatus("ACTIVE");
        travauxRepository.save(travaux);
    }

    // ── Supervisor closure (public, token-auth) ────────────────────────────────

    // ── Direct closure for VISITE (no form, HSE clicks close) ────────────────

    @Transactional
    public void closeVisite(UUID id) {
        Travaux travaux = findById(id);
        if (!"VISITE".equals(travaux.getAccessType())) {
            throw new IllegalStateException("Use validate-closure workflow for TRAVAUX dossiers");
        }
        if (!"ACTIVE".equals(travaux.getStatus()) && !"DRAFT".equals(travaux.getStatus())
                && !"PERMITS_READY".equals(travaux.getStatus())) {
            throw new IllegalStateException("Cannot close dossier in status: " + travaux.getStatus());
        }
        travaux.setStatus("CLOSED");
        travaux.setClosedAt(LocalDateTime.now());
        travauxRepository.save(travaux);
    }

    // ── Supervisor closure (TRAVAUX only, public, token-auth) ─────────────────

    @Transactional
    public void supervisorClose(TravauxClosureRequest request) {
        Travaux travaux = travauxRepository.findByClosureToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));

        if ("VISITE".equals(travaux.getAccessType())) {
            throw new IllegalStateException("VISITE dossiers do not use supervisor closure");
        }
        if (travaux.isClosureTokenUsed()) {
            throw new IllegalStateException("TOKEN_ALREADY_USED");
        }
        if (!"ACTIVE".equals(travaux.getStatus())) {
            throw new IllegalStateException("TRAVAUX_NOT_ACTIVE");
        }

        TravauxClosureForm form = new TravauxClosureForm();
        form.setTravaux(travaux);
        form.setChantierPropre(request.isChantierPropre());
        form.setDangerResiduel(request.isDangerResiduel());
        form.setObservations(request.getObservations());
        form.setSignatureData(request.getSignatureData());
        closureFormRepository.save(form);

        travaux.setClosureTokenUsed(true);
        travaux.setStatus("PENDING_CLOSURE");
        travauxRepository.save(travaux);

        emailService.sendClosureRequestNotification(travaux);
        log.info("Closure requested by supervisor for travaux {} (ticket {})", travaux.getTravauxId(), travaux.getTicketNo());
    }

    // ── Validate closure ──────────────────────────────────────────────────────

    @Transactional
    public void validateClosure(UUID id) {
        Travaux travaux = findById(id);
        if (!"PENDING_CLOSURE".equals(travaux.getStatus())) {
            throw new IllegalStateException("Travaux must be in PENDING_CLOSURE status");
        }
        travaux.setStatus("CLOSED");
        travaux.setClosedAt(LocalDateTime.now());
        travauxRepository.save(travaux);
    }

    // ── Reject closure ────────────────────────────────────────────────────────

    @Transactional
    public void rejectClosure(UUID id) {
        Travaux travaux = findById(id);
        if (!"PENDING_CLOSURE".equals(travaux.getStatus())) {
            throw new IllegalStateException("Travaux must be in PENDING_CLOSURE status");
        }

        // Remove the old closure form and clear the reference before saving
        if (travaux.getClosureForm() != null) {
            TravauxClosureForm form = travaux.getClosureForm();
            travaux.setClosureForm(null);
            closureFormRepository.delete(form);
        }

        // Generate a new token and re-send email
        String newToken = UUID.randomUUID().toString();
        travaux.setClosureToken(newToken);
        travaux.setClosureTokenUsed(false);
        travaux.setStatus("ACTIVE");
        travauxRepository.save(travaux);

        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(id);
        String closureUrl = frontendUrl.replaceAll("/$", "") + "/close-work?token=" + newToken;
        emailService.sendTravauxPermitsEmail(travaux, permits, closureUrl);
        log.info("Closure rejected, new token sent to {} for travaux {}", travaux.getSuperviseurEmail(), id);
    }

    // ── Get travaux by token (public — supervisor page load) ──────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TravauxResponse getByToken(String token) {
        Travaux travaux = travauxRepository.findByClosureToken(token)
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));
        return toDetailResponse(travaux);
    }

    // ── Create permit inside a Travaux ────────────────────────────────────────

    @Transactional
    public void createPermitForTravaux(UUID travauxId, Integer permitTypeId,
            String description, MultipartFile file) {
        Travaux travaux = findById(travauxId);
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Cannot add permit to a closed travaux");
        }

        PermitType permitType = null;
        if (permitTypeId != null) {
            permitType = permitTypeRepository.findById(permitTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("Permit type not found: " + permitTypeId));
        }

        WorkPermit permit = new WorkPermit();
        permit.setPermitId(generatePermitId());
        permit.setSite(travaux.getSite());
        permit.setTravaux(travaux);
        permit.setDescription(description);
        permit.setPermitType(permitType);
        permit.setStartDatetime(travaux.getDateDebut());
        permit.setEndDatetime(travaux.getDateFin());

        if (file != null && !file.isEmpty()) {
            try {
                permit.setPermitFileName(file.getOriginalFilename());
                permit.setPermitFileData(file.getBytes());
                permit.setPermitFileContentType(file.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read permit file", e);
            }
        }

        workPermitRepository.save(permit);
    }

    private String generatePermitId() {
        String id;
        do { id = "STX-" + rand(3) + "-" + rand(3) + "-" + rand(4) + "-" + rand(3); }
        while (workPermitRepository.existsById(id));
        return id;
    }

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private String rand(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    // ── Verify ticket + name for mobile entry (public endpoint) ──────────────

    public TravauxEntryVerifyResponse verifyEntry(String ticketNo, String firstName, String lastName) {
        Travaux travaux = travauxRepository
                .findByTicketNoIgnoreCaseAndStatus(ticketNo.trim(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("NO_ACTIVE_DOSSIER_FOR_TICKET"));

        // Find the matching induction in the intervenants list
        var matching = travaux.getIntervenants().stream()
                .filter(i -> i.getFirstName().equalsIgnoreCase(firstName.trim())
                        && i.getLastName().equalsIgnoreCase(lastName.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("NOT_IN_DOSSIER_INTERVENANTS"));

        // Check habilitations required by the site's zone type
        List<String> missing = List.of();
        if (travaux.getSite().getZoneType() != null) {
            missing = travaux.getSite().getZoneType().getHabilitations().stream()
                    .filter(h -> matching.getHabilitations().stream()
                            .noneMatch(ih -> ih.getHabilitationId().equals(h.getHabilitationId())))
                    .map(h -> h.getCode())
                    .toList();
        }

        String intent = "VISITE".equals(travaux.getAccessType()) ? "VISIT" : "WORK";

        return TravauxEntryVerifyResponse.builder()
                .travauxId(travaux.getTravauxId())
                .inductionId(matching.getInductionId())
                .intent(intent)
                .siteId(travaux.getSite().getSiteId())
                .siteName(travaux.getSite().getName())
                .missingHabilitations(missing)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Travaux findById(UUID id) {
        return travauxRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travaux not found: " + id));
    }

    private TravauxResponse toSummaryResponse(Travaux t) {
        return TravauxResponse.builder()
                .travauxId(t.getTravauxId())
                .accessType(t.getAccessType())
                .ticketNo(t.getTicketNo())
                .companyName(t.getCompany() != null ? t.getCompany().getName() : null)
                .siteId(t.getSite().getSiteId())
                .siteName(t.getSite().getName())
                .objetVisite(t.getObjetVisite())
                .description(t.getDescription())
                .dateDebut(t.getDateDebut())
                .dateFin(t.getDateFin())
                .superviseurNom(t.getSuperviseurNom())
                .superviseurEmail(t.getSuperviseurEmail())
                .impactServices(t.isImpactServices())
                .impactTypes(t.getImpactTypes())
                .hasModop(t.getModopFileData() != null)
                .status(t.getStatus())
                .permitCount((int) workPermitRepository.countByTravauxTravauxId(t.getTravauxId()))
                .intervenantCount(t.getIntervenants().size())
                .createdAt(t.getCreatedAt())
                .closedAt(t.getClosedAt())
                .build();
    }

    private TravauxResponse toDetailResponse(Travaux t) {
        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(t.getTravauxId());

        List<TravauxResponse.IntervenantDto> intervenantDtos = t.getIntervenants().stream()
                .map(i -> TravauxResponse.IntervenantDto.builder()
                        .inductionId(i.getInductionId())
                        .firstName(i.getFirstName())
                        .lastName(i.getLastName())
                        .email(i.getEmail())
                        .work(i.getWork())
                        .build())
                .toList();

        List<TravauxResponse.PermitSummaryDto> permitDtos = permits.stream()
                .map(p -> TravauxResponse.PermitSummaryDto.builder()
                        .permitId(p.getPermitId())
                        .permitTypeLabel(p.getPermitType() != null ? p.getPermitType().getLabel() : null)
                        .description(p.getDescription())
                        .status(p.getStatus())
                        .hasFile(p.getPermitFileData() != null)
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        List<TravauxResponse.EntryLogDto> logDtos = t.getEntryLogs().stream()
                .map(l -> TravauxResponse.EntryLogDto.builder()
                        .logId(l.getLogId())
                        .firstName(l.getInduction().getFirstName())
                        .lastName(l.getInduction().getLastName())
                        .intent(l.getIntent())
                        .status(l.getStatus())
                        .capturedAt(l.getCapturedAt())
                        .offline(l.isOffline())
                        .build())
                .toList();

        TravauxResponse.ClosureFormDto closureDto = null;
        TravauxClosureForm form = t.getClosureForm();
        if (form != null) {
            closureDto = TravauxResponse.ClosureFormDto.builder()
                    .closureId(form.getClosureId())
                    .chantierPropre(form.isChantierPropre())
                    .dangerResiduel(form.isDangerResiduel())
                    .observations(form.getObservations())
                    .signatureData(form.getSignatureData())
                    .submittedAt(form.getSubmittedAt())
                    .build();
        }

        TravauxResponse base = toSummaryResponse(t);
        base.setIntervenants(intervenantDtos);
        base.setPermits(permitDtos);
        base.setEntryLogs(logDtos);
        base.setClosureForm(closureDto);
        return base;
    }
}
