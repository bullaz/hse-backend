package com.stellarix.hse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Societe {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "societe_id", updatable = false, nullable = false)
	private Integer societeId;
	
	@Column(nullable = false, unique = true)
	private String nom;
}
