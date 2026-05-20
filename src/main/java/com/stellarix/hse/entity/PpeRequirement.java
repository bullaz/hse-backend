package com.stellarix.hse.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.hibernate.annotations.Check;

/**
 * PPE Matrix row: for a given site + intent, this PPE item is required.
 * Query all rows matching (site, intent) to get the full required set.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(
    name = "ppe_requirement",
    uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "intent", "ppe_item_id"})
)
public class PpeRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id", updatable = false, nullable = false)
    @JsonProperty("id")
    private Integer requirementId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    @Check(constraints = "intent in ('WORK','VISIT')")
    private String intent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ppe_item_id", nullable = false)
    private PpeItem ppeItem;
}
