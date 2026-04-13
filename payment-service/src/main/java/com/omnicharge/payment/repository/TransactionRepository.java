package com.omnicharge.payment.repository;

import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findByRechargeId(String rechargeId);

    List<Transaction> findByRechargeIdAndStatus(String rechargeId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    // User filtering queries
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR t.createdDate >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR t.createdDate <= :endDate) " +
            "AND (:transactionId IS NULL OR t.transactionId LIKE %:transactionId%)")
    Page<Transaction> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("transactionId") String transactionId,
            Pageable pageable);

    // Admin filtering queries
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:userId IS NULL OR t.userId = :userId) " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR t.createdDate >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR t.createdDate <= :endDate) " +
            "AND (:rechargeId IS NULL OR t.rechargeId = :rechargeId)")
    Page<Transaction> findAllWithFilters(
            @Param("userId") Long userId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("rechargeId") String rechargeId,
            Pageable pageable);

    // Admin stats queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    Long countByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdDate >= :startDate")
    Long countTransactionsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdDate >= :startDate AND t.status = :status")
    BigDecimal sumAmountSinceByStatus(@Param("startDate") LocalDateTime startDate, @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t WHERE t.status = :status")
    BigDecimal averageAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT t.userId, COUNT(t), SUM(t.amount) FROM Transaction t " +
            "WHERE t.status = :status " +
            "GROUP BY t.userId " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopUsersByRevenue(@Param("status") PaymentStatus status, Pageable pageable);

    @Query("SELECT DATE(t.createdDate), COUNT(t), SUM(t.amount) FROM Transaction t " +
            "WHERE t.createdDate >= :startDate AND t.status = :status " +
            "GROUP BY DATE(t.createdDate) " +
            "ORDER BY DATE(t.createdDate) DESC")
    List<Object[]> findRevenueByDate(@Param("startDate") LocalDateTime startDate, @Param("status") PaymentStatus status);

    /**
     * Sweeper: Find all PENDING transactions created before the cutoff time.
     * These are zombie transactions where the user abandoned the payment flow.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdDate < :cutoff")
    List<Transaction> findByStatusAndCreatedDateBefore(
            @Param("status") PaymentStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
