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
        name = "reponse"
)
public class Reponse {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reponse_id", updatable = false, nullable = false)
    private Integer reponseId;
	
	@ManyToOne(
            fetch = FetchType.EAGER
    )
	@JoinColumn(
            name = "toko5_id",
            referencedColumnName = "toko5_id"
    )
	private Toko5 toko5;
	
	
	@ManyToOne(
            fetch = FetchType.EAGER
    )
	@JoinColumn(
            name = "question_id",
            referencedColumnName = "question_id"
    )
	private Question question;
	
	@Column(nullable = false)
	private Boolean valeur;


}
