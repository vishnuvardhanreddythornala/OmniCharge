package com.omnicharge.payment.service;

import com.omnicharge.payment.common.logging.LogEvent;
import com.omnicharge.payment.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RazorpayPaymentService razorpayPaymentService;

    @BeforeEach
    void setUp()  {
        ReflectionTestUtils.setField(razorpayPaymentService, "keyId", "rzp_test_123");
        ReflectionTestUtils.setField(razorpayPaymentService, "keySecret", "rzp_test_secret_123");
    }

    @Test
    void processRazorpayPayment_Success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("REC123");
        request.setAmount(new BigDecimal("100.00"));
        request.setUserId(1L);

        try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.OrderClient orderClientMock = mock(com.razorpay.OrderClient.class);
                    ReflectionTestUtils.setField(mock, "orders", orderClientMock);

                    Order mockOrder = new Order(new JSONObject("{\"id\":\"order_123\"}"));
                    when(orderClientMock.create(any(JSONObject.class))).thenReturn(mockOrder);
                })) {

            PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

            assertNotNull(response);
            assertEquals("PENDING", response.getStatus());
            assertEquals("order_123", response.getRazorpayOrderId());
            assertNotNull(response.getTransactionId());
            assertEquals(new BigDecimal("100.00"), response.getAmount());

            verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
        }
    }

    @Test
    void processRazorpayPayment_Failure() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("REC123");
        request.setAmount(new BigDecimal("100.00"));

        try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.OrderClient orderClientMock = mock(com.razorpay.OrderClient.class);
                    ReflectionTestUtils.setField(mock, "orders", orderClientMock);

                    when(orderClientMock.create(any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));
                })) {

            PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

            assertNotNull(response);
            assertEquals("FAILED", response.getStatus());
            assertNull(response.getRazorpayOrderId());

            verify(logEventPublisher, times(1)).publish(argThat(event -> "ERROR".equals(event.getLevel())));
        }
    }
    
    @Test
    void processPaymentFallback_Success()  {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("REC123");
        request.setAmount(new BigDecimal("50.00"));

        PaymentResponse response = razorpayPaymentService.processPaymentFallback(request, new RuntimeException("Fallback Error"));

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNull(response.getRazorpayOrderId());
        assertEquals(new BigDecimal("50.00"), response.getAmount());
    }

    @Test
    void processRefund_Success() throws Exception {
        try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.PaymentClient paymentClientMock = mock(com.razorpay.PaymentClient.class);
                    ReflectionTestUtils.setField(mock, "payments", paymentClientMock);

                    // refund() returns a Payment object, not void
                    doReturn(null).when(paymentClientMock).refund(eq("pay_123"), any(JSONObject.class));
                })) {

            assertDoesNotThrow(() -> razorpayPaymentService.processRefund("pay_123", new BigDecimal("100.00")));

            verify(logEventPublisher, times(1)).publish(argThat(event -> "INFO".equals(event.getLevel())));
        }
    }

    @Test
    void processRefund_Failure() throws Exception {
        try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.PaymentClient paymentClientMock = mock(com.razorpay.PaymentClient.class);
                    ReflectionTestUtils.setField(mock, "payments", paymentClientMock);

                    doThrow(new RazorpayException("Refund Failed")).when(paymentClientMock).refund(eq("pay_fail"), any(JSONObject.class));
                })) {

            assertDoesNotThrow(() -> razorpayPaymentService.processRefund("pay_fail", new BigDecimal("100.00")));

            verify(logEventPublisher, times(1)).publish(argThat(event -> "ERROR".equals(event.getLevel())));
        }
    }
}
