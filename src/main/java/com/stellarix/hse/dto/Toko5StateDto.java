package com.stellarix.hse.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Toko5StateDto {
	private UUID toko5Id;
	private String etat;
}
