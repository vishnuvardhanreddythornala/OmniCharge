package com.omnicharge.recharge.repository;

import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RechargeRepository extends JpaRepository<Recharge, Long> {

    Optional<Recharge> findByRechargeId(String rechargeId);

    Page<Recharge> findByUserId(Long userId, Pageable pageable);

    long countByStatus(RechargeStatus status);

    List<Recharge> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate = :expiryDate")
    List<Recharge> findByStatusAndPlanExpiryDate(@Param("status") RechargeStatus status, @Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate BETWEEN :startDate AND :endDate")
    List<Recharge> findByStatusAndPlanExpiryDateBetween(
            @Param("status") RechargeStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Downtime-resilient: finds ALL SUCCESS recharges whose planExpiryDate has passed */
    @Query("SELECT r FROM Recharge r WHERE r.status = :status AND r.planExpiryDate < :cutoffDate")
    List<Recharge> findByStatusAndPlanExpiryDateBefore(
            @Param("status") RechargeStatus status,
            @Param("cutoffDate") LocalDate cutoffDate);

    /** Date-filtered user recharge history — supports optional start/end date boundaries */
    @Query("SELECT r FROM Recharge r WHERE r.userId = :userId " +
           "AND r.createdDate >= :startDate AND r.createdDate <= :endDate")
    Page<Recharge> findByUserIdWithDateFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /** Admin: date-filtered listing (no status filter) */
    @Query("SELECT r FROM Recharge r WHERE " +
           "r.createdDate >= :startDate AND r.createdDate <= :endDate")
    Page<Recharge> findAllWithDateFilters(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /** Admin: date + status filtered listing */
    @Query("SELECT r FROM Recharge r WHERE " +
           "r.status = :status " +
           "AND r.createdDate >= :startDate AND r.createdDate <= :endDate")
    Page<Recharge> findAllWithStatusAndDateFilters(
            @Param("status") RechargeStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /** Admin: date + multiple statuses filtered listing (e.g. INITIATED + PROCESSING) */
    @Query("SELECT r FROM Recharge r WHERE " +
           "r.status IN :statuses " +
           "AND r.createdDate >= :startDate AND r.createdDate <= :endDate")
    Page<Recharge> findAllWithStatusesAndDateFilters(
            @Param("statuses") java.util.List<RechargeStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
