package com.omnicharge.payment.scheduler;

import com.omnicharge.payment.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class PaymentSweeperTaskTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PaymentSweeperTask paymentSweeperTask;

    @Test
    void sweepZombieTransactions_NoZombies() {
        when(transactionRepository.findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        paymentSweeperTask.sweepZombieTransactions();

        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }

    @Test
    void sweepZombieTransactions_WithZombies_NoSuccessfulTxns() {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN123");
        txn.setRechargeId("REC123");
        txn.setUserId(1L);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(new BigDecimal("100.00"));
        txn.setCreatedDate(LocalDateTime.now().minusMinutes(20));

        when(transactionRepository.findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(txn));
        when(transactionRepository.findByRechargeIdAndStatus("REC123", PaymentStatus.SUCCESS))
                .thenReturn(Collections.emptyList());

        paymentSweeperTask.sweepZombieTransactions();

        verify(transactionRepository, times(1)).save(txn);
        verify(paymentEventProducer, times(1)).publishPaymentRejected(any(PaymentRejectedEvent.class));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void sweepZombieTransactions_WithZombies_AlreadyPaid() {
        Transaction zombieTxn = new Transaction();
        zombieTxn.setTransactionId("TXN123");
        zombieTxn.setRechargeId("REC123");
        zombieTxn.setUserId(1L);
        zombieTxn.setStatus(PaymentStatus.PENDING);
        zombieTxn.setAmount(new BigDecimal("100.00"));
        zombieTxn.setCreatedDate(LocalDateTime.now().minusMinutes(20));

        Transaction successTxn = new Transaction();
        successTxn.setTransactionId("TXN999");
        successTxn.setStatus(PaymentStatus.SUCCESS);

        when(transactionRepository.findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(zombieTxn));
        when(transactionRepository.findByRechargeIdAndStatus("REC123", PaymentStatus.SUCCESS))
                .thenReturn(List.of(successTxn));

        paymentSweeperTask.sweepZombieTransactions();

        verify(transactionRepository, times(1)).save(zombieTxn);
        verify(paymentEventProducer, never()).publishPaymentRejected(any(PaymentRejectedEvent.class));
    }

    @Test
    void sweepZombieTransactions_ExceptionHandling() {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN123");
        txn.setStatus(PaymentStatus.PENDING);
        txn.setCreatedDate(LocalDateTime.now().minusMinutes(20));

        when(transactionRepository.findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(txn));
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("DB Error"));

        paymentSweeperTask.sweepZombieTransactions();

        // Loop handles exception, so it should not throw out of the method
        verify(transactionRepository, times(1)).save(txn);
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }
}
