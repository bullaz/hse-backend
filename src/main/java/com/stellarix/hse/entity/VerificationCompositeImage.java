package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Side-by-side composite image (original left | annotated right) stored for ALL verifications.
 * 48-hour TTL, purged by the cleanup scheduler.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "verificationLog")
@Table(name = "verification_composite_image", schema = "hse_schema")
public class VerificationCompositeImage {

    @Id
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "log_id", nullable = false)
    private PpeVerificationLog verificationLog;

    @Column(name = "image_data", nullable = false, columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
