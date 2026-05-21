package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.ZoneTypeRequest;
import com.stellarix.hse.entity.ZoneType;
import com.stellarix.hse.repository.ZoneTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZoneTypeService {

    private final ZoneTypeRepository repository;

    public List<ZoneType> getAll() {
        return repository.findAll();
    }

    @Transactional
    public ZoneType create(ZoneTypeRequest request) {
        ZoneType zoneType = new ZoneType();
        zoneType.setLabel(request.getLabel());
        return repository.save(zoneType);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ZoneType not found: " + id);
        }
        repository.deleteById(id);
    }
}
