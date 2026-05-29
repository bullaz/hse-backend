package com.stellarix.hse.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TravauxEntryVerifyResponse {
    private UUID travauxId;
    private UUID inductionId;
    private String intent;       // "WORK" or "VISIT" derived from accessType
    private Integer siteId;
    private String siteName;
    private List<String> missingHabilitations; // empty = all good
}
