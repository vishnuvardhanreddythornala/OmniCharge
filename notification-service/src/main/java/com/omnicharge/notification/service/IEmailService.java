package com.omnicharge.notification.service;

import com.omnicharge.notification.common.event.PaymentCompletedEvent;
import com.omnicharge.notification.common.event.RechargeCompletedEvent;

public interface IEmailService {

    void sendPaymentConfirmation(String toEmail, PaymentCompletedEvent event);

    void sendRechargeConfirmation(String toEmail, RechargeCompletedEvent event);

    void sendPlanExpiryReminder(String toEmail, String userName, String operatorName, 
                                String planName, String mobileNumber, int daysLeft);

    void sendPlanExpiredNotification(String toEmail, String userName, String operatorName, 
                                     String planName, String mobileNumber);
}
