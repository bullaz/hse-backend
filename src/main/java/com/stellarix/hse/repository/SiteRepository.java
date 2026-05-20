package com.stellarix.hse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stellarix.hse.entity.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByName(String name);
}
