package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Hse;

@Repository
public interface HseRepository {
	Optional<Hse> getHseByUsernameOrEmail(String nameOrEmail);
}
