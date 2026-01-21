package com.stellarix.hse.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TaskDetailDto {
	Integer workersNumber;
	LocalDateTime date;
	String description;
	Integer taskId;
}
