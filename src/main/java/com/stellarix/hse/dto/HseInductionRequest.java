package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HseInductionRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}
