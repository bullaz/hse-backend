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
public interface HseRepository extends JpaRepository<Hse, Integer>{
	
	Optional<Hse> findByUsernameOrEmail(String nameOrEmail, String nameOrEmail2);
	
	@Query("select q.* from question q, reponse r where r.question_id = question.question_id and r.valeur = false and r.toko5_id = :toko5Id? and q.required = true")
	List<Question> findToko5Probleme(@Param("toko5Id") UUID toko5Id);
}
