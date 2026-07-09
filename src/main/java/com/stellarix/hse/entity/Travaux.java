package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stellarix.hse.converter.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"permits", "intervenantLinks", "entryLogs", "closureForms"})
@Table(name = "travaux")
public class Travaux {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "travaux_id", updatable = false, nullable = false)
    private UUID travauxId;

    // Guards against concurrent state-transition races (e.g. two staff both clicking
    // activate/validate-closure/reject-closure on the same dossier at once).
    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "ticket_no", nullable = false, length = 50)
    private String ticketNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "objet_visite", nullable = false, length = 30)
    @Check(constraints = "objet_visite IN ('INSTALLATION','DESINSTALLATION','CONFIGURATION','MAINTENANCE','AUDIT','SURVEY','REFUELING','CONTROLE','VISITE')")
    private String objetVisite;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Column(name = "superviseur_nom", nullable = false, length = 200)
    private String superviseurNom;

    @Column(name = "superviseur_email", nullable = false, length = 255)
    private String superviseurEmail;

    @Column(name = "impact_services", nullable = false)
    private boolean impactServices = false;

    @Column(name = "impact_types")
    @Convert(converter = StringListConverter.class)
    private List<String> impactTypes = new ArrayList<>();

    @Column(name = "modop_file_name", length = 255)
    private String modopFileName;

    @Column(name = "modop_file_data", columnDefinition = "BYTEA")
    private byte[] modopFileData;

    @Column(name = "modop_content_type", length = 100)
    private String modopContentType;

    @Column(name = "access_type", nullable = false, length = 10)
    @Check(constraints = "access_type IN ('VISITE','TRAVAUX')")
    private String accessType = "TRAVAUX";

    @Column(nullable = false, length = 20)
    @Check(constraints = "status IN ('DRAFT','PERMITS_READY','ACTIVE','PENDING_CLOSURE','CLOSED')")
    private String status = "DRAFT";

    @Column(name = "closure_token", length = 36, unique = true)
    private String closureToken;

    @Column(name = "closure_token_used", nullable = false)
    private boolean closureTokenUsed = false;

    // Set when the last permit-dispatch email (send-to-supervisor or resend-on-reject)
    // failed — the async send has no other way to tell HSE something went wrong.
    @Column(name = "permit_email_failed", nullable = false)
    private boolean permitEmailFailed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Was a plain @ManyToMany until suspend/reactivate needed a per-membership
    // attribute — see TravauxIntervenant. getIntervenants()/setIntervenants() below
    // preserve the old "flat list of people" call shape for existing callers that
    // only care who's currently active on the dossier.
    @OneToMany(mappedBy = "travaux", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravauxIntervenant> intervenantLinks = new ArrayList<>();

    /** Active (non-suspended) intervenants — who's actually allowed to enter or be added to a new permit. */
    public List<HseInduction> getIntervenants() {
        return intervenantLinks.stream()
                .filter(link -> !link.isSuspended())
                .map(TravauxIntervenant::getInduction)
                .toList();
    }

    /** Full replace, non-suspended — used by create() and the DRAFT-only update(). */
    public void setIntervenants(List<HseInduction> inductions) {
        intervenantLinks.clear();
        for (HseInduction induction : inductions) {
            TravauxIntervenant link = new TravauxIntervenant();
            link.setTravaux(this);
            link.setInduction(induction);
            link.setSuspended(false);
            intervenantLinks.add(link);
        }
    }

    @OneToMany(mappedBy = "travaux", fetch = FetchType.LAZY)
    private List<WorkPermit> permits = new ArrayList<>();

    @OneToMany(mappedBy = "travaux", fetch = FetchType.LAZY)
    private List<PpeVerificationLog> entryLogs = new ArrayList<>();

    @OneToMany(mappedBy = "travaux", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TravauxClosureForm> closureForms = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
