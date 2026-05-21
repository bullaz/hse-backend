package com.stellarix.hse.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "zone_type")
public class ZoneType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_type_id", updatable = false, nullable = false)
    @JsonProperty("id")
    private Integer zoneTypeId;

    @Column(nullable = false, unique = true)
    private String label;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "zone_type_habilitation",
            joinColumns = @JoinColumn(name = "zone_type_id"),
            inverseJoinColumns = @JoinColumn(name = "habilitation_id")
    )
    private List<Habilitation> habilitations = new ArrayList<>();
}
