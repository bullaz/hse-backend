package com.stellarix.hse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
        name = "mesure_controle"
)
public class MesureControle {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mesure_controle_id", updatable = false, nullable = false)
    private Integer mesureControleId;
	
	
	@ManyToOne(
            fetch = FetchType.LAZY
    )
	@JoinColumn(
            name = "toko5_id",
            referencedColumnName = "toko5_id"
    )
	@JsonIgnore  
	@ToString.Exclude
	private Toko5 toko5;
	
	@ManyToOne(
            cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER
    )
	@JoinColumn(
            name = "question_id",
            referencedColumnName = "question_id"
    )
	private Question question;
	
	@Column(name = "mesure_prise", nullable = false)
	String mesurePrise;
	
	private Boolean implemented;
	
}
