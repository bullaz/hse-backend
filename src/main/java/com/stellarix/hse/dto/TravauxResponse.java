package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TravauxResponse {

    private UUID travauxId;
    private String accessType;
    private String ticketNo;
    private String companyName;
    private Integer siteId;
    private String siteName;
    private String objetVisite;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String superviseurNom;
    private String superviseurEmail;
    private boolean impactServices;
    private List<String> impactTypes;
    private boolean hasModop;
    private String status;
    private boolean permitEmailFailed;
    private int permitCount;
    private int intervenantCount;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    // populated only for detail view
    private List<IntervenantDto> intervenants;
    private List<PermitSummaryDto> permits;
    private List<EntryLogDto> entryLogs;
    private List<ClosureFormDto> closureForms;

    @Data
    @Builder
    public static class IntervenantDto {
        private UUID inductionId;
        private String firstName;
        private String lastName;
        private String email;
        private String work;
        private boolean suspended;
    }

    @Data
    @Builder
    public static class PermitSummaryDto {
        private String permitId;
        private String permitTypeLabel;
        private String description;
        private String status;
        private boolean hasFile;
        private LocalDateTime createdAt;
        private List<PermitIntervenantDto> intervenants;
    }

    @Data
    @Builder
    public static class PermitIntervenantDto {
        private UUID inductionId;
        private String firstName;
        private String lastName;
    }

    @Data
    @Builder
    public static class EntryLogDto {
        private UUID logId;
        private String firstName;
        private String lastName;
        private String intent;
        private String status;
        private LocalDateTime capturedAt;
        private boolean offline;
    }

    @Data
    @Builder
    public static class ClosureFormDto {
        private UUID closureId;
        private boolean chantierPropre;
        private boolean dangerResiduel;
        private String observations;
        private String signatureData;
        private LocalDateTime submittedAt;
        private String status;
        private String rejectionReason;
    }
}
