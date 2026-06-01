package com.stellarix.hse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Hse;

@Repository
public interface HseRepository extends JpaRepository<Hse, Integer> {

    Optional<Hse> findByUsernameOrEmail(String nameOrEmail, String nameOrEmail2);

    List<Hse> findByIsAdminFalseOrderByNomAscPrenomAsc();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByIsAdminTrue();
}
