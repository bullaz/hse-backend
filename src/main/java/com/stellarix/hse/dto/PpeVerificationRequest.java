package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PpeVerificationRequest {

    @NotNull
    private UUID logId;

    @NotNull
    private UUID inductionId;

    @NotNull
    @Pattern(regexp = "WORK|VISIT")
    private String intent;

    @NotNull
    private Integer siteId;

    @NotNull
    private LocalDateTime capturedAt;

    private boolean offline = false;

    private UUID travauxId; // optional — links this log to a Travaux/Visite dossier

    private String description;

    @NotNull
    @Pattern(regexp = "VALIDATED|REJECTED")
    private String status;

    @NotNull
    @Valid
    private List<PpeItemResultRequest> itemResults;
}
