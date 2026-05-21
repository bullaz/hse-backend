package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkPermitRequest {

    @NotNull
    private UUID inductionId;

    @NotNull
    private Integer siteId;

    private String description;

    @NotNull
    private LocalDateTime startDatetime;

    @NotNull
    private LocalDateTime endDatetime;
}
