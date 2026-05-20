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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "verificationLog")
@Table(name = "ppe_item_result")
public class PpeItemResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id", updatable = false, nullable = false)
    private UUID resultId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "log_id", nullable = false)
    private PpeVerificationLog verificationLog;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ppe_item_id", nullable = false)
    private PpeItem ppeItem;

    @Column(nullable = false)
    private boolean detected;

    @Column
    private Float confidence;
}
