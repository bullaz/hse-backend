package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.HabilitationRequest;
import com.stellarix.hse.entity.Habilitation;
import com.stellarix.hse.repository.HabilitationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HabilitationService {

    private final HabilitationRepository repository;

    public List<Habilitation> getAll() {
        return repository.findAll();
    }

    @Transactional
    public Habilitation create(HabilitationRequest request) {
        Habilitation habilitation = new Habilitation();
        habilitation.setCode(request.getCode().trim().toUpperCase());
        habilitation.setLabel(request.getLabel());
        habilitation.setDescription(request.getDescription());
        return repository.save(habilitation);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Habilitation not found: " + id);
        }
        repository.deleteById(id);
    }
}
