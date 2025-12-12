package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.Toko5;

public interface Toko5Repository extends JpaRepository<Toko5, UUID>{
	

	@Query(value ="select * from hse_schema.toko5 where CAST(date_heure AS DATE) = CAST(:daty AS DATE)",
			nativeQuery = true
	)
	List<Toko5> findByDate(@Param("daty") String daty);
}
