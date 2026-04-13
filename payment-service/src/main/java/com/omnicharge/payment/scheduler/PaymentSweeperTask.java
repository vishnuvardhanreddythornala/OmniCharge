package com.omnicharge.payment.scheduler;

import com.omnicharge.payment.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zombie Transaction Sweeper.
 * Runs every 5 minutes. Finds PENDING transactions older than 15 minutes
 * (user abandoned the Razorpay modal / closed the tab), marks them FAILED,
 * and publishes PaymentRejectedEvent so recharge-service can roll back too.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSweeperTask {

    private static final int TIMEOUT_MINUTES = 15;

    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final LogEventPublisher logEventPublisher;

    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000) // every 5 min, 1 min after boot
    @Transactional
    public void sweepZombieTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Transaction> zombies = transactionRepository.findByStatusAndCreatedDateBefore(
                PaymentStatus.PENDING, cutoff);

        if (zombies.isEmpty()) {
            return; // Nothing to sweep — silent exit for clean logs
        }

        log.warn("SWEEPER: Found {} zombie PENDING transactions older than {} minutes. Initiating SAGA rollback...",
                zombies.size(), TIMEOUT_MINUTES);

        for (Transaction txn : zombies) {
            try {
                String previousStatus = txn.getStatus().name();
                txn.setStatus(PaymentStatus.FAILED);
                txn.setFailureReason("Timeout: Abandoned by user (auto-swept after " + TIMEOUT_MINUTES + " min)");
                transactionRepository.save(txn);

                // Check if the user successfully paid via another transaction for this same recharge
                List<Transaction> successfulTxns = transactionRepository.findByRechargeIdAndStatus(
                        txn.getRechargeId(), PaymentStatus.SUCCESS);
                
                if (successfulTxns != null && !successfulTxns.isEmpty()) {
                    log.info("SWEEPER: Ignored rollback for zombie {} because recharge {} was already paid via another transaction.",
                            txn.getTransactionId(), txn.getRechargeId());
                } else {
                    // Publish PaymentRejectedEvent → recharge-service marks Recharge as FAILED
                    PaymentRejectedEvent rejectedEvent = PaymentRejectedEvent.builder()
                            .rechargeId(txn.getRechargeId())
                            .failureReason("Payment abandoned by user (timeout)")
                            .timestamp(LocalDateTime.now())
                            .build();
                    paymentEventProducer.publishPaymentRejected(rejectedEvent);
                    
                    log.info("SWEEPER: Published rollback event for rechargeId: {}", txn.getRechargeId());
                }

                log.info("SWEEPER: Rolled back zombie transaction {} (rechargeId: {}, age: {} min)",
                        txn.getTransactionId(), txn.getRechargeId(),
                        java.time.Duration.between(txn.getCreatedDate(), LocalDateTime.now()).toMinutes());

                // Log business operation
                Map<String, Object> context = new HashMap<>();
                context.put("transactionId", txn.getTransactionId());
                context.put("rechargeId", txn.getRechargeId());
                context.put("userId", txn.getUserId().toString());
                context.put("previousStatus", previousStatus);
                context.put("currentStatus", "FAILED");
                context.put("failureReason", "Timeout/Abandoned");
                context.put("amount", txn.getAmount().toString());
                context.put("ageMinutes", java.time.Duration.between(txn.getCreatedDate(), LocalDateTime.now()).toMinutes());

                logEventPublisher.publish(LogEvent.builder()
                        .serviceName("payment-service")
                        .level("WARN")
                        .message("SWEEPER: Zombie transaction rolled back")
                        .eventType("PAYMENT_SWEEPER_ROLLBACK")
                        .context(context)
                        .timestamp(LocalDateTime.now())
                        .build());

            } catch (Exception e) {
                log.error("SWEEPER: Failed to roll back transaction {}: {}", txn.getTransactionId(), e.getMessage());
            }
        }

        log.info("SWEEPER: Completed. {} zombie transactions rolled back.", zombies.size());
    }
}
