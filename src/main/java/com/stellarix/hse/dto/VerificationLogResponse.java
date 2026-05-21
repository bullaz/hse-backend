package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationLogResponse {
    private UUID logId;
    private InductionDto induction;
    private String intent;
    private Integer siteId;
    private String siteName;
    private String status;
    private LocalDateTime capturedAt;
    private boolean offline;
    private LocalDateTime syncedAt;
    private List<ItemResultDto> itemResults;
    private List<String> rejectionCauses;
    private LocalDateTime certifiedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InductionDto {
        private UUID id;
        private String firstName;
        private String lastName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemResultDto {
        private String ppeItemCode;
        private String ppeItemLabel;
        private boolean detected;
        private Float confidence;
    }
}
