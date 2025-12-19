package com.stellarix.hse.service;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MesureControleRequestDto {
	private UUID mesureControleId;
	private UUID toko5Id;
	private String questionNom;
	private String mesure;
	private Boolean implemented;
}
