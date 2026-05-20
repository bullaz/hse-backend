package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.PpeItemRequest;
import com.stellarix.hse.entity.PpeItem;
import com.stellarix.hse.repository.PpeItemRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PpeItemService {

    private final PpeItemRepository repository;

    public List<PpeItem> getAll() {
        return repository.findAll();
    }

    public PpeItem findByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("PPE item not found: " + code));
    }

    @Transactional
    public PpeItem create(PpeItemRequest request) {
        PpeItem item = new PpeItem();
        item.setCode(request.getCode());
        item.setLabel(request.getLabel());
        return repository.save(item);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
