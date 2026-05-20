package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PpeRequirementRequest {
    @NotNull
    private Integer siteId;

    @NotNull
    @Pattern(regexp = "WORK|VISIT")
    private String intent;

    @NotNull
    private Integer ppeItemId;
}
