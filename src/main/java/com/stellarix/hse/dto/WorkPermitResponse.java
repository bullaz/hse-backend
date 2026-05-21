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
public class WorkPermitResponse {

    private String permitId;
    private PersonDto person;
    private SiteDto site;
    private String description;
    private PermitTypeDto permitType;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String status;
    private boolean hasFile;
    private LocalDateTime createdAt;
    /** Non-null only on creation — codes of habilitations the person lacks for this site. */
    private List<String> missingHabilitations;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PersonDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SiteDto {
        private Integer id;
        private String name;
        private String zoneType;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PermitTypeDto {
        private Integer id;
        private String code;
        private String label;
    }
}
