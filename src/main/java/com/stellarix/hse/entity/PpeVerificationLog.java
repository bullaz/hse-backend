package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.stellarix.hse.converter.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@ToString(exclude = {"itemResults", "rejectedImage"})
@Table(name = "ppe_verification_log")
public class PpeVerificationLog {

    @Id
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hse_induction_id", nullable = false)
    private HseInduction induction;

    @Column(nullable = false)
    @Check(constraints = "intent in ('WORK','VISIT')")
    private String intent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    @Check(constraints = "status in ('PENDING','VALIDATED','REJECTED')")
    private String status = "PENDING";

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "is_offline", nullable = false)
    private boolean offline = false;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rejection_causes")
    @Convert(converter = StringListConverter.class)
    private List<String> rejectionCauses;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permit_id")
    private WorkPermit permit;

    @OneToMany(mappedBy = "verificationLog", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<PpeItemResult> itemResults;

    @OneToOne(mappedBy = "verificationLog", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private RejectedImage rejectedImage;
}
