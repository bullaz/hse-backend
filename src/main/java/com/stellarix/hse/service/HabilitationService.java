package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.HabilitationRequest;
import com.stellarix.hse.entity.Habilitation;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.HseInductionRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HabilitationService {

    private final HabilitationRepository repository;
    private final HseInductionRepository inductionRepository;
    private final ZoneTypeRepository zoneTypeRepository;

    public List<Habilitation> getAll() {
        return repository.findAll();
    }

    @Transactional
    public Habilitation create(HabilitationRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new IllegalStateException("Duplicate habilitation code");
        }
        Habilitation habilitation = new Habilitation();
        habilitation.setCode(code);
        habilitation.setLabel(request.getLabel());
        habilitation.setDescription(request.getDescription());
        return repository.save(habilitation);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Habilitation not found: " + id);
        }

        // Same reasoning as ZoneTypeService.delete: check first so the client gets a
        // specific, actionable reason instead of a null-message DataIntegrityViolationException.
        long inductionCount = inductionRepository.countByHabilitations_HabilitationId(id);
        long zoneTypeCount = zoneTypeRepository.countByHabilitations_HabilitationId(id);
        if (inductionCount > 0 || zoneTypeCount > 0) {
            List<String> reasons = new ArrayList<>();
            if (inductionCount > 0) reasons.add(inductionCount + " personne(s) induite(s)");
            if (zoneTypeCount > 0) reasons.add(zoneTypeCount + " type(s) de zone");
            throw new IllegalStateException(
                    "Impossible de supprimer cette habilitation : encore utilisée par "
                            + String.join(" et ", reasons) + ".");
        }

        repository.deleteById(id);
    }
}
