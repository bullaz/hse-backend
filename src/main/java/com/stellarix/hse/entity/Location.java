package com.stellarix.hse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "location")
public class Location {
	
	@Column(name = "id_location")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idLocation;
	
	@Column(nullable = false)
	private String nom;
}
