package com.stellarix.hse.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class TravauxRequest {
    private String accessType; // VISITE | TRAVAUX
    private String ticketNo;
    private Integer companyId;
    private Integer siteId;
    private String objetVisite;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String superviseurNom;
    private String superviseurEmail;
    private boolean impactServices;
    private List<String> impactTypes;
    private List<UUID> intervenantIds;
}
