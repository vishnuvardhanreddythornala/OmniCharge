package com.omnicharge.payment.repository;

import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void findByTransactionId_ReturnsPresent() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-100");
        when(transactionRepository.findByTransactionId("TXN-100")).thenReturn(Optional.of(tx));

        Optional<Transaction> result = transactionRepository.findByTransactionId("TXN-100");
        assertTrue(result.isPresent());
        assertEquals("TXN-100", result.get().getTransactionId());
    }

    @Test
    void findByTransactionId_ReturnsEmpty() {
        when(transactionRepository.findByTransactionId("NON-EXISTENT")).thenReturn(Optional.empty());
        Optional<Transaction> result = transactionRepository.findByTransactionId("NON-EXISTENT");
        assertTrue(result.isEmpty());
    }

    @Test
    void sumAmountByStatus_ReturnsAggregation() {
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("5000.00"));
        BigDecimal sum = transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        assertEquals(0, new BigDecimal("5000.00").compareTo(sum));
    }

    @Test
    void sumAmountByStatus_ReturnsNullWhenNoTransactions() {
        when(transactionRepository.sumAmountByStatus(PaymentStatus.FAILED)).thenReturn(null);
        BigDecimal sum = transactionRepository.sumAmountByStatus(PaymentStatus.FAILED);
        assertNull(sum);
    }

    @Test
    void countByStatus_ReturnsCorrectCount() {
        when(transactionRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(42L);
        assertEquals(42L, transactionRepository.countByStatus(PaymentStatus.SUCCESS));
    }

    @Test
    void averageAmountByStatus_ReturnsCorrectAverage() {
        when(transactionRepository.averageAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("250.00"));
        BigDecimal avg = transactionRepository.averageAmountByStatus(PaymentStatus.SUCCESS);
        assertEquals(0, new BigDecimal("250.00").compareTo(avg));
    }

    @Test
    void findTopUsersByRevenue_ReturnsAggregatedData() {
        Object[] row = new Object[]{1L, 5L, new BigDecimal("2500.00")};
        List<Object[]> topUsers = Collections.singletonList(row);
        when(transactionRepository.findTopUsersByRevenue(eq(PaymentStatus.SUCCESS), any())).thenReturn(topUsers);

        List<Object[]> result = transactionRepository.findTopUsersByRevenue(PaymentStatus.SUCCESS, PageRequest.of(0, 10));
        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0)[0]);
        assertEquals(5L, result.get(0)[1]);
    }

    @Test
    void findRevenueByDate_ReturnsDateGroupedRevenue() {
        Object[] row = new Object[]{"2026-03-28", new BigDecimal("1000.00")};
        List<Object[]> revenue = Collections.singletonList(row);
        when(transactionRepository.findRevenueByDate(any(), eq(PaymentStatus.SUCCESS))).thenReturn(revenue);

        List<Object[]> result = transactionRepository.findRevenueByDate(LocalDateTime.now().minusDays(7), PaymentStatus.SUCCESS);
        assertFalse(result.isEmpty());
        assertEquals("2026-03-28", result.get(0)[0]);
    }

    @Test
    void findByUserIdWithFilters_ReturnsPaginatedResults() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-FILTERED");
        tx.setUserId(1L);
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(tx));

        when(transactionRepository.findByUserIdWithFilters(eq(1L), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<Transaction> result = transactionRepository.findByUserIdWithFilters(
                1L, null, null, null, null, null, PageRequest.of(0, 10));
        assertFalse(result.isEmpty());
        assertEquals("TXN-FILTERED", result.getContent().get(0).getTransactionId());
    }

    @Test
    void findAllWithFilters_ReturnsPaginatedResults() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-ADMIN-ALL");
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(tx));

        when(transactionRepository.findAllWithFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<Transaction> result = transactionRepository.findAllWithFilters(
                null, null, null, null, null, null, null, PageRequest.of(0, 10));
        assertFalse(result.isEmpty());
    }
}
