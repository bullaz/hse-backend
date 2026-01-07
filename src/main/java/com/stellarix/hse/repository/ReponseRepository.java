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
	@Query(value = "DELETE FROM hse_schema.reponse r " +
		       "WHERE r.toko5_id = :toko5Id " +
		       "AND r.valeur = false " +
		       "AND r.question_id IN ( " +
		       "SELECT tq.question_id FROM hse_schema.task_question tq " +
		       "WHERE tq.task_id = (SELECT task_id FROM hse_schema.toko5 WHERE toko5_id = :toko5Id)" +
		       ")", nativeQuery = true)
	void resolve(@Param("toko5Id") UUID toko5Id);
}
