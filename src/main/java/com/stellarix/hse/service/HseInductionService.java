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
import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HseInductionService {

    private final HseInductionRepository repository;
    private final PpeVerificationLogRepository logRepository;
    private final HabilitationRepository habilitationRepository;

    public Page<HseInduction> getAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("registeredAt").descending()));
    }

    /** Mobile calls this on startup to cache the full induction list for offline checks. */
    public List<Map<String, Object>> getAllNames() {
        return repository.findAll().stream()
                .<Map<String, Object>>map(i -> Map.of(
                        "id", i.getInductionId(),
                        "firstName", i.getFirstName(),
                        "lastName", i.getLastName()))
                .toList();
    }

    /**
     * Mobile calls this before the PPE check.
     * Returns {@code {inducted:true, inductionId:UUID}} or {@code {inducted:false}}.
     */
    public Map<String, Object> verifyInduction(String firstName, String lastName) {
        return repository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())
                .<Map<String, Object>>map(i -> Map.of("inducted", true, "inductionId", i.getInductionId()))
                .orElse(Map.of("inducted", false));
    }

    /** Used internally (e.g. admin checks). */
    public boolean isInducted(String firstName, String lastName) {
        return repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim());
    }

    @Transactional
    public HseInduction add(HseInductionRequest request) {
        if (repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(request.getFirstName().trim(), request.getLastName().trim())) {
            throw new IllegalArgumentException("Person already inducted: " + request.getFirstName() + " " + request.getLastName());
        }
        HseInduction induction = new HseInduction();
        induction.setFirstName(request.getFirstName().trim());
        induction.setLastName(request.getLastName().trim());
        induction.setPhone(request.getPhone());
        induction.setEmail(request.getEmail());
        induction.setWork(request.getWork());
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
        repository.deleteById(id);
    }
}
