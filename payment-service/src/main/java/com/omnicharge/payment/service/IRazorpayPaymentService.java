package com.omnicharge.payment.service;

import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;

public interface IRazorpayPaymentService {
    PaymentResponse processRazorpayPayment(PaymentRequest request);
    void processRefund(String paymentId, java.math.BigDecimal amount);
}
