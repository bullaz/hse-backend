package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "hse_induction")
public class HseInduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "induction_id", updatable = false, nullable = false)
    @JsonProperty("id")
    private UUID inductionId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column
    private String phone;

    @Email
    @Column
    private String email;

    @Column
    private String work;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private InductionRole role;

    @Column(name = "cin_number")
    private String cinNumber;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "induction_habilitation",
            joinColumns = @JoinColumn(name = "induction_id"),
            inverseJoinColumns = @JoinColumn(name = "habilitation_id")
    )
    private List<Habilitation> habilitations = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.registeredAt = LocalDateTime.now();
    }
}
