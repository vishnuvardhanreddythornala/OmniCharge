package com.omnicharge.payment.service;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RazorpayPaymentService razorpayPaymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(razorpayPaymentService, "keyId", "rzp_test_mock");
        ReflectionTestUtils.setField(razorpayPaymentService, "keySecret", "mock_secret");
    }

    @Test
    void processRazorpayPayment_SimulatedSuccessDevMode() {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-A1");
        request.setAmount(new BigDecimal("499.00"));

        PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus()); // Testing dev mode mapping
        assertTrue(response.getTransactionId().startsWith("TXN-"));
        assertTrue(response.getRazorpayOrderId().startsWith("order_"));
        assertEquals(new BigDecimal("499.00"), response.getAmount());
    }

    @Test
    void processPaymentFallback_TriggeredOnCircuitBreak() {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-A2");
        request.setAmount(new BigDecimal("199.00"));

        RuntimeException mockException = new RuntimeException("API Timeout");
        PaymentResponse response = razorpayPaymentService.processPaymentFallback(request, mockException);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNull(response.getRazorpayOrderId());
        assertEquals(new BigDecimal("199.00"), response.getAmount());
    }

    @Test
    void processRefund_CapturesExceptionGracefully() {
        // Due to dev bypass logic missing actual SDK checkout mock, processRefund throws RazorpayException natively wrapped
        assertDoesNotThrow(() -> {
            razorpayPaymentService.processRefund("pay_123", new BigDecimal("100.00"));
        });
    }
}
