package com.stellarix.hse.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.entity.HseInduction;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.PpeVerificationLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HseInductionService {

    private final HseInductionRepository repository;
    private final PpeVerificationLogRepository logRepository;

    public Page<HseInduction> getAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("registeredAt").descending()));
    }

    /** Used by the mobile app to verify a worker before allowing the PPE check. */
    public boolean isInducted(String firstName, String lastName) {
        return repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim());
    }

    @Transactional
    public HseInduction add(String firstName, String lastName) {
        if (repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName.trim(), lastName.trim())) {
            throw new IllegalArgumentException("Person already inducted: " + firstName + " " + lastName);
        }
        HseInduction induction = new HseInduction();
        induction.setFirstName(firstName.trim());
        induction.setLastName(lastName.trim());
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
