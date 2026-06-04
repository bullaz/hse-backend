package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkPermitRequest {

    @NotNull
    private UUID travauxId;

    private List<UUID> inductionIds;

    private String description;

    @NotNull
    private LocalDateTime startDatetime;

    @NotNull
    private LocalDateTime endDatetime;

    private Integer permitTypeId;
}
