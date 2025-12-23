package com.stellarix.hse.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.Question;
import com.stellarix.hse.entity.Reponse;
import com.stellarix.hse.entity.Toko5;

import jakarta.transaction.Transactional;

public interface ReponseRepository extends JpaRepository<Reponse, Integer>{
	Optional<Reponse> findByToko5AndQuestion(Toko5 toko5, Question question);
	
	@Modifying
	@Transactional
	@Query("DELETE FROM Reponse r " +
		       "WHERE r.toko5.toko5Id = :toko5Id " +
		       "AND r.valeur = false " +
		       "AND r.question.required = true")
	void resolve(@Param("toko5Id") UUID toko5Id);
	
}
