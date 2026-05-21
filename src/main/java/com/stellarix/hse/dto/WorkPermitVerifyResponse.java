package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkPermitVerifyResponse {

    private boolean valid;
    /** Null when valid=true. One of: INVALID_ID, WRONG_SITE, OUTSIDE_WINDOW, REVOKED, EXPIRED */
    private String reason;
    private String permitId;
    /** UUID of the HseInduction record — used by mobile to fill inductionId in the verification payload. */
    private UUID inductionId;
    private String firstName;
    private String lastName;
    private String siteName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
