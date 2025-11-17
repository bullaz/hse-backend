package com.stellarix.hse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "hse"
)
public class Hse {
	
	@Id
	@Column(
			name = "hse_id"
	)
	String hseId = "";
	
	String email;
	String nom;
	String prenom;
	
}
