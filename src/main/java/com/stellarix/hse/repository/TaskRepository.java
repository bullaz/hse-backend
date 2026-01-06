package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stellarix.hse.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer>{
	Optional<Task> findByNom(String nom);
}
