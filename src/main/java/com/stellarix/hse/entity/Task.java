package com.stellarix.hse.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Data;

@Entity
@Data
public class Task {
	
	@Column(name="task_id")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer taskId = 0;
	private String nom;
	
	@ManyToMany(
			fetch = FetchType.EAGER
	)
	@JoinTable(
        name = "task_question",
        joinColumns = @JoinColumn(name = "task_id"), 
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
	List<Question> listQuestion;
	
}
