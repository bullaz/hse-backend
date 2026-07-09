package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.HseInductionRequest;
import com.stellarix.hse.entity.Company;
import com.stellarix.hse.entity.Habilitation;
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.entity.InductionRole;
import com.stellarix.hse.repository.CompanyRepository;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.InductionRoleRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;
import com.stellarix.hse.repository.TravauxIntervenantRepository;
import com.stellarix.hse.repository.WorkPermitRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HseInductionService {

    private final HseInductionRepository repository;
    private final PpeVerificationLogRepository logRepository;
    private final HabilitationRepository habilitationRepository;
    private final CompanyRepository companyRepository;
    private final InductionRoleRepository inductionRoleRepository;
    private final TravauxIntervenantRepository travauxIntervenantRepository;
    private final WorkPermitRepository workPermitRepository;

    public Page<HseInduction> getAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("registeredAt").descending()));
    }

    /** Mobile calls this on startup to cache the full induction list for offline checks. */
    public List<Map<String, Object>> getAllNames() {
        return repository.findAll().stream()
                .<Map<String, Object>>map(i -> {
                    var habCodes = i.getHabilitations().stream().map(h -> h.getCode()).toList();
                    var result = new java.util.HashMap<String, Object>();
                    result.put("id", i.getInductionId());
                    result.put("firstName", i.getFirstName());
                    result.put("lastName", i.getLastName());
                    result.put("habilitationCodes", habCodes);
                    if (i.getCinNumber() != null) result.put("cinNumber", i.getCinNumber());
                    if (i.getCompany() != null) {
                        result.put("company", java.util.Map.of(
                                "id", i.getCompany().getCompanyId(),
                                "name", i.getCompany().getName()));
                    }
                    return result;
                })
                .toList();
    }

    /**
     * Mobile calls this before the PPE check.
     * Returns {@code {inducted:true, inductionId:UUID, habilitationCodes:[...]}} or {@code {inducted:false}}.
     * If {@code cin} is provided, the record's CIN must match (case-insensitive).
     */
    public Map<String, Object> verifyInduction(String firstName, String lastName, String cin) {
        var opt = repository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim());
        if (opt.isEmpty()) {
            return Map.of("inducted", false, "habilitationCodes", java.util.List.of());
        }
        var i = opt.get();
        if (cin != null && !cin.isBlank() && i.getCinNumber() != null
                && !i.getCinNumber().equalsIgnoreCase(cin.trim())) {
            return Map.of("inducted", false, "cinMismatch", true, "habilitationCodes", java.util.List.of());
        }
        var habCodes = i.getHabilitations().stream().map(h -> h.getCode()).toList();
        return Map.of("inducted", true, "inductionId", i.getInductionId(), "habilitationCodes", habCodes);
    }

    public boolean isInducted(String firstName, String lastName) {
        return repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim());
    }

    @Transactional
    public HseInduction add(HseInductionRequest request) {
        if (repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(request.getFirstName().trim(), request.getLastName().trim())) {
            throw new IllegalArgumentException("Person already inducted: " + request.getFirstName() + " " + request.getLastName());
        }

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId()).orElse(null);
        }

        InductionRole role = null;
        if (request.getRoleId() != null) {
            role = inductionRoleRepository.findById(request.getRoleId()).orElse(null);
        }

        HseInduction induction = new HseInduction();
        induction.setFirstName(request.getFirstName().trim());
        induction.setLastName(request.getLastName().trim());
        induction.setCinNumber(request.getCinNumber());
        induction.setPhone(request.getPhone());
        induction.setEmail(request.getEmail());
        induction.setWork(request.getWork());
        induction.setCompany(company);
        induction.setRole(role);
        induction.setHabilitations(request.getHabilitationIds() != null && !request.getHabilitationIds().isEmpty()
                ? habilitationRepository.findAllById(request.getHabilitationIds())
                : new ArrayList<>());
        return repository.save(induction);
    }

    @Transactional
    public HseInduction update(UUID id, HseInductionRequest request) {
        HseInduction induction = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Induction not found: " + id));

        if (!induction.getFirstName().equalsIgnoreCase(request.getFirstName().trim()) ||
                !induction.getLastName().equalsIgnoreCase(request.getLastName().trim())) {
            if (repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(request.getFirstName().trim(), request.getLastName().trim())) {
                throw new IllegalArgumentException("Person already inducted: " + request.getFirstName() + " " + request.getLastName());
            }
        }

        induction.setFirstName(request.getFirstName().trim());
        induction.setLastName(request.getLastName().trim());
        induction.setCinNumber(request.getCinNumber());
        induction.setPhone(request.getPhone());
        induction.setEmail(request.getEmail());
        induction.setWork(request.getWork());

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId()).orElse(null);
        }
        induction.setCompany(company);

        InductionRole role = null;
        if (request.getRoleId() != null) {
            role = inductionRoleRepository.findById(request.getRoleId()).orElse(null);
        }
        induction.setRole(role);

        induction.setHabilitations(request.getHabilitationIds() != null && !request.getHabilitationIds().isEmpty()
                ? habilitationRepository.findAllById(request.getHabilitationIds())
                : new ArrayList<>());

        return repository.save(induction);
    }

    @Transactional
    public void delete(UUID id) {
        if (logRepository.existsByInduction_InductionId(id)) {
            throw new IllegalStateException("Cannot delete: this person has existing verification logs.");
        }

        // Same reasoning as ZoneTypeService/HabilitationService.delete: check first so the
        // client gets a specific, actionable reason instead of a null-message
        // DataIntegrityViolationException from the FK constraints below.
        long travauxCount = travauxIntervenantRepository.countByInduction_InductionId(id);
        long permitCount = workPermitRepository.countByIntervenants_InductionId(id);
        if (travauxCount > 0 || permitCount > 0) {
            List<String> reasons = new ArrayList<>();
            if (travauxCount > 0) reasons.add(travauxCount + " demande(s) d'accès");
            if (permitCount > 0) reasons.add(permitCount + " permis de travail");
            throw new IllegalStateException(
                    "Impossible de supprimer cette personne : intervenant sur "
                            + String.join(" et ", reasons) + ".");
        }

        repository.deleteById(id);
    }
}
