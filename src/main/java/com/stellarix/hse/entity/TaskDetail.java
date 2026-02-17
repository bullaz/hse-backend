package com.stellarix.hse.entity;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
        name = "task_detail"
)
public class TaskDetail {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)		
    @Column(name = "task_detail_id", updatable = false, nullable = false)
    private Integer taskDetailId;
	
	@Column(name = "date")
	private ZonedDateTime date;
	
	private String description;
	
	@ManyToOne(fetch = FetchType.EAGER)
	private Location location;
	
	@Column(name = "workers_number")
	private Integer workersNumber;
	
	@ManyToOne(
		fetch = FetchType.EAGER
    )
	private Task task;
	
	@Column(unique = true)
	private String codeSup;
	
	@Column(unique = true)
	private String codeWorker;
}
