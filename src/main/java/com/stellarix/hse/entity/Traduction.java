package com.stellarix.hse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
        name = "traduction"
)
public class Traduction {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "traduction_id", updatable = false, nullable = false)
    private Integer traductionId;
	
	@ManyToOne(
            cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER
    )
	@JoinColumn(
            name = "question_id",
            referencedColumnName = "id"
    )
	private Question question;
	
	@Column(nullable = false)
	private String nom;
	
	@Column(nullable = false)
	private String description;
	
	@Column(name="code_langue", nullable = false)
	private String codeLangue;

}
