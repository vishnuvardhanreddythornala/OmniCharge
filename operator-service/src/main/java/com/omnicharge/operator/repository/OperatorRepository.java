package com.omnicharge.operator.repository;

import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    
    Optional<Operator> findByCode(String code);
    
    Optional<Operator> findByName(String name);
    
    List<Operator> findByCategory(OperatorCategory category);
    
    List<Operator> findByIsActive(Boolean isActive);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
    
    // Find active operator by ID (for user-facing endpoints)
    @Query("SELECT o FROM Operator o WHERE o.id = :id AND o.isActive = true")
    Optional<Operator> findActiveById(@Param("id") Long id);
}
