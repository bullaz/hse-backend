package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Check;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
	private String nomContractant;
	
	@Column(name = "prenom_contractant", nullable = false)
	private String prenomContractant;
	
	@Column(name = "date_heure", nullable = false)
	private LocalDateTime dateHeure;
	
	@Column(nullable = false)
	@Check(constraints = "etat in ('valide','invalide','ongoing')")
	private String etat = "ongoing";
	
	@OneToMany(
			mappedBy = "toko5", 
			fetch = FetchType.EAGER,
			cascade = CascadeType.ALL
	)
    private List<Commentaire> listCommentaire;
	
	@OneToMany(
			mappedBy = "toko5", 
			fetch = FetchType.EAGER,
			cascade = CascadeType.ALL
	)
    private List<MesureControle> listMesureControle;
	
    
}
