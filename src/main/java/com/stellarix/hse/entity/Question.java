package com.stellarix.hse.entity;

import org.hibernate.annotations.Check;

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
        name = "question"
)
public class Question {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id", updatable = false, nullable = false)
    private Integer questionId;
	
	@Column(nullable = false)
	private String nom;
	
	private String pictogramme;
	
	@Check(constraints = "categorie in ('think','organise','risk','epi','safety')")
	private String categorie;
	
	@Column(nullable = false)
	private Boolean required = false;
	
//	@Column(nullable = false)
//	private Boolean is_risk_question = false;

}
