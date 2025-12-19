package com.stellarix.hse.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.Commentaire;

public interface CommentaireRepository extends JpaRepository<Commentaire, UUID>{
	
	@Query(value = "SELECT * FROM hse_schema.commentaire where toko5_id = :toko5Id", 
    nativeQuery = true)
	List<Commentaire> findByToko5Id(@Param("toko5Id") UUID toko5Id);

}
