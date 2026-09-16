package com.pmh.repository;

import com.pmh.model.entity.Components;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentRepository extends JpaRepository<Components, Long>, JpaSpecificationExecutor<Components> {
    boolean existsByComponentCode(String componentCode);
    boolean existsByComponentCodeAndIdNot(String componentCode, Long id);
    Optional<Components> findByComponentCode(String componentCode);
}

