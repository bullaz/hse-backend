package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ZoneTypeRequest {
    @NotBlank
    private String label;
}
