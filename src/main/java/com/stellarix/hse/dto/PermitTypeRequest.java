package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermitTypeRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String label;
    private String description;
}
