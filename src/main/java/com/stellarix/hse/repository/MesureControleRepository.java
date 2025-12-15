package com.stellarix.hse.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.MesureControle;

public interface MesureControleRepository extends JpaRepository<MesureControle, Integer>{
	
	@Query(value = "SELECT * FROM hse_schema.mesure_controle where toko5_id = :toko5Id", 
		    nativeQuery = true)
			List<MesureControle> findByToko5Id(@Param("toko5Id") UUID toko5Id);
	
}
