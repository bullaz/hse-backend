package com.stellarix.hse.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stellarix.hse.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer>{

}
