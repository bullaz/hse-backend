package com.stellarix.hse.entity;

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
        name = "hse"
)
public class Hse {
	
	@Id
	@Column(
			name = "hse_id"
	)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer hseId = 0;
	
	@Column(nullable = false)
	private String email;
	
	@Column(nullable = false)
	private String nom;
	
	@Column(nullable = false)
	private String username;
	
	@Column(nullable = false)
	private String prenom;
	
	@Column(nullable = false)
	private String password;
	
}
