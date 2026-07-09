package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;

/**
 * One supervisor closure submission. A Travaux can accumulate several of these across
 * reject/resubmit cycles — rejected ones are kept (status=REJECTED + rejectionReason)
 * rather than deleted, so the audit trail shows what was actually submitted and why
 * HSE sent it back, not just the latest accepted version.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "travaux")
@Table(name = "travaux_closure_form")
public class TravauxClosureForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "closure_id", updatable = false, nullable = false)
    private UUID closureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travaux_id", nullable = false)
    private Travaux travaux;

    @Column(name = "chantier_propre", nullable = false)
    private boolean chantierPropre;

    @Column(name = "danger_residuel", nullable = false)
    private boolean dangerResiduel;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false, length = 20)
    @Check(constraints = "status IN ('PENDING','ACCEPTED','REJECTED')")
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @PrePersist
    void prePersist() {
        this.submittedAt = LocalDateTime.now();
    }
}
