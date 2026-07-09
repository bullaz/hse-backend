package com.stellarix.hse.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A person's membership on a Travaux/Visite dossier. Was a plain @ManyToMany join
 * table until suspend/reactivate needed a per-membership attribute — a suspended
 * intervenant stays on the dossier's history but is treated as not-on-the-dossier
 * everywhere entry/eligibility is checked (see TravauxService.verifyEntry).
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"travaux"})
@Table(name = "travaux_intervenant", uniqueConstraints = @UniqueConstraint(columnNames = {"travaux_id", "induction_id"}))
public class TravauxIntervenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travaux_id", nullable = false)
    private Travaux travaux;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "induction_id", nullable = false)
    private HseInduction induction;

    @Column(name = "suspended", nullable = false)
    private boolean suspended = false;
}
