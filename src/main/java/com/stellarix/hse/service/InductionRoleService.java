package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.InductionRoleRequest;
import com.stellarix.hse.entity.InductionRole;
import com.stellarix.hse.repository.InductionRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InductionRoleService {

    private final InductionRoleRepository repository;

    public List<InductionRole> getAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional
    public InductionRole create(InductionRoleRequest request) {
        String name = request.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        InductionRole role = new InductionRole();
        role.setName(name);
        return repository.save(role);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
