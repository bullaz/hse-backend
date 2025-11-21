package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(
        name = "toko5"
)
public class Toko5 {
	
	@Id
    @Column(name = "toko5_id", updatable = false, nullable = false)
    private UUID toko5Id;
	
	@Column(name = "nom_contractant", nullable = false)
	private String nom_contractant;
	
	@Column(name = "prenom_contractant", nullable = false)
	private String prenom_contractant;
	
	@Column(name = "date_heure", nullable = false)
	private LocalDateTime dateHeure;
	
	@Column(nullable = false)
	@Check(constraints = "etat in ('valide','invalide','ongoing')")
	private String etat = "ongoing";
	
	// add those one to many relationships (fetch eager) .... change to fetch lazy in those other classes if you don't systematically need the toko5
    
}
