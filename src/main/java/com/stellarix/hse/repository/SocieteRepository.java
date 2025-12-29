package com.stellarix.hse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.stellarix.hse.entity.Societe;

@RepositoryRestResource(path="societes")
public interface SocieteRepository extends CrudRepository<Societe, Integer>{

}
