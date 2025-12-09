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
	
	@Query(value = "SELECT q.* FROM question q " +
            "JOIN reponse r ON r.question_id = q.question_id " +
            "WHERE r.valeur = false " +
            "AND r.toko5_id = :toko5Id " +
            "AND q.required = true", 
    nativeQuery = true)
	List<Question> findToko5Probleme(@Param("toko5Id") UUID toko5Id);
}
