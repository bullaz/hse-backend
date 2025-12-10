package com.stellarix.hse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stellarix.hse.entity.Toko5;

public interface Toko5Repository extends JpaRepository<Toko5, UUID>{
	List<Toko5> findByDateHeure(LocalDateTime date);
}
