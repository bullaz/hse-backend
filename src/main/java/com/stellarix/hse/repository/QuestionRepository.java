package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer>{
	
	@Query(value = "SELECT q.* FROM hse_schema.question q " +
            "JOIN hse_schema.reponse r ON r.question_id = q.question_id " +
            "WHERE r.valeur = false " +
            "AND r.toko5_id = :toko5Id " +
            "AND q.question_id in (select question_id from hse_schema.task_question tq where tq.task_id = (select task_id from hse_schema.toko5 where toko5_id = :toko5Id))", 
    nativeQuery = true)
	List<Question> findToko5ListProblem(@Param("toko5Id") UUID toko5Id);
	
	List<Question> findByCategorieAndRequired(String categorie,  Boolean required);
	
	List<Question> findByCategorie(String categorie);
	
	Optional<Question> findByNom(String nom);
	
	List<Question> findByCategorieNot(String categorie);
}
