package com.omnicharge.payment.service;

import com.omnicharge.payment.common.event.PaymentCompletedEvent;
import com.omnicharge.payment.common.exception.BadRequestException;
import com.omnicharge.payment.common.exception.ResourceNotFoundException;
import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.dto.DailyRevenueStats;
import com.omnicharge.payment.dto.TopUserStats;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {

    private static final String SERVICE_NAME = "payment-service";
    private static final String CTX_TRANSACTION_ID = "transactionId";
    private static final String CTX_RECHARGE_ID = "rechargeId";
    private static final String CTX_USER_ID = "userId";
    private static final String CTX_AMOUNT = "amount";
    private static final String CTX_PAYMENT_METHOD = "paymentMethod";
    private static final String CTX_FAILURE_REASON = "failureReason";

    private final TransactionRepository transactionRepository;
    private final IRazorpayPaymentService razorpayPaymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final RestTemplate restTemplate;
    private final LogEventPublisher logEventPublisher;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Generate transaction ID first
        String transactionId = "TXN-" + java.util.UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        
        // Log business operation: PAYMENT_PROCESSING_START
        Map<String, Object> startContext = new HashMap<>();
        startContext.put(CTX_TRANSACTION_ID, transactionId);
        startContext.put(CTX_RECHARGE_ID, request.getRechargeId());
        startContext.put(CTX_USER_ID, request.getUserId().toString());
        startContext.put(CTX_AMOUNT, request.getAmount().toString());
        startContext.put(CTX_PAYMENT_METHOD, request.getPaymentMethod());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .level("INFO")
                .message("Payment processing started")
                .eventType("PAYMENT_PROCESSING_START")
                .context(startContext)
                .timestamp(LocalDateTime.now())
                .build());
        
        // Create transaction record (PENDING)
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setRechargeId(request.getRechargeId());
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        transaction.setStatus(PaymentStatus.PENDING);
        
        // Save metadata for notification service (used later when webhook confirms payment)
        transaction.setUserEmail(request.getUserEmail());
        transaction.setUserMobile(request.getUserMobile());
        transaction.setMobileNumber(request.getMobileNumber());
        transaction.setOperatorName(request.getOperatorName());
        transaction.setPlanName(request.getPlanName());

        log.info("Saving Transaction metadata: email={}, mobile={}, op={}, plan={}, target={}", 
                transaction.getUserEmail(), transaction.getUserMobile(), 
                transaction.getOperatorName(), transaction.getPlanName(), transaction.getMobileNumber());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created with PENDING status: {} for recharge: {}", transactionId, request.getRechargeId());

        // Process payment via Razorpay
        PaymentResponse paymentResponse = razorpayPaymentService.processRazorpayPayment(request);

        // Log business operation: RAZORPAY_GATEWAY_INTERACTION
        Map<String, Object> gatewayContext = new HashMap<>();
        gatewayContext.put(CTX_TRANSACTION_ID, transactionId);
        gatewayContext.put(CTX_RECHARGE_ID, request.getRechargeId());
        gatewayContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
        gatewayContext.put("responseStatus", paymentResponse.getStatus());
        gatewayContext.put(CTX_AMOUNT, request.getAmount().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .level("INFO")
                .message("Razorpay gateway interaction completed")
                .eventType("RAZORPAY_GATEWAY_INTERACTION")
                .context(gatewayContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Update transaction with Razorpay response
        transaction.setRazorpayOrderId(paymentResponse.getRazorpayOrderId());

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment successful for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_SUCCESS
            Map<String, Object> successContext = new HashMap<>();
            successContext.put(CTX_TRANSACTION_ID, transactionId);
            successContext.put(CTX_RECHARGE_ID, request.getRechargeId());
            successContext.put(CTX_USER_ID, request.getUserId().toString());
            successContext.put(CTX_AMOUNT, request.getAmount().toString());
            successContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
            successContext.put(CTX_PAYMENT_METHOD, request.getPaymentMethod());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("INFO")
                    .message("Payment completed successfully")
                    .eventType("PAYMENT_SUCCESS")
                    .context(successContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } else if ("PENDING".equals(paymentResponse.getStatus())) {
            transaction.setStatus(PaymentStatus.PENDING);
            log.info("Payment pending for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_PENDING
            Map<String, Object> pendingContext = new HashMap<>();
            pendingContext.put(CTX_TRANSACTION_ID, transactionId);
            pendingContext.put(CTX_RECHARGE_ID, request.getRechargeId());
            pendingContext.put(CTX_USER_ID, request.getUserId().toString());
            pendingContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("INFO")
                    .message("Payment is pending confirmation")
                    .eventType("PAYMENT_PENDING")
                    .context(pendingContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Razorpay payment failed");
            log.warn("Payment failed for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_FAILED
            Map<String, Object> failedContext = new HashMap<>();
            failedContext.put(CTX_TRANSACTION_ID, transactionId);
            failedContext.put(CTX_RECHARGE_ID, request.getRechargeId());
            failedContext.put(CTX_USER_ID, request.getUserId().toString());
            failedContext.put(CTX_AMOUNT, request.getAmount().toString());
            failedContext.put(CTX_FAILURE_REASON, "Razorpay payment failed");
            failedContext.put(CTX_PAYMENT_METHOD, request.getPaymentMethod());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName(SERVICE_NAME)
                    .level("WARN")
                    .message("Payment failed")
                    .eventType("PAYMENT_FAILED")
                    .context(failedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        transaction = transactionRepository.save(transaction);

        // Only publish the event if it's a terminal state to avoid "failed" notifications for PENDING
        if (transaction.getStatus() == PaymentStatus.SUCCESS || transaction.getStatus() == PaymentStatus.FAILED) {
            publishPaymentCompletedEvent(transaction, transaction.getUserEmail(), transaction.getUserMobile(),
                    transaction.getMobileNumber(), transaction.getOperatorName(), transaction.getPlanName());
        }

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(paymentResponse.getStatus())
                .razorpayOrderId(paymentResponse.getRazorpayOrderId())
                .amount(paymentResponse.getAmount())
                .timestamp(paymentResponse.getTimestamp())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse confirmPayment(String transactionId, String razorpayPaymentId, String razorpaySignature) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Transaction {} is already confirmed", transactionId);
            return mapToResponse(transaction);
        }

        String previousStatus = transaction.getStatus().name();
        transaction.setStatus(PaymentStatus.SUCCESS);
        // Store the Razorpay payment ID (NOT in razorpayOrderId — that holds the order_id)
        transaction.setRazorpayPaymentId(razorpayPaymentId);
        transaction = transactionRepository.save(transaction);
        
        log.info("Payment confirmed successfully for transaction: {} (razorpayPaymentId: {}, razorpayOrderId: {})", 
                transactionId, razorpayPaymentId, transaction.getRazorpayOrderId());

        // Log business operation: PAYMENT_CONFIRMED
        Map<String, Object> confirmContext = new HashMap<>();
        confirmContext.put(CTX_TRANSACTION_ID, transactionId);
        confirmContext.put(CTX_RECHARGE_ID, transaction.getRechargeId());
        confirmContext.put(CTX_USER_ID, transaction.getUserId().toString());
        confirmContext.put("previousStatus", previousStatus);
        confirmContext.put("currentStatus", "SUCCESS");
        confirmContext.put("razorpayPaymentId", razorpayPaymentId);
        confirmContext.put(CTX_AMOUNT, transaction.getAmount().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .level("INFO")
                .message("Payment confirmed via webhook")
                .eventType("PAYMENT_CONFIRMED")
                .context(confirmContext)
                .timestamp(LocalDateTime.now())
                .build());

        // If notification metadata is missing from Transaction, fetch from recharge-service
        if (transaction.getMobileNumber() == null || transaction.getOperatorName() == null || transaction.getPlanName() == null) {
            log.warn("Transaction {} has null metadata (mobileNumber/operatorName/planName). Fetching from recharge-service...", transactionId);
            enrichTransactionFromRechargeService(transaction);
        }
        
        // Publish PaymentApprovedEvent for saga orchestrator
        com.omnicharge.payment.common.event.saga.PaymentApprovedEvent approvedEvent = com.omnicharge.payment.common.event.saga.PaymentApprovedEvent.builder()
                .rechargeId(transaction.getRechargeId())
                .transactionId(transaction.getTransactionId())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .amount(transaction.getAmount())
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
        paymentEventProducer.publishPaymentApproved(approvedEvent);

        // Publish PaymentCompletedEvent for notification-service with enriched data
        publishPaymentCompletedEvent(transaction, 
                transaction.getUserEmail(), 
                transaction.getUserMobile(), 
                transaction.getMobileNumber(), 
                transaction.getOperatorName(), 
                transaction.getPlanName());

        return mapToResponse(transaction);
    }

    @Override
    public TransactionResponse getTransaction(String transactionId, Long userId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to transaction");
        }

        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getPaymentHistory(
            Long userId, 
            String transactionId,
            BigDecimal minAmount, 
            BigDecimal maxAmount, 
            PaymentStatus status, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.findByUserIdWithFilters(
                userId, minAmount, maxAmount, status, startDate, endDate, transactionId, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Override
    public Page<TransactionResponse> getAllTransactions(
            Long userId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String rechargeId,
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.findAllWithFilters(
                userId, minAmount, maxAmount, status, startDate, endDate, rechargeId, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Override
    public PaymentStatsResponse getPaymentStats(Integer days) {
        if (days == null) {
            days = 30; // Default to last 30 days
        }
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        // Overall stats
        long totalTransactions = transactionRepository.count();
        long successfulTransactions = transactionRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedTransactions = transactionRepository.countByStatus(PaymentStatus.FAILED);
        long pendingTransactions = transactionRepository.countByStatus(PaymentStatus.PENDING);
        
        BigDecimal totalRevenue = transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        BigDecimal successAmount = totalRevenue;
        BigDecimal failedAmount = transactionRepository.sumAmountByStatus(PaymentStatus.FAILED);
        BigDecimal averageAmount = transactionRepository.averageAmountByStatus(PaymentStatus.SUCCESS);
        
        // Today's stats
        long todayTransactions = transactionRepository.countTransactionsSince(todayStart);
        BigDecimal todayRevenue = transactionRepository.sumAmountSinceByStatus(todayStart, PaymentStatus.SUCCESS);
        
        // Revenue by date (last N days)
        List<Object[]> revenueData = transactionRepository.findRevenueByDate(startDate, PaymentStatus.SUCCESS);
        List<DailyRevenueStats> revenueByDate = revenueData.stream()
                .map(row -> DailyRevenueStats.builder()
                        .date(row[0].toString())
                        .transactionCount((Long) row[1])
                        .revenue((BigDecimal) row[2])
                        .build())
                .toList();
        
        // Top 10 users by revenue
        List<Object[]> topUsersData = transactionRepository.findTopUsersByRevenue(
                PaymentStatus.SUCCESS, PageRequest.of(0, 10));
        List<TopUserStats> topUsers = topUsersData.stream()
                .map(row -> TopUserStats.builder()
                        .userId((Long) row[0])
                        .transactionCount((Long) row[1])
                        .totalSpent((BigDecimal) row[2])
                        .build())
                .toList();

        return PaymentStatsResponse.builder()
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .totalRevenue(totalRevenue)
                .successAmount(successAmount)
                .failedAmount(failedAmount)
                .averageTransactionAmount(averageAmount)
                .todayTransactions(todayTransactions)
                .todayRevenue(todayRevenue)
                .revenueByDate(revenueByDate)
                .topUsers(topUsers)
                .build();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .rechargeId(transaction.getRechargeId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .userEmail(transaction.getUserEmail())
                .userMobile(transaction.getUserMobile())
                .mobileNumber(transaction.getMobileNumber())
                .operatorName(transaction.getOperatorName())
                .planName(transaction.getPlanName())
                .createdDate(transaction.getCreatedDate())
                .build();
    }

    private void publishPaymentCompletedEvent(Transaction transaction, String userEmail, String userMobile,
                                              String mobileNumber, String operatorName, String planName) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .transactionId(transaction.getTransactionId())
                    .rechargeId(transaction.getRechargeId())
                    .userId(transaction.getUserId())
                    .userEmail(userEmail)
                    .userMobile(userMobile)
                    .mobileNumber(mobileNumber)
                    .operatorName(operatorName)
                    .planName(planName)
                    .amount(transaction.getAmount())
                    .status(transaction.getStatus().name())
                    .paymentMethod(transaction.getPaymentMethod().name())
                    .timestamp(LocalDateTime.now())
                    .build();

            paymentEventProducer.publishPaymentCompleted(event);
            log.info("Published payment completed event: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish payment event: {}", transaction.getTransactionId(), e);
        }
    }

    /**
     * Fetches recharge details from the recharge-service and enriches the Transaction entity.
     * This is a fallback for when the saga event did not propagate mobileNumber/operatorName/planName.
     */
    @SuppressWarnings("unchecked")
    private void enrichTransactionFromRechargeService(Transaction transaction) {
        try {
            String url = "http://recharge-service/api/internal/recharges/" + transaction.getRechargeId();
            log.info("Calling recharge-service: {}", url);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    String mobileNumber = (String) data.get("mobileNumber");
                    String operatorName = (String) data.get("operatorName");
                    String planName = (String) data.get("planName");
                    
                    log.info("Enriched from recharge-service: mobile={}, op={}, plan={}", 
                            mobileNumber, operatorName, planName);
                    
                    transaction.setMobileNumber(mobileNumber);
                    transaction.setOperatorName(operatorName);
                    transaction.setPlanName(planName);
                    
                    // Persist the enriched data for future reference
                    transactionRepository.save(transaction);
                }
            } else {
                log.warn("Recharge-service returned no data for rechargeId: {}", transaction.getRechargeId());
            }
        } catch (Exception e) {
            log.error("Failed to fetch recharge details from recharge-service for rechargeId: {}", 
                    transaction.getRechargeId(), e);
        }
    }

    /**
     * SERVER-SIDE VERIFICATION FALLBACK.
     * Checks the Razorpay API directly to see if the order was paid.
     * Called when the Razorpay checkout popup closes without the handler callback firing.
     */
    @Override
    @Transactional
    public TransactionResponse verifyPayment(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // If already confirmed, just return
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Transaction {} is already confirmed", transactionId);
            return mapToResponse(transaction);
        }

        String razorpayOrderId = transaction.getRazorpayOrderId();
        if (razorpayOrderId == null) {
            log.warn("No Razorpay order ID found for transaction {}", transactionId);
            return mapToResponse(transaction);
        }

        try {
            // Check Razorpay API for payments on this order
            com.razorpay.RazorpayClient razorpay = new com.razorpay.RazorpayClient(razorpayKeyId, razorpayKeySecret);
            java.util.List<com.razorpay.Payment> payments = razorpay.orders.fetchPayments(razorpayOrderId);

            log.info("Razorpay order {} has {} payment(s)", razorpayOrderId, payments.size());

            for (com.razorpay.Payment payment : payments) {
                String status = payment.get("status");
                String paymentId = payment.get("id");
                log.info("Razorpay payment {} status: {}", paymentId, status);

                if ("captured".equals(status) || "authorized".equals(status)) {
                    // Payment was successful! Confirm it.
                    log.info("Razorpay payment {} is {}, confirming transaction {}", paymentId, status, transactionId);
                    return confirmPayment(transactionId, paymentId, null);
                }
            }

            log.info("No successful payment found for order {} yet", razorpayOrderId);
        } catch (Exception e) {
            log.error("Failed to verify payment with Razorpay for transaction {}: {}", transactionId, e.getMessage());
        }

        return mapToResponse(transaction);
    }

    /**
     * SAGA ROLLBACK: Marks a PENDING transaction as FAILED and publishes PaymentRejectedEvent.
     * Called when the user closes the Razorpay modal or a payment.failed event fires in the frontend.
     * The RechargeSagaConsumer in recharge-service will listen for this and update Recharge → FAILED.
     */
    @Override
    @Transactional
    public TransactionResponse failPayment(String transactionId, String failureReason) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Only fail if currently PENDING — don't overwrite a SUCCESS
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Cannot fail transaction {} — already marked as SUCCESS", transactionId);
            return mapToResponse(transaction);
        }
        
        if (transaction.getStatus() == PaymentStatus.FAILED) {
            log.info("Transaction {} is already marked as FAILED", transactionId);
            return mapToResponse(transaction);
        }

        String previousStatus = transaction.getStatus().name();
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureReason(failureReason);
        transaction = transactionRepository.save(transaction);

        log.warn("Payment marked as FAILED for transaction: {} | Reason: {}", transactionId, failureReason);

        // Log business operation: PAYMENT_FAILED_BY_USER
        Map<String, Object> failContext = new HashMap<>();
        failContext.put(CTX_TRANSACTION_ID, transactionId);
        failContext.put(CTX_RECHARGE_ID, transaction.getRechargeId());
        failContext.put(CTX_USER_ID, transaction.getUserId().toString());
        failContext.put("previousStatus", previousStatus);
        failContext.put("currentStatus", "FAILED");
        failContext.put(CTX_FAILURE_REASON, failureReason);
        failContext.put(CTX_AMOUNT, transaction.getAmount().toString());

        logEventPublisher.publish(LogEvent.builder()
                .serviceName(SERVICE_NAME)
                .level("WARN")
                .message("Payment failed/cancelled by user")
                .eventType("PAYMENT_FAILED_BY_USER")
                .context(failContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Publish PaymentRejectedEvent for SAGA orchestrator → recharge-service updates Recharge to FAILED
        com.omnicharge.payment.common.event.saga.PaymentRejectedEvent rejectedEvent = com.omnicharge.payment.common.event.saga.PaymentRejectedEvent.builder()
                .rechargeId(transaction.getRechargeId())
                .failureReason(failureReason)
                .timestamp(LocalDateTime.now())
                .build();
        paymentEventProducer.publishPaymentRejected(rejectedEvent);

        return mapToResponse(transaction);
    }
}

