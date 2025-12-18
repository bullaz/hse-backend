package com.stellarix.hse.service;

import java.util.List;

import com.stellarix.hse.entity.Reponse;
import com.stellarix.hse.entity.Toko5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReponseDTO {
	private String nomQuestion;
	private Boolean valeur;
}
