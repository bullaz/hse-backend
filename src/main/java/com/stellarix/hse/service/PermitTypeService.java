package com.stellarix.hse.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stellarix.hse.dto.PermitTypeRequest;
import com.stellarix.hse.dto.PermitTypeResponse;
import com.stellarix.hse.entity.PermitTemplate;
import com.stellarix.hse.entity.PermitType;
import com.stellarix.hse.repository.PermitTemplateRepository;
import com.stellarix.hse.repository.PermitTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermitTypeService {

    private final PermitTypeRepository permitTypeRepository;
    private final PermitTemplateRepository templateRepository;

    public List<PermitTypeResponse> getAll() {
        return permitTypeRepository.findAllByOrderByLabelAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PermitTypeResponse create(PermitTypeRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (permitTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalStateException("Duplicate permit type code");
        }
        PermitType pt = new PermitType();
        pt.setCode(code);
        pt.setLabel(request.getLabel().trim());
        pt.setDescription(request.getDescription());
        return toResponse(permitTypeRepository.save(pt));
    }

    @Transactional
    public void delete(Integer id) {
        templateRepository.deleteByPermitType_PermitTypeId(id);
        permitTypeRepository.deleteById(id);
    }

    @Transactional
    public void uploadTemplate(Integer permitTypeId, String fileName, String contentType, byte[] data) {
        PermitType pt = permitTypeRepository.findById(permitTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Permit type not found: " + permitTypeId));
        templateRepository.deleteByPermitType_PermitTypeId(permitTypeId);
        PermitTemplate tmpl = new PermitTemplate();
        tmpl.setPermitType(pt);
        tmpl.setFileName(fileName);
        tmpl.setContentType(contentType != null ? contentType : "application/octet-stream");
        tmpl.setFileData(data);
        templateRepository.save(tmpl);
    }

    public PermitTemplate getTemplate(Integer permitTypeId) {
        return templateRepository.findByPermitType_PermitTypeId(permitTypeId)
                .orElseThrow(() -> new EntityNotFoundException("No template for permit type: " + permitTypeId));
    }

    private PermitTypeResponse toResponse(PermitType pt) {
        boolean hasTemplate = templateRepository.findByPermitType_PermitTypeId(pt.getPermitTypeId()).isPresent();
        return new PermitTypeResponse(pt.getPermitTypeId(), pt.getCode(), pt.getLabel(), pt.getDescription(), hasTemplate);
    }
}
