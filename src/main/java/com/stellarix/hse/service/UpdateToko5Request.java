package com.stellarix.hse.service;

import java.util.List;

import com.stellarix.hse.entity.Question;
import com.stellarix.hse.entity.Reponse;
import com.stellarix.hse.entity.Toko5;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateToko5Request {
	private Toko5 toko5;
	private List<ReponseDTO> listReponseDTO;
}
