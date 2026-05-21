package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.ZoneTypeRequest;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.HabilitationRepository;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZoneTypeService {

    private final ZoneTypeRepository repository;
    private final HabilitationRepository habilitationRepository;

    public List<ZoneType> getAll() {
        return repository.findAll();
    }

    @Transactional
    public ZoneType create(ZoneTypeRequest request) {
        ZoneType zoneType = new ZoneType();
        applyRequest(zoneType, request);
        return repository.save(zoneType);
    }

    @Transactional
    public ZoneType update(Integer id, ZoneTypeRequest request) {
        ZoneType zoneType = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ZoneType not found: " + id));
        applyRequest(zoneType, request);
        return repository.save(zoneType);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ZoneType not found: " + id);
        }
        repository.deleteById(id);
    }

    private void applyRequest(ZoneType zoneType, ZoneTypeRequest request) {
        zoneType.setLabel(request.getLabel());
        zoneType.setHabilitations(request.getHabilitationIds() != null && !request.getHabilitationIds().isEmpty()
                ? habilitationRepository.findAllById(request.getHabilitationIds())
                : new ArrayList<>());
    }
}
