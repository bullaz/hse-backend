package com.stellarix.hse.service;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentaireRequestDto {
	private UUID commentaireId;
	private UUID toko5Id;
	private String nom;
	private String prenom;
	private String commentaire;
}
