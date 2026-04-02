package com.omnicharge.operator.repository;

import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    
    List<Plan> findByOperatorIdAndIsActive(Long operatorId, Boolean isActive);
    
    List<Plan> findByOperatorId(Long operatorId);
    
    Page<Plan> findByOperatorId(Long operatorId, Pageable pageable);
    
    // User-facing query: only active plans of active operators
    @Query("SELECT p FROM Plan p WHERE p.id = :id " +
           "AND p.isActive = true " +
           "AND p.operator.isActive = true")
    Optional<Plan> findActiveById(@Param("id") Long id);
    
    // User-facing search: only active plans of active operators
    @Query("SELECT p FROM Plan p WHERE p.operator.id = :operatorId " +
           "AND p.operator.isActive = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND p.isActive = true")
    Page<Plan> searchActivePlans(
            @Param("operatorId") Long operatorId,
            @Param("category") PlanCategory category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    
    // Admin query: filter by status
    @Query("SELECT p FROM Plan p WHERE p.operator.id = :operatorId " +
           "AND (:isActive IS NULL OR p.isActive = :isActive)")
    List<Plan> findByOperatorIdAndStatus(
            @Param("operatorId") Long operatorId,
            @Param("isActive") Boolean isActive
    );
    
    // Admin search with status filter
    @Query("SELECT p FROM Plan p WHERE " +
           "(:operatorId IS NULL OR p.operator.id = :operatorId) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:isActive IS NULL OR p.isActive = :isActive)")
    Page<Plan> searchPlansWithStatus(
            @Param("operatorId") Long operatorId,
            @Param("category") PlanCategory category,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
