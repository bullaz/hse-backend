package com.stellarix.hse.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "work_permit")
public class WorkPermit {

    @Id
    @Column(name = "permit_id", updatable = false, nullable = false, length = 20)
    private String permitId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "induction_id")
    private HseInduction induction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travaux_id")
    private Travaux travaux;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column
    private String description;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(nullable = false)
    @Check(constraints = "status in ('ACTIVE','EXPIRED','REVOKED')")
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permit_type_id")
    private PermitType permitType;

    @Column(name = "permit_file_name", length = 255)
    private String permitFileName;

    @Column(name = "permit_file_data", columnDefinition = "BYTEA")
    private byte[] permitFileData;

    @Column(name = "permit_file_content_type", length = 100)
    private String permitFileContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
