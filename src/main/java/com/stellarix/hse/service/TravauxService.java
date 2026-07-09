package com.stellarix.hse.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import com.stellarix.hse.entity.TravauxIntervenant;
import com.stellarix.hse.entity.WorkPermit;
import com.stellarix.hse.repository.CompanyRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PermitTypeRepository;
import com.stellarix.hse.repository.SiteRepository;
import com.stellarix.hse.repository.TravauxClosureFormRepository;
import com.stellarix.hse.repository.TravauxRepository;
import com.stellarix.hse.repository.WorkPermitRepository;
import com.stellarix.hse.utils.Utils;

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
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : " + request.getSiteId()));

        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("L'entreprise est requise.");
        }
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable : " + request.getCompanyId()));

        if (request.getIntervenantIds() == null || request.getIntervenantIds().isEmpty()) {
            throw new IllegalArgumentException("Au moins un intervenant est requis.");
        }
        assertDatesNotInPast(request.getDateDebut(), request.getDateFin());
        assertTicketNotInUse(request.getTicketNo(), null);

        List<HseInduction> intervenants = inductionRepository.findAllById(request.getIntervenantIds());
        if (intervenants.size() != request.getIntervenantIds().size()) {
            throw new IllegalArgumentException("Une ou plusieurs personnes induites sont introuvables.");
        }
        intervenants.forEach(induction -> assertHabilitationsMet(site, induction));

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
            throw new IllegalStateException("Seules les demandes en brouillon peuvent être modifiées.");
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : " + request.getSiteId()));

        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("L'entreprise est requise.");
        }
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Entreprise introuvable : " + request.getCompanyId()));

        if (request.getIntervenantIds() == null || request.getIntervenantIds().isEmpty()) {
            throw new IllegalArgumentException("Au moins un intervenant est requis.");
        }
        assertDatesNotInPast(request.getDateDebut(), request.getDateFin());
        assertTicketNotInUse(request.getTicketNo(), id);

        List<HseInduction> intervenants = inductionRepository.findAllById(request.getIntervenantIds());
        if (intervenants.size() != request.getIntervenantIds().size()) {
            throw new IllegalArgumentException("Une ou plusieurs personnes induites sont introuvables.");
        }
        intervenants.forEach(induction -> assertHabilitationsMet(site, induction));

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

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Travaux travaux = findById(id);
        if (!"DRAFT".equals(travaux.getStatus())) {
            throw new IllegalStateException("Seules les demandes en brouillon peuvent être supprimées.");
        }
        // A DRAFT can already have permits attached (nothing blocks issuing one before
        // send-permits is clicked) — safe to wipe them too since none were ever emailed.
        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(id);
        if (!permits.isEmpty()) {
            workPermitRepository.deleteAll(permits);
        }
        travauxRepository.delete(travaux);
    }

    // ── Intervenant management (post-DRAFT) ──────────────────────────────────────
    // Add/suspend/reactivate work on a dossier in any status except CLOSED — unlike
    // update(), which only ever applies to DRAFT.

    @Transactional
    public TravauxResponse addIntervenant(UUID travauxId, UUID inductionId) {
        Travaux travaux = findById(travauxId);
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible de modifier les intervenants d'une demande clôturée.");
        }
        HseInduction induction = inductionRepository.findById(inductionId)
                .orElseThrow(() -> new EntityNotFoundException("Personne induite introuvable : " + inductionId));

        Optional<TravauxIntervenant> existing = travaux.getIntervenantLinks().stream()
                .filter(link -> link.getInduction().getInductionId().equals(inductionId))
                .findFirst();
        if (existing.isPresent()) {
            throw new IllegalStateException(existing.get().isSuspended()
                    ? "Cette personne est déjà intervenant sur cette demande (suspendu) — réactivez-la plutôt que de l'ajouter à nouveau."
                    : "Cette personne est déjà intervenant sur cette demande.");
        }

        assertHabilitationsMet(travaux.getSite(), induction);

        TravauxIntervenant link = new TravauxIntervenant();
        link.setTravaux(travaux);
        link.setInduction(induction);
        link.setSuspended(false);
        travaux.getIntervenantLinks().add(link);

        return toDetailResponse(travauxRepository.save(travaux));
    }

    @Transactional
    public TravauxResponse suspendIntervenant(UUID travauxId, UUID inductionId) {
        Travaux travaux = findById(travauxId);
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible de modifier les intervenants d'une demande clôturée.");
        }
        findIntervenantLink(travaux, inductionId).setSuspended(true);
        return toDetailResponse(travauxRepository.save(travaux));
    }

    @Transactional
    public TravauxResponse reactivateIntervenant(UUID travauxId, UUID inductionId) {
        Travaux travaux = findById(travauxId);
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible de modifier les intervenants d'une demande clôturée.");
        }
        TravauxIntervenant link = findIntervenantLink(travaux, inductionId);
        // Re-check — habilitations (theirs or the site's requirements) may have
        // changed while they were suspended.
        assertHabilitationsMet(travaux.getSite(), link.getInduction());
        link.setSuspended(false);
        return toDetailResponse(travauxRepository.save(travaux));
    }

    private TravauxIntervenant findIntervenantLink(Travaux travaux, UUID inductionId) {
        return travaux.getIntervenantLinks().stream()
                .filter(link -> link.getInduction().getInductionId().equals(inductionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Intervenant introuvable sur cette demande : " + inductionId));
    }

    // ── Send permits to supervisor ────────────────────────────────────────────

    @Transactional
    public void sendPermits(UUID id) {
        Travaux travaux = findById(id);
        if ("VISITE".equals(travaux.getAccessType())) {
            throw new IllegalStateException("Une demande de type visite ne nécessite pas de permis.");
        }
        if (!"DRAFT".equals(travaux.getStatus()) && !"PERMITS_READY".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible d'envoyer les permis pour une demande au statut : " + travaux.getStatus());
        }

        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(id);
        if (permits.isEmpty()) {
            throw new IllegalStateException("Aucun permis n'est rattaché à cette demande.");
        }

        String token = UUID.randomUUID().toString();
        travaux.setClosureToken(token);
        travaux.setClosureTokenUsed(false);
        travaux.setStatus("PERMITS_READY");
        travauxRepository.save(travaux);

        // Intervenants were already emailed their own permit at creation time
        // (createPermitForTravaux sends that immediately, regardless of DRAFT status) —
        // only the supervisor still needs the batch notification.
        String closureUrl = frontendUrl.replaceAll("/$", "") + "/travaux/cloture?token=" + token;
        emailService.sendTravauxPermitsEmail(travaux, permits, closureUrl, Utils.currentUserEmail())
                .thenAccept(success -> recordPermitEmailOutcome(id, success));
        log.info("Permits sent to supervisor {} for travaux {}", travaux.getSuperviseurEmail(), id);
    }

    // Called from the async email callback — status transitions already committed before
    // the send even started, so this is the only way HSE learns a dispatch actually failed.
    private void recordPermitEmailOutcome(UUID travauxId, boolean success) {
        travauxRepository.findById(travauxId).ifPresent(t -> {
            t.setPermitEmailFailed(!success);
            travauxRepository.save(t);
        });
    }

    // ── Activate ──────────────────────────────────────────────────────────────

    @Transactional
    public void activate(UUID id) {
        Travaux travaux = findById(id);
        // VISITE skips the permits step entirely (no permits to send), so it activates
        // straight from DRAFT. TRAVAUX still needs permits sent to the supervisor first.
        boolean visiteReady = "VISITE".equals(travaux.getAccessType()) && "DRAFT".equals(travaux.getStatus());
        boolean travauxReady = "TRAVAUX".equals(travaux.getAccessType()) && "PERMITS_READY".equals(travaux.getStatus());
        if (!visiteReady && !travauxReady) {
            throw new IllegalStateException("Une visite s'active depuis le statut brouillon ; une demande de travaux s'active depuis le statut permis prêts.");
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
            throw new IllegalStateException("Utilisez le processus de clôture avec validation pour les demandes de travaux.");
        }
        if (!"ACTIVE".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible de clôturer une demande au statut : " + travaux.getStatus());
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
            throw new IllegalStateException("La demande doit être en attente de clôture.");
        }
        closureFormRepository.findByTravaux_TravauxIdAndStatus(id, "PENDING")
                .ifPresent(form -> {
                    form.setStatus("ACCEPTED");
                    closureFormRepository.save(form);
                });
        travaux.setStatus("CLOSED");
        travaux.setClosedAt(LocalDateTime.now());
        travauxRepository.save(travaux);
    }

    // ── Reject closure ────────────────────────────────────────────────────────

    @Transactional
    public void rejectClosure(UUID id, String reason) {
        Travaux travaux = findById(id);
        if (!"PENDING_CLOSURE".equals(travaux.getStatus())) {
            throw new IllegalStateException("La demande doit être en attente de clôture.");
        }

        // Keep the submission on file (status=REJECTED + reason) instead of deleting it —
        // it's what the supervisor actually submitted and why HSE sent it back, both of
        // which belong in the audit trail.
        closureFormRepository.findByTravaux_TravauxIdAndStatus(id, "PENDING")
                .ifPresent(form -> {
                    form.setStatus("REJECTED");
                    form.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : null);
                    closureFormRepository.save(form);
                });

        // Generate a new token and notify the supervisor
        String newToken = UUID.randomUUID().toString();
        travaux.setClosureToken(newToken);
        travaux.setClosureTokenUsed(false);
        travaux.setStatus("ACTIVE");
        travauxRepository.save(travaux);

        String closureUrl = frontendUrl.replaceAll("/$", "") + "/travaux/cloture?token=" + newToken;
        emailService.sendClosureRejectedEmail(travaux, reason, closureUrl, Utils.currentUserEmail());
        log.info("Closure rejected, new token sent to {} for travaux {}", travaux.getSuperviseurEmail(), id);
    }

    // ── Get travaux by token (public — supervisor page load) ──────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TravauxResponse getByToken(String token) {
        Travaux travaux = travauxRepository.findByClosureToken(token)
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID"));
        if (travaux.isClosureTokenUsed()) {
            throw new IllegalArgumentException("TOKEN_ALREADY_USED");
        }
        return toDetailResponse(travaux);
    }

    // ── Create permit inside a Travaux ────────────────────────────────────────

    @Transactional
    public void createPermitForTravaux(UUID travauxId, Integer permitTypeId,
            String description, String startDatetime, String endDatetime,
            List<UUID> inductionIds, MultipartFile file) {
        Travaux travaux = findById(travauxId);
        if (!"TRAVAUX".equals(travaux.getAccessType())) {
            throw new IllegalArgumentException("Les permis ne peuvent être créés que pour une demande de travaux, pas une visite.");
        }
        if ("CLOSED".equals(travaux.getStatus())) {
            throw new IllegalStateException("Impossible d'ajouter un permis à une demande clôturée.");
        }

        if (permitTypeId == null) {
            throw new IllegalArgumentException("Le type de permis est requis.");
        }
        PermitType permitType = permitTypeRepository.findById(permitTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Type de permis introuvable : " + permitTypeId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier du permis est requis.");
        }

        // getIntervenants() already excludes suspended people — a suspended intervenant
        // can't be picked for a new permit without being reactivated first.
        List<HseInduction> intervenants;
        if (inductionIds == null || inductionIds.isEmpty()) {
            intervenants = new java.util.ArrayList<>(travaux.getIntervenants());
        } else {
            intervenants = travaux.getIntervenants().stream()
                    .filter(i -> inductionIds.contains(i.getInductionId()))
                    .collect(java.util.stream.Collectors.toList());
            if (intervenants.isEmpty()) {
                throw new IllegalArgumentException("Aucune des personnes indiquées ne fait partie des intervenants de cette demande.");
            }
        }
        intervenants.forEach(induction -> assertHabilitationsMet(travaux.getSite(), induction));

        WorkPermit permit = new WorkPermit();
        permit.setPermitId(generatePermitId());
        permit.setSite(travaux.getSite());
        permit.setTravaux(travaux);
        permit.setDescription(description);
        permit.setPermitType(permitType);
        java.time.format.DateTimeFormatter dtFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime permitStart = startDatetime != null && startDatetime.length() >= 16
                ? java.time.LocalDateTime.parse(startDatetime.substring(0, 16), dtFmt)
                : travaux.getDateDebut();
        LocalDateTime permitEnd = endDatetime != null && endDatetime.length() >= 16
                ? java.time.LocalDateTime.parse(endDatetime.substring(0, 16), dtFmt)
                : travaux.getDateFin();
        if (permitStart.isBefore(travaux.getDateDebut()) || permitEnd.isAfter(travaux.getDateFin())) {
            throw new IllegalArgumentException(
                    "La période du permis doit être comprise dans celle de la demande d'accès ("
                            + travaux.getDateDebut() + " – " + travaux.getDateFin() + ").");
        }
        permit.setStartDatetime(permitStart);
        permit.setEndDatetime(permitEnd);
        permit.setIntervenants(intervenants);

        try {
            permit.setPermitFileName(file.getOriginalFilename());
            permit.setPermitFileData(file.getBytes());
            permit.setPermitFileContentType(file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read permit file", e);
        }

        WorkPermit saved = workPermitRepository.save(permit);
        String hseSenderEmail = Utils.currentUserEmail();
        emailService.sendPermitEmailToAll(saved, hseSenderEmail);

        // Permits created while still DRAFT are bundled into the one batch email
        // sendPermits() sends when HSE clicks "Envoyer permis" — nothing to notify yet.
        // A permit added afterward (dossier already PERMITS_READY/ACTIVE/PENDING_CLOSURE)
        // has no future batch to ride along with, so the supervisor needs its own heads-up.
        if (!"DRAFT".equals(travaux.getStatus())) {
            String closureUrl = frontendUrl.replaceAll("/$", "") + "/travaux/cloture?token=" + travaux.getClosureToken();
            emailService.sendTravauxPermitsEmail(travaux, List.of(saved), closureUrl, hseSenderEmail);
        }
    }

    private String generatePermitId() {
        String id;
        do { id = "STX-PRMT-" + rand(4) + "-" + rand(4); }
        while (workPermitRepository.existsById(id));
        return id;
    }

    // No 0/O or 1/I — those are the pairs people actually confuse when reading a
    // permit ID aloud or copying it by hand.
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private String rand(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    // ── Verify ticket + name for mobile entry (public endpoint) ──────────────

    public TravauxEntryVerifyResponse verifyEntry(String ticketNo, String firstName, String lastName, String cin, Integer siteId, String permitId) {
        // "No such ticket" and "ticket exists but this name isn't on it" are deliberately
        // reported to this public, unauthenticated caller as the same generic reason —
        // distinguishing them lets someone probe ticket numbers and brute-force names
        // against a known ticket. The real reason is still logged server-side.
        Travaux travaux = travauxRepository
                .findByTicketNoIgnoreCaseAndStatus(ticketNo.trim(), "ACTIVE")
                .orElseThrow(() -> {
                    log.info("verify-entry: no active dossier for ticket {}", ticketNo);
                    return new IllegalArgumentException("ENTRY_NOT_RECOGNIZED");
                });

        // status=ACTIVE alone doesn't mean the dossier's own date range is still current —
        // it could be activated ahead of its start, or overrun (or HSE simply forgets to
        // close it) past its end, letting entries keep succeeding indefinitely otherwise.
        // Same generic reason as "no such ticket" above, for the same reason: don't let an
        // unauthenticated caller distinguish "wrong window" from "doesn't exist" and use
        // that to probe for real ticket numbers.
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(travaux.getDateDebut()) || now.isAfter(travaux.getDateFin())) {
            log.info("verify-entry: travaux {} is ACTIVE but outside its date range ({} – {})",
                    travaux.getTravauxId(), travaux.getDateDebut(), travaux.getDateFin());
            throw new IllegalArgumentException("ENTRY_NOT_RECOGNIZED");
        }

        if (siteId != null && !siteId.equals(travaux.getSite().getSiteId())) {
            throw new IllegalArgumentException("WRONG_SITE");
        }

        if ("TRAVAUX".equals(travaux.getAccessType()) && (permitId == null || permitId.isBlank())) {
            throw new IllegalArgumentException("PERMIT_REQUIRED");
        }

        var matching = travaux.getIntervenants().stream()
                .filter(i -> i.getFirstName().equalsIgnoreCase(firstName.trim())
                        && i.getLastName().equalsIgnoreCase(lastName.trim()))
                .findFirst()
                .orElseThrow(() -> {
                    log.info("verify-entry: {} {} not in intervenants for travaux {}", firstName, lastName, travaux.getTravauxId());
                    return new IllegalArgumentException("ENTRY_NOT_RECOGNIZED");
                });

        if (cin != null && !cin.isBlank() && matching.getCinNumber() != null
                && !matching.getCinNumber().equalsIgnoreCase(cin.trim())) {
            throw new IllegalArgumentException("CIN_MISMATCH");
        }

        List<String> missing = missingHabilitations(travaux.getSite(), matching);

        String intent = "VISITE".equals(travaux.getAccessType()) ? "VISIT" : "WORK";

        String resolvedPermitId = null;
        String permitDescription = null;
        if (permitId != null && !permitId.isBlank()) {
            WorkPermit permit = workPermitRepository.findById(permitId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("INVALID_PERMIT_ID"));

            if (!permit.getTravaux().getTravauxId().equals(travaux.getTravauxId())) {
                throw new IllegalArgumentException("PERMIT_WRONG_TICKET");
            }
            if (!"ACTIVE".equals(permit.getStatus())) {
                throw new IllegalArgumentException("PERMIT_NOT_ACTIVE");
            }
            if (now.isBefore(permit.getStartDatetime().minusHours(1)) || now.isAfter(permit.getEndDatetime())) {
                throw new IllegalArgumentException("PERMIT_OUTSIDE_WINDOW");
            }
            boolean workerInPermit = permit.getIntervenants().stream()
                    .anyMatch(i -> i.getInductionId().equals(matching.getInductionId()));
            if (!workerInPermit) {
                throw new IllegalArgumentException("WORKER_NOT_IN_PERMIT");
            }
            resolvedPermitId = permit.getPermitId();
            permitDescription = permit.getDescription();
        }

        return TravauxEntryVerifyResponse.builder()
                .travauxId(travaux.getTravauxId())
                .inductionId(matching.getInductionId())
                .intent(intent)
                .siteId(travaux.getSite().getSiteId())
                .siteName(travaux.getSite().getName())
                .missingHabilitations(missing)
                .permitId(resolvedPermitId)
                .permitDescription(permitDescription)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Travaux findById(UUID id) {
        return travauxRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande d'accès introuvable : " + id));
    }

    private void assertDatesNotInPast(LocalDateTime dateDebut, LocalDateTime dateFin) {
        LocalDateTime now = LocalDateTime.now();
        if (dateDebut != null && dateDebut.isBefore(now)) {
            throw new IllegalArgumentException("La date de début ne peut pas être dans le passé.");
        }
        if (dateFin != null && dateFin.isBefore(now)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être dans le passé.");
        }
    }

    /**
     * Ticket number is a permanent identifier, never reused — [excludeId] lets update()
     * compare against everyone else without tripping on the dossier's own row.
     */
    private void assertTicketNotInUse(String ticketNo, UUID excludeId) {
        boolean inUse = excludeId == null
                ? travauxRepository.existsByTicketNoIgnoreCase(ticketNo)
                : travauxRepository.existsByTicketNoIgnoreCaseAndTravauxIdNot(ticketNo, excludeId);
        if (inUse) {
            throw new IllegalStateException("Ce numéro de ticket est déjà utilisé par une autre demande d'accès.");
        }
    }

    /** Required habilitation codes for [site]'s zone type that [induction] doesn't hold. Empty = compliant. */
    private List<String> missingHabilitations(Site site, HseInduction induction) {
        if (site.getZoneType() == null) {
            return List.of();
        }
        return site.getZoneType().getHabilitations().stream()
                .filter(h -> induction.getHabilitations().stream()
                        .noneMatch(ih -> ih.getHabilitationId().equals(h.getHabilitationId())))
                .map(h -> h.getCode())
                .toList();
    }

    /**
     * Hard block: throws if [induction] is missing any habilitation [site]'s zone type
     * requires. Used everywhere an intervenant gets linked to a Travaux or a permit —
     * creation, edit, add-intervenant, and permit issuance all enforce the same rule,
     * rather than only checking going forward and grandfathering in whoever was added
     * before the check existed.
     */
    private void assertHabilitationsMet(Site site, HseInduction induction) {
        List<String> missing = missingHabilitations(site, induction);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    induction.getFirstName() + " " + induction.getLastName()
                            + " n'a pas les habilitations requises pour ce site : "
                            + String.join(", ", missing) + ".");
        }
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
                .permitEmailFailed(t.isPermitEmailFailed())
                .permitCount((int) workPermitRepository.countByTravauxTravauxId(t.getTravauxId()))
                .intervenantCount(t.getIntervenantLinks().size())
                .createdAt(t.getCreatedAt())
                .closedAt(t.getClosedAt())
                .build();
    }

    private TravauxResponse toDetailResponse(Travaux t) {
        List<WorkPermit> permits = workPermitRepository.findByTravauxTravauxId(t.getTravauxId());

        List<TravauxResponse.IntervenantDto> intervenantDtos = t.getIntervenantLinks().stream()
                .map(link -> TravauxResponse.IntervenantDto.builder()
                        .inductionId(link.getInduction().getInductionId())
                        .firstName(link.getInduction().getFirstName())
                        .lastName(link.getInduction().getLastName())
                        .email(link.getInduction().getEmail())
                        .work(link.getInduction().getWork())
                        .suspended(link.isSuspended())
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
                        .intervenants(p.getIntervenants().stream()
                                .map(i -> TravauxResponse.PermitIntervenantDto.builder()
                                        .inductionId(i.getInductionId())
                                        .firstName(i.getFirstName())
                                        .lastName(i.getLastName())
                                        .build())
                                .toList())
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

        List<TravauxResponse.ClosureFormDto> closureDtos =
                closureFormRepository.findByTravaux_TravauxIdOrderBySubmittedAtDesc(t.getTravauxId()).stream()
                        .map(form -> TravauxResponse.ClosureFormDto.builder()
                                .closureId(form.getClosureId())
                                .chantierPropre(form.isChantierPropre())
                                .dangerResiduel(form.isDangerResiduel())
                                .observations(form.getObservations())
                                .signatureData(form.getSignatureData())
                                .submittedAt(form.getSubmittedAt())
                                .status(form.getStatus())
                                .rejectionReason(form.getRejectionReason())
                                .build())
                        .toList();

        TravauxResponse base = toSummaryResponse(t);
        base.setIntervenants(intervenantDtos);
        base.setPermits(permitDtos);
        base.setEntryLogs(logDtos);
        base.setClosureForms(closureDtos);
        return base;
    }
}
