package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Hse;

@Repository
public interface HseRepository extends JpaRepository<Hse, Integer>{
	
	Optional<Hse> findByUsernameOrEmail(String nameOrEmail, String nameOrEmail2);
}
