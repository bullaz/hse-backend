package com.stellarix.hse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stellarix.hse.entity.TaskDetail;

public interface TaskDetailRepository extends JpaRepository<TaskDetail, Integer>{
	
	@Query(value ="select * from hse_schema.task_detail where CAST(date AS DATE) = CAST(:daty AS DATE)",
			nativeQuery = true
	)
	List<TaskDetail> findByDate(@Param("daty") String date);
}
