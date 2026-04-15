package com.omnicharge.recharge.service;

import com.omnicharge.recharge.common.dto.ApiResponse;
import com.omnicharge.recharge.common.event.RechargeCompletedEvent;
import com.omnicharge.recharge.common.exception.BadRequestException;
import com.omnicharge.recharge.common.exception.ResourceNotFoundException;
import com.omnicharge.recharge.common.logging.LogEvent;
import com.omnicharge.recharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.OperatorServiceClient;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.*;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RechargeService implements IRechargeService {

    private final RechargeRepository rechargeRepository;
    private final OperatorServiceClient operatorServiceClient;
    private final UserServiceClient userServiceClient;
    private final RechargeEventProducer rechargeEventProducer;
    private final LogEventPublisher logEventPublisher;

    @Override
    @Transactional
    public RechargeResponse initiateRecharge(Long userId, RechargeRequest request) {
        // Validate plan with circuit breaker, retry, and caching(Feign)
        ApiResponse<PlanResponse> planApiResponse = operatorServiceClient.getPlan(request.getPlanId());
        
        // Check if Operator Service is unavailable (circuit breaker fallback)
        if (planApiResponse == null || !planApiResponse.isSuccess() || planApiResponse.getData() == null) {
            throw new BadRequestException("Unable to validate plan. Operator Service is temporarily unavailable. Please try again later.");
        }
        
        PlanResponse plan = planApiResponse.getData();

        if (!plan.getIsActive()) {
            throw new BadRequestException("Invalid or inactive plan");
        }

        if (!plan.getOperatorId().equals(request.getOperatorId())) {
            throw new BadRequestException("Plan does not belong to the specified operator");
        }

        // Create recharge record (INITIATED)
        Recharge recharge = new Recharge();
        recharge.setRechargeId("OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        recharge.setUserId(userId);
        recharge.setMobileNumber(request.getMobileNumber());
        recharge.setOperatorId(plan.getOperatorId());
        recharge.setOperatorName(plan.getOperatorName());
        recharge.setPlanId(plan.getId());
        recharge.setPlanName(plan.getPlanName());
        recharge.setAmount(plan.getPrice());
        recharge.setPlanValidityDays(plan.getValidityDays());
        recharge.setPlanExpiryDate(LocalDate.now().plusDays(plan.getValidityDays()));
        recharge.setStatus(RechargeStatus.INITIATED);

        recharge = rechargeRepository.save(recharge);
        log.info("Recharge initiated: {}", recharge.getRechargeId());

        // Log business operation: RECHARGE_INITIATED
        Map<String, Object> initiatedContext = new HashMap<>();
        initiatedContext.put("rechargeId", recharge.getRechargeId());
        initiatedContext.put("userId", recharge.getUserId().toString());
        initiatedContext.put("amount", recharge.getAmount().toString());
        initiatedContext.put("operatorName", recharge.getOperatorName());
        initiatedContext.put("planName", recharge.getPlanName());
        initiatedContext.put("mobileNumber", recharge.getMobileNumber());
        initiatedContext.put("status", recharge.getStatus().name());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("INFO")
                .message("Recharge initiated")
                .eventType("RECHARGE_INITIATED")
                .context(initiatedContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Update to PROCESSING
        recharge.setStatus(RechargeStatus.PROCESSING);
        recharge = rechargeRepository.save(recharge);

        // Log business operation: RECHARGE_PROCESSING
        Map<String, Object> processingContext = new HashMap<>();
        processingContext.put("rechargeId", recharge.getRechargeId());
        processingContext.put("userId", recharge.getUserId().toString());
        processingContext.put("previousStatus", "INITIATED");
        processingContext.put("currentStatus", "PROCESSING");
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("INFO")
                .message("Recharge status updated to PROCESSING")
                .eventType("RECHARGE_PROCESSING")
                .context(processingContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Fetch user details to pass along for notifications
        String userEmail = "";
        String userMobile = "";
        try {
            //via Feign to get email/mobile for notifications
            ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(userId);
            if (userApiResponse != null && userApiResponse.isSuccess() && userApiResponse.getData() != null) {
                if (Boolean.TRUE.equals(userApiResponse.getData().getIsEmailVerified())) {
                    userEmail = userApiResponse.getData().getEmail();
                }
                userMobile = userApiResponse.getData().getMobileNumber();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile for userId: {}. Payment notification may lack email/mobile details.", userId);
        }

        // Publish event asynchronously for saga orchestration
        com.omnicharge.recharge.common.event.saga.RechargeInitiatedEvent sagaEvent = com.omnicharge.recharge.common.event.saga.RechargeInitiatedEvent.builder()
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .amount(recharge.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .mobileNumber(recharge.getMobileNumber())
                .operatorName(recharge.getOperatorName())
                .planName(recharge.getPlanName())
                .userEmail(userEmail)
                .userMobile(userMobile)
                .timestamp(LocalDateTime.now())
                .build();
        
        log.info("Publishing RechargeInitiatedEvent: email={}, mobile={}, op={}, plan={}, target={}", 
                sagaEvent.getUserEmail(), sagaEvent.getUserMobile(), 
                sagaEvent.getOperatorName(), sagaEvent.getPlanName(), sagaEvent.getMobileNumber());
        
        rechargeEventProducer.publishRechargeInitiated(sagaEvent);

        return mapToResponse(recharge);
    }

    @Override
    public RechargeResponse getRechargeById(String rechargeId, Long userId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));

        if (!recharge.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to recharge");
        }

        return mapToResponse(recharge);
    }

    @Override
    public Page<RechargeResponse> getRechargeHistory(Long userId, Pageable pageable) {
        Page<Recharge> recharges = rechargeRepository.findByUserId(userId, pageable);
        return recharges.map(this::mapToResponse);
    }

    @Override
    public Page<RechargeResponse> getRechargeHistory(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        LocalDateTime safeStart = (startDate != null) ? startDate : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime safeEnd = (endDate != null) ? endDate : LocalDateTime.of(9999, 12, 31, 23, 59);

        Page<Recharge> recharges = rechargeRepository.findByUserIdWithDateFilters(userId, safeStart, safeEnd, pageable);
        return recharges.map(this::mapToResponse);
    }

    @Override
    public String getRechargeStatus(String rechargeId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));
        return recharge.getStatus().name();
    }

    @Override
    public Page<RechargeResponse> getAllRecharges(Pageable pageable) {
        Page<Recharge> recharges = rechargeRepository.findAll(pageable);
        return recharges.map(this::mapToResponse);
    }

    @Override
    public Page<RechargeResponse> getAllRecharges(String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        LocalDateTime safeStart = (startDate != null) ? startDate : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime safeEnd = (endDate != null) ? endDate : LocalDateTime.of(9999, 12, 31, 23, 59);

        Page<Recharge> recharges;
        if (status == null || status.isBlank()) {
            // ALL — no status filter
            recharges = rechargeRepository.findAllWithDateFilters(safeStart, safeEnd, pageable);
        } else if ("PROCESSING".equalsIgnoreCase(status) || "INITIATED".equalsIgnoreCase(status)) {
            // "PROCESSING" tab covers both INITIATED and PROCESSING DB statuses
            recharges = rechargeRepository.findAllWithStatusesAndDateFilters(
                    java.util.List.of(RechargeStatus.INITIATED, RechargeStatus.PROCESSING),
                    safeStart, safeEnd, pageable);
        } else {
            // Direct enum match: SUCCESS, FAILED, EXPIRED
            RechargeStatus enumStatus = RechargeStatus.valueOf(status.toUpperCase());
            recharges = rechargeRepository.findAllWithStatusAndDateFilters(enumStatus, safeStart, safeEnd, pageable);
        }
        return recharges.map(this::mapToResponse);
    }

    @Override
    public RechargeStatsResponse getRechargeStats() {
        long totalRecharges = rechargeRepository.count();
        long successCount = rechargeRepository.countByStatus(RechargeStatus.SUCCESS);
        long failedCount = rechargeRepository.countByStatus(RechargeStatus.FAILED);

        // Calculate total amount from successful recharges
        List<Recharge> successfulRecharges = rechargeRepository.findByCreatedDateBetween(
                LocalDateTime.now().minusYears(10),
                LocalDateTime.now()
        );

        BigDecimal totalAmount = successfulRecharges.stream()
                .filter(r -> r.getStatus() == RechargeStatus.SUCCESS)
                .map(Recharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RechargeStatsResponse.builder()
                .totalRecharges(totalRecharges)
                .successCount(successCount)
                .failedCount(failedCount)
                .totalAmount(totalAmount)
                .build();
    }

    @Override
    public List<ExpiringRechargeResponse> getExpiringRecharges(int daysLeft) {
        LocalDate expiryDate = LocalDate.now().plusDays(daysLeft);
        List<Recharge> recharges = rechargeRepository.findByStatusAndPlanExpiryDate(RechargeStatus.SUCCESS, expiryDate);

        return recharges.stream()
                .map(this::mapToExpiringResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpiringRechargeResponse> getExpiredToday() {
        LocalDate today = LocalDate.now();
        List<Recharge> recharges = rechargeRepository.findByStatusAndPlanExpiryDate(RechargeStatus.SUCCESS, today);

        return recharges.stream()
                .map(this::mapToExpiringResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsExpired(String rechargeId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));

        recharge.setStatus(RechargeStatus.EXPIRED);
        rechargeRepository.save(recharge);
        log.info("Marked recharge as expired: {}", rechargeId);

        // Log business operation: RECHARGE_EXPIRED
        Map<String, Object> expiredContext = new HashMap<>();
        expiredContext.put("rechargeId", rechargeId);
        expiredContext.put("userId", recharge.getUserId().toString());
        expiredContext.put("expiryDate", recharge.getPlanExpiryDate().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("WARN")
                .message("Recharge marked as expired")
                .eventType("RECHARGE_EXPIRED")
                .context(expiredContext)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private RechargeResponse mapToResponse(Recharge recharge) {
        return RechargeResponse.builder()
                .id(recharge.getId())
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .mobileNumber(recharge.getMobileNumber())
                .operatorId(recharge.getOperatorId())
                .operatorName(recharge.getOperatorName())
                .planId(recharge.getPlanId())
                .planName(recharge.getPlanName())
                .amount(recharge.getAmount())
                .planValidityDays(recharge.getPlanValidityDays())
                .planExpiryDate(recharge.getPlanExpiryDate())
                .status(recharge.getStatus())
                .failureReason(recharge.getFailureReason())
                .transactionId(recharge.getTransactionId())
                .createdDate(recharge.getCreatedDate())
                .build();
    }

    private ExpiringRechargeResponse mapToExpiringResponse(Recharge recharge) {
        // Fetch user details
        UserProfileResponse user = null;
        try {
            ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(recharge.getUserId());
            user = userApiResponse.getData();
        } catch (Exception e) {
            log.error("Failed to fetch user details for userId: {}", recharge.getUserId(), e);
        }

        return ExpiringRechargeResponse.builder()
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userMobile(user != null ? user.getMobileNumber() : null)
                .mobileNumber(recharge.getMobileNumber())
                .operatorName(recharge.getOperatorName())
                .planName(recharge.getPlanName())
                .amount(recharge.getAmount())
                .expiryDate(recharge.getPlanExpiryDate())
                .build();
    }

    private void publishRechargeCompletedEvent(Recharge recharge, String userEmail, String userMobile) {
        try {
            RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                    .rechargeId(recharge.getRechargeId())
                    .userId(recharge.getUserId())
                    .userEmail(userEmail)
                    .userMobile(userMobile)
                    .mobileNumber(recharge.getMobileNumber())
                    .operatorName(recharge.getOperatorName())
                    .planName(recharge.getPlanName())
                    .amount(recharge.getAmount())
                    .status(recharge.getStatus().name())
                    .transactionId(recharge.getTransactionId())
                    .timestamp(LocalDateTime.now())
                    .build();

            rechargeEventProducer.publishRechargeCompleted(event);
            log.info("Published recharge completed event: {}", recharge.getRechargeId());
        } catch (Exception e) {
            log.error("Failed to publish recharge event: {}", recharge.getRechargeId(), e);
        }
    }
}
