package com.stellarix.hse.dto;

import java.util.List;

import lombok.Data;

@Data
public class TaskDto {
	private Integer taskId;
	private String nom;
	private List<Integer> listQuestionId;
}
