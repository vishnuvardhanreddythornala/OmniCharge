package com.omnicharge.notification.service;

public interface ISmsService {

    void sendSms(String mobileNumber, String message);
}
