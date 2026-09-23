package com.cocheraya.repository;

import com.cocheraya.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    
    Optional<Feature> findByName(String name);

    
    boolean existsByName(String name);
}
