package com.omnicharge.payment.entity;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    CARD,          // Razorpay generic card type
    UPI,
    NET_BANKING,
    WALLET,
    RAZORPAY,
    UNKNOWN        // Fallback for any unrecognized method
}
