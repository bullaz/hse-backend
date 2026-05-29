package com.stellarix.hse.dto;

import lombok.Data;

@Data
public class TravauxClosureRequest {
    private String token;
    private boolean chantierPropre;
    private boolean dangerResiduel;
    private String observations;
    private String signatureData;
}
