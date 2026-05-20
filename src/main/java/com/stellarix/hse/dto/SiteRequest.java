package com.stellarix.hse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SiteRequest {
    @NotBlank
    private String name;
    private Double latitude;
    private Double longitude;
}
