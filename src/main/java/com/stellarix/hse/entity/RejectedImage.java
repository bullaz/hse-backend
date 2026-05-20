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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Stores encrypted image data for REJECTED verifications only.
 * Supervisor review window: 48 hours from capturedAt, then flagged for deletion via expiresAt.
 * VALIDATED logs never produce a RejectedImage record.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "verificationLog")
@Table(name = "rejected_image")
public class RejectedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "image_id", updatable = false, nullable = false)
    private UUID imageId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false, unique = true)
    private PpeVerificationLog verificationLog;

    @Column(name = "encrypted_data", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedData;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
