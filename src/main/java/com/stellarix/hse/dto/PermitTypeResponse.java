package com.stellarix.hse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermitTypeResponse {
    private Integer id;
    private String code;
    private String label;
    private String description;
    private boolean hasTemplate;
}
