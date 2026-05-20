package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PpeItemResultRequest {
    @NotBlank
    private String ppeItemCode;
    private boolean detected;
    private Float confidence;
}
