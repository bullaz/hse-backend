package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.CompanyRequest;
import com.stellarix.hse.entity.Company;
import com.stellarix.hse.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repository;

    public List<Company> getAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Company create(CompanyRequest request) {
        String name = request.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Company already exists: " + name);
        }
        Company company = new Company();
        company.setName(name);
        return repository.save(company);
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
