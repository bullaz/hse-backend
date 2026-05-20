package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PpeItemRequest {
    @NotBlank
    @Pattern(regexp = "HELMET|SAFETY_GLASSES|HIGH_VIS_VEST|GLOVES|SAFETY_SHOES")
    private String code;

    @NotBlank
    private String label;
}
